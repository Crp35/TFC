package com.example.lectorlibros.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import java.io.Serializable

@Entity(tableName = "libro")
data class LibroEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long =0,
    var titulo: String,
    var autor: String,
    var paginaActual: Int? = null, // Solo PDF o EPUB
    var totalPagina: Int? = null, // Solo PDF o EPUB
    var posicionMs: Long? = null, // Solo AUDIO
    var estado: EstadoLibro = EstadoLibro.NUEVO,
    var descargado: Boolean = false,
    var tipoLibro: TipoDeLibro,
    var leido: Boolean = false,
    var uriPDF: String? = null,
    var uriEpub: String? = null,
    var uriAudio: String? = null,
    var ultimaPosicion: Int = 0,
): Serializable