package com.example.tareas.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TareaDao {

    @Query("SELECT * FROM tareas ORDER BY estado_completado ASC, fecha_creacion DESC")
    fun getAll(): Flow<List<Tarea>>

    @Query("SELECT * FROM tareas WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Tarea?

    @Query("SELECT * FROM tareas WHERE sincronizado = 0")
    suspend fun getNoSincronizadas(): List<Tarea>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tarea: Tarea): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tareas: List<Tarea>)

    @Update
    suspend fun update(tarea: Tarea)

    @Delete
    suspend fun delete(tarea: Tarea)

    @Query("DELETE FROM tareas WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("UPDATE tareas SET sincronizado = 1 WHERE id = :id")
    suspend fun marcarSincronizada(id: Int)

    @Query("DELETE FROM tareas WHERE estado_completado = 1")
    suspend fun eliminarCompletadas()

    @Query("DELETE FROM tareas WHERE categoria = :categoria")
    suspend fun deleteByCategoria(categoria: String)
}
