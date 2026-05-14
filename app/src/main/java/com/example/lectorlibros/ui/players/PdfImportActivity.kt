package com.example.lectorlibros.ui.players

import android.os.Build
import androidx.annotation.RequiresApi


import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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

class PdfImportActivity : AppCompatActivity() {

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
            importarPdf(uri)
        } else {
            val mensaje = getString(R.string.error_recepcion_pdf)
            Toast.makeText(this, mensaje, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun importarPdf(uri: Uri) {
        lifecycleScope.launch {
            try {
                // Obtener el nombre real del archivo para el título
                val nombreArchivoConExtension = obtenerNombreDesdeUri(uri) ?: "PDF_${System.currentTimeMillis()}"
                val fallback = "PDF_${System.currentTimeMillis()}.PDF"
                val nombreFinal = nombreArchivoConExtension.ifBlank { fallback }
                val tituloLimpio = nombreFinal
                    .removeSuffix(".pdf")
                    .removeSuffix(".PDF")


                // Copiar el PDF al almacenamiento interno
                val rutaLocal = withContext(Dispatchers.IO) {
                    guardarPdfEnInterno(uri, nombreArchivoConExtension)
                }

                // Verificamos el existencia del título
                val tituloExiste = getString(R.string.ya_existe)
                val yaExiste = repository.existeLibroConTitulo(tituloLimpio)
                if(yaExiste){
                    Toast.makeText(this@PdfImportActivity, "$tituloExiste\" $tituloLimpio\"",
                        Toast.LENGTH_LONG).show()
                    finish()
                    return@launch
                }

                // Crear LibroEntity tipo PDF
                val nuevoLibroPdf = LibroEntity(
                    titulo = tituloLimpio,                  // el nombre original
                    autor = getString(R.string.autor_desconocido),
                    uriPDF = rutaLocal,                      // solo PDF
                    tipoLibro = TipoDeLibro.PDF,
                    descargado = true,
                    estado = EstadoLibro.NUEVO,
                    leido = false,
                    paginaActual = 0,                        // 0 páginas hasta que se cargue el PDF
                    totalPagina = 0,
                    posicionMs = null,                       // no aplica para PDF
                    ultimaPosicion = 0
                )

                // Guardar en Room
                repository.insertLibro(nuevoLibroPdf)

                //  Mostrar mensaje y volver a MainActivity
                val importado = getString(R.string.libro_importado)

                Toast.makeText(this@PdfImportActivity, "$importado: $tituloLimpio", Toast.LENGTH_LONG).show()

                val mainIntent = Intent(this@PdfImportActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(mainIntent)
                finish()

            } catch (e: Exception) {
                val mensaje = getString(R.string.mensaje_error)
                Toast.makeText(this@PdfImportActivity, "$mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun guardarPdfEnInterno(uri: Uri, nombre: String): String {
        val mensaje = getString(R.string.mensaje_excepcion)
        val destino = File(filesDir, "PDF_${System.currentTimeMillis()}_$nombre")
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

        // Fallback por si el scheme no es "content"
        if (nombre == null) {
            nombre = uri.path?.let { File(it).name }
        }

        // Log para confirmar qué nombre se está leyendo
        return nombre
    }

}