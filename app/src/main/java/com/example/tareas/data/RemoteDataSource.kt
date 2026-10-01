package com.example.tareas.data

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

class RemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    companion object {
        private const val TAG = "RemoteDataSource"
        private const val COLECCION_CATALOGO = "catalogo_tareas"
    }

    /**
     * Descarga todos los documentos de la colección "catalogo_tareas" de Firestore desde el servidor
     * y los mapea a objetos Tarea con remoteId asignado, estadoCompletado = false y sincronizado = true.
     */
    suspend fun obtenerCatalogoTareas(): List<Tarea> {
        return try {
            // Forzamos Source.SERVER para garantizar sincronización real con la nube;
            // en modo avión / sin internet fallará de inmediato en lugar de enmascarar el error con la caché interna de Firestore.
            val snapshot = firestore.collection(COLECCION_CATALOGO).get(Source.SERVER).await()
            snapshot.documents.mapNotNull { doc ->
                try {
                    val titulo = doc.getString("titulo")
                        ?: doc.getString("title")
                        ?: "Tarea sin título"

                    val descripcion = doc.getString("descripcion")
                        ?: doc.getString("description")
                        ?: ""

                    val categoria = doc.getString("categoria")
                        ?: doc.getString("category")
                        ?: "Universidad"

                    val prioridad = (doc.getString("prioridad") ?: doc.getString("priority") ?: "MEDIA").uppercase()

                    // fechaLimite puede venir como Number (Long/Int) o como Timestamp de Firestore
                    val fechaLimiteLong: Long? = when (val rawFecha = doc.get("fechaLimite") ?: doc.get("fecha_limite")) {
                        is Number -> rawFecha.toLong()
                        is Timestamp -> rawFecha.toDate().time
                        else -> null
                    }

                    Tarea(
                        id = 0,
                        titulo = titulo,
                        descripcion = descripcion,
                        estadoCompletado = false,
                        fechaCreacion = System.currentTimeMillis(),
                        sincronizado = true,
                        prioridad = prioridad,
                        categoria = categoria,
                        fechaLimite = fechaLimiteLong,
                        remoteId = doc.id,
                        fechaActualizacion = System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Aviso al procesar documento con id=${doc.id}: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Conexión remota no disponible (modo offline o sin internet): ${e.message}")
            throw e
        }
    }
}
