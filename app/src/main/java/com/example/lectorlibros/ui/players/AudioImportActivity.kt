package com.example.lectorlibros.ui.players

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AudioImportActivity : AppCompatActivity() {

    private lateinit var repository: LibroRepository

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializar repositorio y DAO
        val dao = BaseDatos.getDatabase(this).libroDao()
        repository = LibroRepository(
            contexto = this,
            libroDao = dao,
            servicioDescargaPdf = ServicioDescargaPdf(this)
        )

        val uri: Uri? = intent?.data

        if (uri != null) {
            importarAudio(uri)
        } else {
            val mensaje = getString(R.string.error_recepcion_audio)
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun importarAudio(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Obtener el nombre real del archivo para el título
                val nombreArchivoConExtension = obtenerNombreDesdeUri(uri) ?:
                "Audio_${System.currentTimeMillis()}"
                val tituloLimpio = nombreArchivoConExtension
                    .replace(".mp3", "", ignoreCase = true)
                    .replace(".wav", "", ignoreCase = true)
                    .replace(".ogg", "", ignoreCase = true)

                // Copiar el archivo al almacenamiento interno (en hilo secundario)
                val rutaLocal = withContext(Dispatchers.IO) {
                    guardarAudioEnInterno(uri, nombreArchivoConExtension)
                }

                // Verificamos la existencia del título
                val tituloExiste = getString(R.string.ya_existe)
                val yaExiste = repository.existeLibroConTitulo(tituloLimpio)
                if(yaExiste){
                    Toast.makeText(this@AudioImportActivity,
                        "$tituloExiste\" $tituloLimpio\"",
                        Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }

                // Crear la entidad LibroEntity con sus parámetros específicos
                val nuevoLibroAudio = LibroEntity(
                    titulo = tituloLimpio,
                    autor = getString(R.string.autor_desconocido),
                    uriAudio = rutaLocal,
                    tipoLibro = TipoDeLibro.AUDIO,
                    descargado = true,
                    estado = EstadoLibro.NUEVO,
                    leido = false,
                    paginaActual = null,
                    totalPagina = null,
                    posicionMs = 0L,
                    ultimaPosicion = 0
                )

                // Insertamos el audio en Room
                repository.insertLibro(nuevoLibroAudio)

                // Mostrar un mensaje y navegación
                val mensaje = getString(R.string.audiolibro_importado)
                Toast.makeText(this@AudioImportActivity, "$mensaje: $tituloLimpio", Toast.LENGTH_LONG).show()

                // Volver a la MainActivity para refrescar la lista
                val mainIntent = Intent(this@AudioImportActivity, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(mainIntent)
                finish()

            } catch (e: Exception) {
                val mensaje = getString(R.string.mensaje_error)
                Toast.makeText(this@AudioImportActivity, mensaje,
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun guardarAudioEnInterno(uri: Uri, nombre: String): String {
        // Creamos el archivo en el directorio interno de la app
        val mensaje = getString(R.string.mensaje_excepcion)
        val destino = File(filesDir, "AUDIO_${System.currentTimeMillis()}_$nombre")
        contentResolver.openInputStream(uri)?.use { input ->
            destino.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception(mensaje)

        return destino.absolutePath
    }

    private fun obtenerNombreDesdeUri(uri: Uri): String? {
        var nombre: String? = null
        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) nombre = it.getString(index)
                }
            }
        }
        return nombre ?: uri.path?.let { File(it).name }
    }
}