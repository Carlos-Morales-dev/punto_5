package com.example.tareas.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tareas.data.Tarea

enum class FiltroEstado {
    TODAS,
    PENDIENTES,
    COMPLETADAS,
    OFFLINE_PENDIENTE_SYNC
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListaTareasScreen(
    viewModel: TareaViewModel,
    modifier: Modifier = Modifier
) {
    // Manejo de estado reactivo que observa el StateFlow desde el ViewModel, seguro ante rotaciones de pantalla
    val todasLasTareas by viewModel.todasLasTareas.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val errorMensaje by viewModel.errorMensaje.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var mensajeFeedbackUsuario by remember { mutableStateOf<String?>(null) }

    // Notificaciones de usuario y de errores de sincronización sin corrutinas acopladas a la pantalla
    LaunchedEffect(mensajeFeedbackUsuario) {
        mensajeFeedbackUsuario?.let { mensaje ->
            snackbarHostState.showSnackbar(mensaje)
            mensajeFeedbackUsuario = null
        }
    }

    LaunchedEffect(errorMensaje) {
        errorMensaje?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.limpiarError()
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    var filtroEstado by remember { mutableStateOf(FiltroEstado.TODAS) }
    var prioridadSeleccionada by remember { mutableStateOf("Todas") }
    var categoriaSeleccionada by remember { mutableStateOf("Todas") }
    var busquedaQuery by remember { mutableStateOf("") }
    var busquedaVisible by remember { mutableStateOf(false) }

    var tareaParaEditar by remember { mutableStateOf<Tarea?>(null) }
    var tareaParaEliminar by remember { mutableStateOf<Tarea?>(null) }
    var categoriaParaEliminar by remember { mutableStateOf<String?>(null) }
    var mostrarDialogoAgregarEditar by remember { mutableStateOf(false) }
    var mostrarDialogoConfirmarEliminar by remember { mutableStateOf(false) }

    val categoriasDisponibles = remember(todasLasTareas) {
        val base = listOf("Todas", "Universidad", "Trabajo", "Proyectos", "Personal", "Urgente")
        (base + todasLasTareas.map { it.categoria }).distinct()
    }

    val tareasFiltradas = remember(todasLasTareas, filtroEstado, prioridadSeleccionada, categoriaSeleccionada, busquedaQuery) {
        todasLasTareas.filter { tarea ->
            val coincideEstado = when (filtroEstado) {
                FiltroEstado.TODAS -> true
                FiltroEstado.PENDIENTES -> !tarea.estadoCompletado
                FiltroEstado.COMPLETADAS -> tarea.estadoCompletado
                FiltroEstado.OFFLINE_PENDIENTE_SYNC -> !tarea.sincronizado
            }

            val coincidePrioridad = if (prioridadSeleccionada == "Todas") {
                true
            } else {
                tarea.prioridad.equals(prioridadSeleccionada, ignoreCase = true)
            }

            val coincideCategoria = if (categoriaSeleccionada == "Todas") {
                true
            } else {
                tarea.categoria.equals(categoriaSeleccionada, ignoreCase = true)
            }

            val coincideBusqueda = if (busquedaQuery.isBlank()) {
                true
            } else {
                tarea.titulo.contains(busquedaQuery, ignoreCase = true) ||
                        tarea.descripcion.contains(busquedaQuery, ignoreCase = true)
            }

            coincideEstado && coincidePrioridad && coincideCategoria && coincideBusqueda
        }
    }

    val totalTareas = todasLasTareas.size
    val tareasCompletadas = todasLasTareas.count { it.estadoCompletado }
    val tareasSinSincronizar = todasLasTareas.count { !it.sincronizado }
    val ratioProgreso = if (totalTareas == 0) 0f else tareasCompletadas.toFloat() / totalTareas

    val ejecutarSincronizacion: () -> Unit = {
        viewModel.sincronizarTareasLocales { exito ->
            if (exito) {
                mensajeFeedbackUsuario = "Tareas sincronizadas con la nube"
            }
        }
    }

    val solicitarEliminarCompletadas: () -> Unit = {
        if (tareasCompletadas > 0) {
            mostrarDialogoConfirmarEliminar = true
        } else {
            mensajeFeedbackUsuario = "No hay tareas completadas para eliminar"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TaskAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Lista de Tareas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .testTag("indicador_sync_actions"),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(
                        onClick = {
                            busquedaVisible = !busquedaVisible
                            if (!busquedaVisible) busquedaQuery = ""
                        },
                        modifier = Modifier.testTag("btn_toggle_busqueda")
                    ) {
                        Icon(
                            imageVector = if (busquedaVisible) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = "Buscar tareas"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (!isLandscape) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.testTag("contenedor_acciones_inferior_izq")
                        ) {
                            BotonSincronizar(
                                tareasSinSincronizar = tareasSinSincronizar,
                                onClick = ejecutarSincronizacion
                            )

                            BotonEliminarHechas(
                                tareasCompletadas = tareasCompletadas,
                                onClick = solicitarEliminarCompletadas
                            )
                        }

                        FloatingActionButton(
                            onClick = {
                                tareaParaEditar = null
                                mostrarDialogoAgregarEditar = true
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(52.dp)
                                .testTag("fab_agregar_tarea")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nueva tarea",
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Column(
                    modifier = Modifier
                        .width(340.dp)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(
                        visible = busquedaVisible,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = busquedaQuery,
                            onValueChange = { busquedaQuery = it },
                            placeholder = { Text("Buscar tareas...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_busqueda_tiempo_real"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (busquedaQuery.isNotBlank()) {
                                    IconButton(onClick = { busquedaQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            }
                        )
                    }

                    CardMetricas(
                        totalTareas = totalTareas,
                        tareasCompletadas = tareasCompletadas,
                        tareasSinSincronizar = tareasSinSincronizar,
                        ratioProgreso = ratioProgreso,
                        compacto = true
                    )

                    FilaFiltrosDesplegables(
                        filtroEstado = filtroEstado,
                        onFiltroEstadoChange = { filtroEstado = it },
                        totalTareas = totalTareas,
                        tareasCompletadas = tareasCompletadas,
                        tareasSinSincronizar = tareasSinSincronizar,
                        prioridadSeleccionada = prioridadSeleccionada,
                        onPrioridadChange = { prioridadSeleccionada = it },
                        categoriaSeleccionada = categoriaSeleccionada,
                        onCategoriaChange = { categoriaSeleccionada = it },
                        categoriasDisponibles = categoriasDisponibles,
                        onEliminarCategoria = { cat ->
                            categoriaParaEliminar = cat
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f, fill = false))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("contenedor_acciones_landscape_izq"),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Acciones Rápidas",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            BotonSincronizar(
                                tareasSinSincronizar = tareasSinSincronizar,
                                onClick = ejecutarSincronizacion,
                                modifier = Modifier.weight(1f)
                            )
                            BotonEliminarHechas(
                                tareasCompletadas = tareasCompletadas,
                                onClick = solicitarEliminarCompletadas,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    ListaTareasContent(
                        tareasFiltradas = tareasFiltradas,
                        busquedaQuery = busquedaQuery,
                        totalTareas = totalTareas,
                        // Callback para alternar el estado completado y actualizar la tarea en el almacenamiento local
                        onToggleCompletado = { tarea ->
                            viewModel.cambiarEstadoTarea(tarea, !tarea.estadoCompletado)
                        },
                        // Callback para asignar la tarea seleccionada y abrir el dialogo de edicion
                        onEditar = { tarea ->
                            tareaParaEditar = tarea
                            mostrarDialogoAgregarEditar = true
                        },
                        // Callback para seleccionar la tarea que se desea eliminar tras confirmacion
                        onEliminar = { tarea ->
                            tareaParaEliminar = tarea
                        },
                        contentPadding = PaddingValues(bottom = 76.dp)
                    )

                    FloatingActionButton(
                        onClick = {
                            tareaParaEditar = null
                            mostrarDialogoAgregarEditar = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = 8.dp)
                            .size(52.dp)
                            .testTag("fab_agregar_tarea")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Nueva tarea",
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AnimatedVisibility(
                    visible = busquedaVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    OutlinedTextField(
                        value = busquedaQuery,
                        onValueChange = { busquedaQuery = it },
                        placeholder = { Text("Buscar en título o descripción...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("input_busqueda_tiempo_real"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (busquedaQuery.isNotBlank()) {
                                IconButton(onClick = { busquedaQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                                }
                            }
                        }
                    )
                }

                if (totalTareas > 0) {
                    CardMetricas(
                        totalTareas = totalTareas,
                        tareasCompletadas = tareasCompletadas,
                        tareasSinSincronizar = tareasSinSincronizar,
                        ratioProgreso = ratioProgreso,
                        compacto = false
                    )
                }

                FilaFiltrosDesplegables(
                    filtroEstado = filtroEstado,
                    onFiltroEstadoChange = { filtroEstado = it },
                    totalTareas = totalTareas,
                    tareasCompletadas = tareasCompletadas,
                    tareasSinSincronizar = tareasSinSincronizar,
                    prioridadSeleccionada = prioridadSeleccionada,
                    onPrioridadChange = { prioridadSeleccionada = it },
                    categoriaSeleccionada = categoriaSeleccionada,
                    onCategoriaChange = { categoriaSeleccionada = it },
                    categoriasDisponibles = categoriasDisponibles,
                    onEliminarCategoria = { cat ->
                        categoriaParaEliminar = cat
                    }
                )

                Spacer(modifier = Modifier.height(6.dp))

                ListaTareasContent(
                    tareasFiltradas = tareasFiltradas,
                    busquedaQuery = busquedaQuery,
                    totalTareas = totalTareas,
                    // Callback para alternar el estado completado y actualizar la tarea en el almacenamiento local
                    onToggleCompletado = { tarea ->
                        viewModel.cambiarEstadoTarea(tarea, !tarea.estadoCompletado)
                    },
                    // Callback para asignar la tarea seleccionada y abrir el dialogo de edicion
                    onEditar = { tarea ->
                        tareaParaEditar = tarea
                        mostrarDialogoAgregarEditar = true
                    },
                    // Callback para seleccionar la tarea que se desea eliminar tras confirmacion
                    onEliminar = { tarea ->
                        tareaParaEliminar = tarea
                    },
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 16.dp
                    )
                )
            }
        }
    }

    // Diálogo de confirmación para eliminar tareas completadas en lote
    if (mostrarDialogoConfirmarEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoConfirmarEliminar = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar tareas hechas?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Se eliminarán permanentemente las $tareasCompletadas tareas finalizadas. ¿Deseas continuar?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarCompletadas()
                        mensajeFeedbackUsuario = "Tareas completadas eliminadas de Room"
                        mostrarDialogoConfirmarEliminar = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirmar_eliminar_hechas")
                ) {
                    Text("Eliminar todas")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { mostrarDialogoConfirmarEliminar = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_cancelar_eliminar_hechas")
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("dialogo_confirmar_eliminar")
        )
    }

    // Diálogo de confirmación para eliminar tarea individual
    if (tareaParaEliminar != null) {
        val tareaActual = tareaParaEliminar!!
        AlertDialog(
            onDismissRequest = { tareaParaEliminar = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar esta tarea?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "¿Estás seguro de que deseas eliminar la tarea \"${tareaActual.titulo}\"? Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    // Callback para eliminar definitivamente la tarea del repositorio local
                    onClick = {
                        viewModel.eliminarTarea(tareaActual)
                        mensajeFeedbackUsuario = "Tarea eliminada correctamente"
                        tareaParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirmar_eliminar_individual")
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { tareaParaEliminar = null },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_cancelar_eliminar_individual")
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("dialogo_confirmar_eliminar_individual")
        )
    }

    // Diálogo de confirmación para eliminar categoría y sus tareas
    if (categoriaParaEliminar != null) {
        val catAEliminar = categoriaParaEliminar!!
        val tareasEnCategoria = todasLasTareas.count { it.categoria.equals(catAEliminar, ignoreCase = true) }
        AlertDialog(
            onDismissRequest = { categoriaParaEliminar = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "¿Eliminar categoría '$catAEliminar'?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Se eliminará la categoría y las $tareasEnCategoria tareas asociadas a ella. Esta acción no se puede deshacer.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eliminarPorCategoria(catAEliminar)
                        if (categoriaSeleccionada.equals(catAEliminar, ignoreCase = true)) {
                            categoriaSeleccionada = "Todas"
                        }
                        mensajeFeedbackUsuario = "Categoría '$catAEliminar' y sus tareas eliminadas"
                        categoriaParaEliminar = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_confirmar_eliminar_categoria")
                ) {
                    Text("Eliminar todo")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { categoriaParaEliminar = null },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("btn_cancelar_eliminar_categoria")
                ) {
                    Text("Cancelar")
                }
            },
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.testTag("dialogo_confirmar_eliminar_categoria")
        )
    }

    // Diálogo modal para agregar o modificar tarea
    if (mostrarDialogoAgregarEditar) {
        val tareaAEditarLocal = tareaParaEditar
        AgregarEditarTareaScreen(
            tareaAEditar = tareaAEditarLocal,
            onGuardar = { titulo, descripcion, categoria, prioridad, fechaLimite ->
                if (tareaAEditarLocal == null) {
                    // Callback para insertar una nueva tarea en la base de datos local
                    val nuevaTarea = Tarea(
                        titulo = titulo,
                        descripcion = descripcion,
                        categoria = categoria,
                        prioridad = prioridad,
                        fechaLimite = fechaLimite,
                        sincronizado = false,
                        fechaCreacion = System.currentTimeMillis()
                    )
                    viewModel.insertarTarea(nuevaTarea)
                    mensajeFeedbackUsuario = "Tarea guardada en Room exitosamente"
                } else {
                    // Callback para actualizar los datos de la tarea existente en la base de datos local
                    val tareaActualizada = tareaAEditarLocal.copy(
                        titulo = titulo,
                        descripcion = descripcion,
                        categoria = categoria,
                        prioridad = prioridad,
                        fechaLimite = fechaLimite,
                        sincronizado = false,
                        fechaActualizacion = System.currentTimeMillis()
                    )
                    viewModel.actualizarTarea(tareaActualizada)
                    mensajeFeedbackUsuario = "Tarea actualizada en Room"
                }
                mostrarDialogoAgregarEditar = false
                tareaParaEditar = null
            },
            onDescartar = {
                mostrarDialogoAgregarEditar = false
                tareaParaEditar = null
            }
        )
    }
}

@Composable
fun FilaFiltrosDesplegables(
    filtroEstado: FiltroEstado,
    onFiltroEstadoChange: (FiltroEstado) -> Unit,
    totalTareas: Int,
    tareasCompletadas: Int,
    tareasSinSincronizar: Int,
    prioridadSeleccionada: String,
    onPrioridadChange: (String) -> Unit,
    categoriaSeleccionada: String,
    onCategoriaChange: (String) -> Unit,
    categoriasDisponibles: List<String>,
    onEliminarCategoria: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val textoEstado = when (filtroEstado) {
            FiltroEstado.TODAS -> "Todas ($totalTareas)"
            FiltroEstado.PENDIENTES -> "Pendientes (${totalTareas - tareasCompletadas})"
            FiltroEstado.COMPLETADAS -> "Hechas ($tareasCompletadas)"
            FiltroEstado.OFFLINE_PENDIENTE_SYNC -> "Sync ($tareasSinSincronizar)"
        }

        MenuDesplegableFiltro(
            label = "Estado",
            valorSeleccionado = textoEstado,
            icono = Icons.Default.FilterList,
            opciones = listOf(
                FiltroEstado.TODAS to "Todas ($totalTareas)",
                FiltroEstado.PENDIENTES to "Pendientes (${totalTareas - tareasCompletadas})",
                FiltroEstado.COMPLETADAS to "Hechas ($tareasCompletadas)",
                FiltroEstado.OFFLINE_PENDIENTE_SYNC to "Offline / Sync ($tareasSinSincronizar)"
            ),
            onSeleccionar = onFiltroEstadoChange,
            modifier = Modifier.width(155.dp),
            testTag = "desplegable_filtro_estado"
        )

        MenuDesplegableFiltro(
            label = "Prioridad",
            valorSeleccionado = prioridadSeleccionada,
            icono = Icons.Default.Flag,
            opciones = listOf("Todas", "ALTA", "MEDIA", "BAJA").map { it to it },
            onSeleccionar = onPrioridadChange,
            modifier = Modifier.width(140.dp),
            testTag = "desplegable_filtro_prioridad"
        )

        MenuDesplegableFiltro(
            label = "Categoría",
            valorSeleccionado = categoriaSeleccionada,
            icono = Icons.Default.Category,
            opciones = categoriasDisponibles.map { it to it },
            onSeleccionar = onCategoriaChange,
            onEliminarOpcion = onEliminarCategoria,
            modifier = Modifier.width(160.dp),
            testTag = "desplegable_filtro_categoria"
        )
    }
}

@Composable
fun <T> MenuDesplegableFiltro(
    label: String,
    valorSeleccionado: String,
    icono: ImageVector,
    opciones: List<Pair<T, String>>,
    onSeleccionar: (T) -> Unit,
    onEliminarOpcion: ((T) -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    var expandido by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
                .clickable { expandido = true },
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            border = BorderStroke(
                1.dp,
                if (expandido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = valorSeleccionado,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Icon(
                    imageVector = if (expandido) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Expandir menú",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false },
            modifier = Modifier.widthIn(min = 190.dp)
        ) {
            opciones.forEach { (item, texto) ->
                val esSeleccionado = texto == valorSeleccionado || texto.startsWith(valorSeleccionado)
                val esTodas = texto == "Todas" || texto.startsWith("Todas")
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (esSeleccionado) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                Text(
                                    text = texto,
                                    fontWeight = if (esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                    color = if (esSeleccionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (onEliminarOpcion != null && !esTodas) {
                                IconButton(
                                    onClick = {
                                        expandido = false
                                        onEliminarOpcion(item)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Eliminar categoría",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    },
                    onClick = {
                        onSeleccionar(item)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
fun BotonSincronizar(
    tareasSinSincronizar: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1E5631),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.testTag("btn_sincronizar_bottom_left")
    ) {
        Icon(
            imageVector = Icons.Default.CloudSync,
            contentDescription = "Sincronizar",
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (tareasSinSincronizar > 0) "Sync ($tareasSinSincronizar)" else "Sync",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun BotonEliminarHechas(
    tareasCompletadas: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f),
            contentColor = MaterialTheme.colorScheme.error
        ),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        modifier = modifier.testTag("btn_eliminar_hechas_bottom_left")
    ) {
        Icon(
            imageVector = Icons.Default.DeleteSweep,
            contentDescription = "Eliminar tareas hechas",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "Limpiar ($tareasCompletadas)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun CardMetricas(
    totalTareas: Int,
    tareasCompletadas: Int,
    tareasSinSincronizar: Int,
    ratioProgreso: Float,
    compacto: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = if (compacto) 0.dp else 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progreso de Tareas",
                        style = if (compacto) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "$tareasCompletadas de $totalTareas finalizadas",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                Text(
                    text = "${(ratioProgreso * 100).toInt()}%",
                    style = if (compacto) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { ratioProgreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .testTag("progreso_metricas"),
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.3f),
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SQLite Local (Room)",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.8f)
                )

                if (tareasSinSincronizar > 0) {
                    Text(
                        text = "$tareasSinSincronizar por sincronizar",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.Yellow
                    )
                } else {
                    Text(
                        text = "Todo sincronizado",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ListaTareasContent(
    tareasFiltradas: List<Tarea>,
    busquedaQuery: String,
    totalTareas: Int,
    onToggleCompletado: (Tarea) -> Unit,
    onEditar: (Tarea) -> Unit,
    onEliminar: (Tarea) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val tareasPendientes = remember(tareasFiltradas) { tareasFiltradas.filter { !it.estadoCompletado } }
    val tareasCompletadas = remember(tareasFiltradas) { tareasFiltradas.filter { it.estadoCompletado } }
    var completadasExpandidas by remember { mutableStateOf(false) }

    if (tareasFiltradas.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (busquedaQuery.isNotBlank()) "No hay resultados para la búsqueda"
                    else if (totalTareas == 0) "¡Aún no hay tareas registradas!"
                    else "No hay tareas con estos filtros",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (busquedaQuery.isNotBlank()) "Verifica el término ingresado o limpia la búsqueda"
                    else "Pulsa el botón + para registrar una tarea persistida en Room",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        // Renderiza listas dinamicas de forma eficiente reciclando elementos visibles como un RecyclerView
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = tareasPendientes,
                key = { "pendiente_${it.id}" }
            ) { tarea ->
                TareaItem(
                    tarea = tarea,
                    onToggleCompletado = { onToggleCompletado(tarea) },
                    onEditar = { onEditar(tarea) },
                    onEliminar = { onEliminar(tarea) }
                )
            }

            if (tareasCompletadas.isNotEmpty()) {
                item(key = "seccion_completadas_header") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { completadasExpandidas = !completadasExpandidas }
                            .testTag("card_seccion_completadas"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Tareas Completadas (${tareasCompletadas.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (completadasExpandidas) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = if (completadasExpandidas) "Ocultar completadas" else "Mostrar completadas",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                if (completadasExpandidas) {
                    items(
                        items = tareasCompletadas,
                        key = { "completada_${it.id}" }
                    ) { tarea ->
                        TareaItem(
                            tarea = tarea,
                            onToggleCompletado = { onToggleCompletado(tarea) },
                            onEditar = { onEditar(tarea) },
                            onEliminar = { onEliminar(tarea) }
                        )
                    }
                }
            }
        }
    }
}
