package com.example.lectorlibros.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import java.io.Serializable

@Entity(tableName = "libro")
data class LibroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long =0,
    val titulo: String,
    val autor: String,
    val paginaActual: Int? = null, // Solo PDF o EPUB
    val totalPagina: Int? = null, // Solo PDF o EPUB
    val posicionMs: Long? = null, // Solo AUDIO
    val estado: EstadoLibro = EstadoLibro.NUEVO,
    val descargado: Boolean = false,
    val tipoLibro: TipoDeLibro,
    val leido: Boolean = false,
    val uriPDF: String? = null,
    val uriAudio: String? = null,
    var ultimaPosicion: Int = 0
): Serializable

