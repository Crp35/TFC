package com.example.lectorlibros.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lectorlibros.data.converter.Conversor
import com.example.lectorlibros.entities.LecturaLibro
import com.example.lectorlibros.entities.LibroEntity

@Database(entities = [LibroEntity::class, LecturaLibro::class],
    version = 5,
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

        // Migración de versión 4 a 5: Se añade el campo uriEpub a la tabla LibroEntity
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE libro ADD COLUMN uriEpub TEXT DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): BaseDatos {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BaseDatos::class.java,
                    "leo_db"
                )
                    .addMigrations(MIGRATION_4_5) // Agrega la migración aquí
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}