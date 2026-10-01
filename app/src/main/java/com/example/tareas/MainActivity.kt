package com.example.tareas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.example.tareas.data.AppDatabase
import com.example.tareas.data.TareaRepository
import com.example.tareas.ui.ListaTareasScreen
import com.example.tareas.ui.TareaViewModel
import com.example.tareas.ui.TareaViewModelFactory
import com.example.tareas.ui.theme.TareasTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TareaRepository(database.tareaDao())
        val factory = TareaViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[TareaViewModel::class.java]

        setContent {
            TareasTheme {
                ListaTareasScreen(viewModel = viewModel)
            }
        }
    }
}
