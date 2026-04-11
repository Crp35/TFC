package com.example.lectorlibros.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.lectorlibros.entities.LecturaLibro

@Dao
interface LecturaLibroDao {
    @Query("SELECT * FROM libro WHERE titulo = :titulo")
    suspend fun getProgreso(titulo: String): LecturaLibro? // Buscamos la página actual del libro

    @Insert(onConflict = OnConflictStrategy.REPLACE) // Insertamos o reemplazamos progreso
    suspend fun guardarProgreso(libro: LecturaLibro)

}