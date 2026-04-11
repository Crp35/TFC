package com.example.lectorlibros.data.converter

import androidx.room.TypeConverter
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro

/**
 * Con esto Room podrá convertir automáticamente el enum TipoDeLibro a String
 * en la base de datos y viceversa.
 * */
class Conversor {

    // TipoDeLibro a String
    @TypeConverter
    fun fromFormatoLibro(formato: TipoDeLibro): String {
        return formato.name
    }

    @TypeConverter
    fun toFormatoLibro(formato: String): TipoDeLibro {
        return TipoDeLibro.valueOf(formato)
    }

    @TypeConverter
    fun fromEstadoLibro(estado: EstadoLibro): String{
        return estado.name
    }
}
