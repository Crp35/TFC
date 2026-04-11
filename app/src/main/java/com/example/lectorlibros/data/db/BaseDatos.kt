package com.example.lectorlibros.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lectorlibros.data.converter.Conversor
import com.example.lectorlibros.entities.LecturaLibro
import com.example.lectorlibros.entities.LibroEntity

@Database(entities = [LibroEntity::class, LecturaLibro::class],
    version = 4,
    exportSchema = false
)
/**
 * Con esto Room podrá convertir automáticamente el enum TipoDeLibro a String
 * en la base de datos y viceversa.
 * */
@TypeConverters(Conversor::class)
abstract class BaseDatos : RoomDatabase() {
    abstract fun libroDao(): LibroDao
    abstract fun lecturaLibroDao(): LecturaLibroDao

    companion object {
        @Volatile
        private var INSTANCE: BaseDatos? = null

        fun getDatabase(context: Context): BaseDatos {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BaseDatos::class.java,
                    "leo_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}