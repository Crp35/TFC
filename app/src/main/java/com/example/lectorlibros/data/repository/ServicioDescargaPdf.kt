package com.example.lectorlibros.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@RequiresApi(Build.VERSION_CODES.Q)
class ServicioDescargaPdf(
    private val context: Context

){

    suspend fun descargarPDF(
        urlPDF: String,
        nombreLibro: String
    ): Uri? {
        return withContext(Dispatchers.IO){
            try {
                val url = URL(urlPDF)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()
                if (connection.responseCode != HttpURLConnection.HTTP_OK){
                    return@withContext null
                }

                val inputStream = connection.inputStream

                guardarEnMediaStore(nombreLibro, inputStream)

            } catch (e: Exception){
                e.printStackTrace()

                null
            }
        }

    }

    private fun guardarEnMediaStore(
        nombreLibro: String,
        inputStream: java.io.InputStream
    ): Uri? {

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "$nombreLibro.pdf")
            put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val resolver = context.contentResolver
        val collection = MediaStore.Downloads.getContentUri(
            MediaStore.VOLUME_EXTERNAL_PRIMARY
        )

        val uri = resolver.insert(collection, contentValues)

        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
        }

        return uri
        }
    }

