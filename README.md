
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
### 4. Qué pasa si no hay conexión a internet

La app está pensada para que funcione bien incluso sin internet, no solo cuando todo está conectado.

- **No se cae la app:** en `TareaRepository.kt`, envolvimos la llamada a Firestore con `runCatching`, así que si falla por cualquier motivo (sin red, timeout, lo que sea), la excepción se captura y no provoca un crash.
- **Los datos siguen disponibles:** si no hay internet, el usuario puede seguir usando la app con total normalidad — ver sus tareas, filtrarlas, marcarlas como completadas, crear nuevas — porque todo eso sigue funcionando contra Room, que no depende de la conexión.
- **Se avisa de forma discreta:** cuando falla la sincronización, el `ViewModel` actualiza un estado de error que dispara un Snackbar con el mensaje "Modo sin conexión". Después de mostrarse, ese estado se limpia solo, para que no se quede repitiendo el mensaje cada vez que la pantalla se recompone.

---

### 5. Un detalle técnico que encontramos probando: Source.DEFAULT vs Source.SERVER

Mientras probábamos el comportamiento sin conexión (activando modo avión en el emulador), nos encontramos con algo que no esperábamos: la app decía que había sincronizado correctamente, aunque estuviéramos en modo avión.

Después de revisarlo, nos dimos cuenta de que **Firestore tiene su propia caché interna**, completamente aparte de la que nosotros manejamos en Room. Por defecto (`Source.DEFAULT`), si el dispositivo ya había descargado datos antes, Firestore simplemente devuelve esos datos desde su caché propia sin intentar conectarse al servidor — entonces nuestra consulta "tenía éxito" aunque no hubiera internet real, lo cual hacía que nuestra lógica de manejo de errores nunca se activara.

Para solucionarlo, forzamos que la consulta use `Source.SERVER` en vez del comportamiento por defecto:

```kotlin
val snapshot = firestore.collection("catalogo_tareas").get(Source.SERVER).await()
```

Con este cambio, la app ahora sí intenta conectarse directamente al servidor cada vez, y si no hay conexión real, la consulta falla de inmediato como se esperaría — permitiendo que nuestro manejo de errores funcione correctamente y el usuario vea el aviso de "Modo sin conexión".

Este ajuste también nos deja algo más ordenado: toda la responsabilidad de guardar datos para uso offline queda centralizada en Room (que es la base de datos que nosotros controlamos), en vez de depender de dos cachés distintas funcionando por separado sin que nos diéramos cuenta.
