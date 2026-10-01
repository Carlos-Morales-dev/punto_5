
## Punto 5 — Persistencia Deslocalizada en la Nube (Firebase Firestore + Room)

### 1. Cómo funciona la sincronización

Para este punto implementamos una estrategia donde la app primero muestra los datos que ya tiene guardados localmente, y mientras tanto, por detrás, intenta traer información nueva desde Firebase. La idea es que el usuario nunca vea una pantalla en blanco esperando internet.

- **La app carga instantáneo:** cuando abres la app, no espera a que responda Firebase para mostrar algo. Lee directamente lo que ya está guardado en Room (nuestra base de datos local) a través de un `StateFlow`, así que las tareas aparecen de inmediato.
- **La sincronización pasa en segundo plano:** apenas se crea el `TareaViewModel` (en su bloque `init`), se lanza automáticamente una corrutina que intenta descargar el catálogo desde Firestore. También dejamos un botón "Sync" por si el usuario quiere forzar la sincronización manualmente. Todo esto corre en `Dispatchers.IO` dentro de `viewModelScope`, para no trabarse con el hilo principal.
- **La interfaz no se congela:** mientras está sincronizando, se muestra un indicador de carga pequeño en la barra superior, pero el usuario puede seguir usando la app con normalidad (ver tareas, filtrar, marcar como completadas) sin que nada se bloquee.

---

### 2. De dónde vienen los datos

Los datos remotos vienen de una colección en Firestore llamada `catalogo_tareas`.

Decidimos que fuera un catálogo genérico de tareas comunes (cosas de estudio, trabajo, trámites, actividades personales), porque nuestra app no está enfocada a un público específico, sino que es un gestor de tareas de uso general. La idea es que estas tareas sirvan como sugerencias o plantillas que el usuario puede usar como punto de partida.

Cuando la app descarga estos documentos, el `RemoteDataSource` los convierte en objetos `Tarea` (el mismo modelo que ya usábamos desde el Punto 2), guardando el `id` del documento de Firestore en un campo nuevo llamado `remoteId`, además del título, descripción, categoría y prioridad de cada una.

---

### 3. Cómo evitamos que se dupliquen las tareas

Un problema que nos encontramos al principio era que, si el usuario sincronizaba varias veces seguidas, corríamos el riesgo de insertar las mismas tareas una y otra vez. Para evitarlo, hicimos lo siguiente:

**Agregamos un índice único en Room**, sobre el campo `remoteId`, en la entidad `Tarea.kt`:

```kotlin
@Entity(
    tableName = "tareas",
    indices = [Index(value = ["remote_id"], unique = true)]
)
data class Tarea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "remote_id") val remoteId: String? = null,
    ...
)
```
