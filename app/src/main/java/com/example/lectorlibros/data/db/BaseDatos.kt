package com.example.lectorlibros.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.lectorlibros.data.converter.Conversor

@Database(entities = [LibroEntity::class],
    version = 2,
    exportSchema = false
)
/**
 * Con esto Room podrá convertir automáticamente el enum TipoDeLibro a String
 * en la base de datos y viceversa.
 * */
@TypeConverters(Conversor::class)
abstract class BaseDatos : RoomDatabase() {
    abstract fun libroDao(): LibroDao

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