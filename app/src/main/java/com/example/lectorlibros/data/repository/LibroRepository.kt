package com.example.lectorlibros.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.LibroDao
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.enums.TipoColeccion
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

@RequiresApi(Build.VERSION_CODES.Q)
class LibroRepository(
    private val contexto: Context,
    private val libroDao: LibroDao,
    private val servicioDescargaPdf: ServicioDescargaPdf
) {

    suspend fun  actualizaLibros(libro: LibroEntity) {
        libroDao.actualizarLibro(libro)
    }


    // Carpeta sandbox para audios
    private val audioDir: File by lazy {
        File(contexto.filesDir, "audio").apply { if (!exists()) mkdirs() }
    }

    fun buscarLibrosPorTitulo(titulo: String): Flow<List<LibroEntity>> {
        return libroDao.buscarLibrosPorTitulo(titulo)
    }

    // Método que nos permite renombrar los libros
    suspend fun renombrarLibros(id: Long, nuevoTitulo: String, nuevoAutor: String){
        libroDao.actualizarLibro(id, nuevoTitulo, nuevoAutor)
    }

    // Método que nos permite elminr libros
    suspend fun deleteLibro(libro: LibroEntity) {
        libroDao.eliminarLibros(libro.id)
    }



    // Descarga de PDFs
    suspend fun descargarLibro(
        libro: LibroEntity,
        urlPDF: String
    ) {
        val uri = servicioDescargaPdf.descargarPDF(urlPDF, libro.titulo)
        uri?.let {
            val libroActualizado = libro.copy(
                descargado = true,
                uriPDF = it.toString()
            )
            libroDao.insertLibro(libroActualizado)
        }
    }


    // Guardar audio en sandbox + BD
    suspend fun guardarAudio(
        titulo: String,
        autor: String,
        origen: InputStream,      // InputStream de raw o descarga
        nombreArchivo: String     // nombre final en sandbox
    ): LibroEntity = withContext(Dispatchers.IO) {
        // Copiar archivo al sandbox
        val archivoDestino = File(audioDir, nombreArchivo)
        origen.use { input ->
            archivoDestino.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // Crear entidad y guardar en BD
        val libro = LibroEntity(
            titulo = titulo,
            autor = autor,
            tipoLibro = TipoDeLibro.AUDIO,
            uriAudio = archivoDestino.absolutePath,
            descargado = true
        )
        libroDao.insertLibro(libro)
        libro
    }

    // -----------------------
    // Funciones de inserción y consulta
    // -----------------------
    suspend fun insertLibro(libro: LibroEntity) {
        libroDao.insertLibro(libro)
    }

    fun getAllLibros(): Flow<List<LibroEntity>> = libroDao.getAllLibros()
    fun getLibrosPDF(): Flow<List<LibroEntity>> = libroDao.getLibrosPDF()
    fun getLibrosAudio(): Flow<List<LibroEntity>> = libroDao.getLibrosAudio(TipoDeLibro.AUDIO)

    suspend fun cambiarTituloAutor(libro: LibroEntity) {
        libroDao.actualizarLibro(libro.id, libro.titulo, libro.autor)
    }

    suspend fun getLibroByTitulo(titulo: String) = libroDao.getLibroByTitulo(titulo)
    suspend fun getLibroByAutor(autor: String) = libroDao.getLibroByAutor(autor)

    suspend fun contarLibros(tipo: TipoColeccion): Int {
        return when (tipo) {
            TipoColeccion.TODOS -> libroDao.countTotalLibros()
            TipoColeccion.DESCARGADOS -> libroDao.countTotalLibrosDescargados()
            TipoColeccion.TERMINADOS -> libroDao.countLibrosLeidos()
            TipoColeccion.PDF -> libroDao.countLibrosTotalLibrosPDF()
            TipoColeccion.AUDIO -> libroDao.countLibrosTotalLibrosAudio()
        }
    }

    // Nuevo: obtener libro por id
    suspend fun getLibroById(id: Long): LibroEntity? = libroDao.getLibroById(id)

    // -----------------------
    // POBLAR BD CON TODOS LOS AUDIOS DE RAW
    // -----------------------
    suspend fun poblarBDDesdeRaw() = withContext(Dispatchers.IO) {
        // Lista de todos los audios de raw
        val todosLosAudiosRaw = arrayOf(
            R.raw.sutras,
            // Agrega aquí más audios de raw según necesites
            // R.raw.otro_audio
        )

        for (resId in todosLosAudiosRaw) {
            try {
                val afd = contexto.resources.openRawResourceFd(resId) ?: continue
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)

                // Extraer metadata
                val titulo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: contexto.resources.getResourceEntryName(resId)  // fallback: nombre del recurso
                val autor = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
                    ?: contexto.getString(R.string.nombreAutor)

                // Copiar archivo al sandbox
                contexto.resources.openRawResource(resId).use { input ->
                    val nombreArchivo = "${titulo.replace(" ", "_")}.mp3"
                    guardarAudio(titulo, autor, input, nombreArchivo)
                }

                retriever.release()
                afd.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // -----------------------
    // Función de prueba para insertar libros
    // -----------------------
    /*suspend fun pruebaInsertarLibros() {
        if (libroDao.countLibrosTotalLibrosAudio() == 0) {
            poblarBDDesdeRaw()

            // PDF de prueba
            insertLibro(
                LibroEntity(
                    titulo = "El principito",
                    autor = "Antoine de Saint-Exupéry",
                    tipoLibro = TipoDeLibro.PDF
                )
            )
        }
    }*/

}
