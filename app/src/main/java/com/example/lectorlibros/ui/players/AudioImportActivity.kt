package com.example.lectorlibros.ui.players

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_import)

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
            Toast.makeText(this, "No se recibió ningún audio", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun importarAudio(uri: Uri) {
        lifecycleScope.launch {
            try {
                // 1. Obtener el nombre real del archivo para el título
                val nombreArchivoConExtension = obtenerNombreDesdeUri(uri) ?: "Audio_${System.currentTimeMillis()}"
                val tituloLimpio = nombreArchivoConExtension
                    .replace(".mp3", "", ignoreCase = true)
                    .replace(".wav", "", ignoreCase = true)
                    .replace(".ogg", "", ignoreCase = true)

                // 2. Copiar el archivo al almacenamiento interno (en hilo secundario)
                val rutaLocal = withContext(Dispatchers.IO) {
                    guardarAudioEnInterno(uri, nombreArchivoConExtension)
                }

                // 3. Crear la entidad LibroEntity con tus parámetros específicos
                val nuevoLibroAudio = LibroEntity(
                    titulo = tituloLimpio,
                    autor = "Desconocido",
                    uriAudio = rutaLocal,         // Nombre de parámetro corregido
                    tipoLibro = TipoDeLibro.AUDIO,
                    descargado = true,
                    estado = EstadoLibro.NUEVO,
                    leido = false,
                    paginaActual = null,
                    totalPagina = null,
                    posicionMs = 0L,
                    ultimaPosicion = 0
                )

                // 4. Insertar en Room
                repository.insertLibro(nuevoLibroAudio)

                // 5. Feedback y navegación
                Toast.makeText(this@AudioImportActivity, "Audio importado: $tituloLimpio", Toast.LENGTH_LONG).show()

                // Volver a la MainActivity para refrescar la lista
                val mainIntent = Intent(this@AudioImportActivity, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(mainIntent)
                finish()

            } catch (e: Exception) {
                Log.e("AudioImport", "Error crítico: ${e.message}")
                Toast.makeText(this@AudioImportActivity, "Error al importar el archivo", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun guardarAudioEnInterno(uri: Uri, nombre: String): String {
        // Creamos el archivo en el directorio interno de la app
        val destino = File(filesDir, "AUDIO_${System.currentTimeMillis()}_$nombre")
        contentResolver.openInputStream(uri)?.use { input ->
            destino.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception("No se pudo abrir el flujo de entrada")

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