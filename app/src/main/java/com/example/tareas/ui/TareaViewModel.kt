package com.example.tareas.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.tareas.data.Tarea
import com.example.tareas.data.TareaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel que gestiona el estado de las tareas y la sincronización con Firestore.
 * Sobrevive a cambios de configuración (rotación de pantalla) y mantiene la lógica de corrutinas
 * desacoplada de la UI de Compose.
 */
class TareaViewModel(
    private val repository: TareaRepository
) : ViewModel() {

    // Flujo de todas las tareas obtenido desde Room y convertido en StateFlow seguro para Compose
    val todasLasTareas: StateFlow<List<Tarea>> = repository.todasLasTareas
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Indicador de sincronización en segundo plano (para carga sutil en UI)
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Mensaje de error discreto para notificar incidencias (ej. sin conexión) sin bloquear la UI
    private val _errorMensaje = MutableStateFlow<String?>(null)
    val errorMensaje: StateFlow<String?> = _errorMensaje.asStateFlow()

    init {
        // Disparo automático de sincronización en segundo plano al inicializar el ViewModel
        syncWithRemote()
    }

    /**
     * Ejecuta la sincronización en segundo plano con Firestore sin bloquear el hilo principal.
     */
    fun syncWithRemote(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            _errorMensaje.value = null

            val resultado = repository.syncWithRemote()
            val exito = resultado.isSuccess

            resultado.onFailure { exception ->
                _errorMensaje.value = "Modo sin conexión"
            }

            _isSyncing.value = false
            onComplete?.invoke(exito)
        }
    }

    /**
     * Limpia el mensaje de error una vez visualizado por el usuario.
     */
    fun limpiarError() {
        _errorMensaje.value = null
    }

    // Funciones de conveniencia para manipulación de tareas desde la UI
    fun insertarTarea(tarea: Tarea, onCompletado: ((Long) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insert(tarea)
            onCompletado?.invoke(id)
        }
    }

    fun actualizarTarea(tarea: Tarea) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(tarea)
        }
    }

    fun cambiarEstadoTarea(tarea: Tarea, completado: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(tarea.copy(estadoCompletado = completado))
        }
    }

    fun eliminarTarea(tarea: Tarea) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(tarea)
        }
    }

    fun eliminarTareaPorId(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteById(id)
        }
    }

    fun eliminarCompletadas() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.eliminarCompletadas()
        }
    }

    fun eliminarPorCategoria(categoria: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteByCategoria(categoria)
        }
    }

    fun sincronizarTareasLocales(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            syncWithRemote { exito ->
                if (exito) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val noSync = repository.getNoSincronizadas()
                        noSync.forEach { repository.marcarSincronizada(it.id) }
                    }
                }
                onComplete?.invoke(exito)
            }
        }
    }
}

/**
 * Factory para instanciar TareaViewModel con inyección de TareaRepository.
 */
class TareaViewModelFactory(
    private val repository: TareaRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TareaViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TareaViewModel(repository) as T
        }
        throw IllegalArgumentException("Clase ViewModel desconocida: ${modelClass.name}")
    }
}
