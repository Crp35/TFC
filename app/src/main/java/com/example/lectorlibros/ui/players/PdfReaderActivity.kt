package com.example.lectorlibros.ui.players

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.lectorlibros.R
import com.example.lectorlibros.ui.fragment.PdfFragment
import java.io.File

class PdfReaderActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pdf_reader)

        val uri: Uri? = intent?.data
        Log.d("PdfReaderActivity", "URI recibida: $uri")
        Log.d("PdfReaderActivity", "URI scheme: ${uri?.scheme}")

        if (uri != null) {
            try {
                // Pedir permiso persistente si viene de otra app
                if (intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
            } catch (e: SecurityException) {
                Log.e("PdfReaderActivity", "Error al obtener permisos para URI: $uri", e)
            }

            // Guardar PDF externo en almacenamiento interno si es necesario
            val rutaPdf = guardarPdfInternoSiEsExterno(uri)

            // Cargar PdfFragment con la ruta absoluta
            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainerPdf,
                    PdfFragment.newInstance(rutaPdf)
                )
                .commit()
        } else {
            Log.e("PdfReaderActivity", "No se recibió URI de PDF")
        }
    }

    // Copia el PDF externo a filesDir si viene de otra app
    private fun guardarPdfInternoSiEsExterno(uri: Uri): String {
        val file = File(uri.path ?: "")
        return if (!file.exists() && uri.scheme == "content") {
            val destino = File(filesDir, "PDF_" + System.currentTimeMillis() + ".pdf")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    destino.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            destino.absolutePath
        } else {
            file.absolutePath
        }
    }
}