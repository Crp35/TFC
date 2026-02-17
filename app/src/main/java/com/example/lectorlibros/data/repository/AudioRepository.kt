package com.example.lectorlibros.data.repository

import android.content.Context
import com.example.lectorlibros.data.db.LibroDao
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AudioRepository {

    // Carpeta donde se guardan los audios
    private fun getAudioDir(context: Context): File {
        val dir = File(context.filesDir, "audio")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // Insertar audio en BD y copiar archivo a sandbox
    suspend fun guardarAudio(
        context: Context,
        dao: LibroDao,
        titulo: String,
        autor: String,
        resRawId: Int // puedes cambiar a URL si descargas de Internet
    ) {
        withContext(Dispatchers.IO) {
            val existente = dao.getLibroByTitulo(titulo)
            if (existente != null) return@withContext

            val audioDir = getAudioDir(context)
            val audioFile = File(audioDir, "$titulo.mp3")

            // Copiar desde res/raw al archivo si no existe
            if (!audioFile.exists()) {
                context.resources.openRawResource(resRawId).use { input ->
                    audioFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            val nuevoAudio = LibroEntity(
                titulo = titulo,
                autor = autor,
                tipoLibro = TipoDeLibro.AUDIO,
                uriAudio = audioFile.absolutePath,
                posicionMs = 0L,
                descargado = true,
                estado = EstadoLibro.NUEVO
            )
            dao.insertLibro(nuevoAudio)
        }
    }

    // Obtener todos los audios
    fun getAudios(dao: LibroDao) = dao.getLibrosAudio()
}
