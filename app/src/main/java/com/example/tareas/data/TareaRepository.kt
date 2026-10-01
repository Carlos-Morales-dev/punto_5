package com.example.tareas.data

import android.util.Log
import kotlinx.coroutines.flow.Flow

// Patron de diseno repositorio para separar la fuente de datos de la interfaz
class TareaRepository(
    private val tareaDao: TareaDao,
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) {

    companion object {
        private const val TAG = "TareaRepository"
    }

    val todasLasTareas: Flow<List<Tarea>> = tareaDao.getAll()

    suspend fun insert(tarea: Tarea): Long {
        return tareaDao.insert(tarea)
    }

    suspend fun insertAll(tareas: List<Tarea>) {
        tareaDao.insertAll(tareas)
    }

    suspend fun update(tarea: Tarea) {
        tareaDao.update(tarea)
    }

    suspend fun delete(tarea: Tarea) {
        tareaDao.delete(tarea)
    }

    suspend fun deleteById(id: Int) {
        tareaDao.deleteById(id)
    }

    suspend fun getById(id: Int): Tarea? {
        return tareaDao.getById(id)
    }

    suspend fun getNoSincronizadas(): List<Tarea> {
        return tareaDao.getNoSincronizadas()
    }

    suspend fun marcarSincronizada(id: Int) {
        tareaDao.marcarSincronizada(id)
    }

    suspend fun eliminarCompletadas() {
        tareaDao.eliminarCompletadas()
    }

    suspend fun deleteByCategoria(categoria: String) {
        tareaDao.deleteByCategoria(categoria)
    }

    /**
     * Sincroniza el catálogo remoto de Firestore con la base de datos local Room.
     * Si la descarga tiene éxito, inserta los elementos en Room usando [insertAll].
     * Si ocurre un error (modo offline, timeout, error de Firestore), se captura de forma
     * segura mediante [runCatching] sin crashear la aplicación.
     */
    suspend fun syncWithRemote(): Result<Unit> {
        return runCatching {
            val catalogoRemoto = remoteDataSource.obtenerCatalogoTareas()
            if (catalogoRemoto.isNotEmpty()) {
                tareaDao.insertAll(catalogoRemoto)
            }
            Log.d(TAG, "Sincronización remota exitosa: ${catalogoRemoto.size} tareas sincronizadas con Room.")
            Unit
        }.onFailure { error ->
            Log.w(TAG, "Fallo al sincronizar con Firestore (trabajando en modo offline): ${error.message}")
        }
    }
}
