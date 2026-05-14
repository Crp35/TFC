package com.example.lectorlibros.util

import android.R.attr.textSize
import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.media.MediaMetadataRetriever
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.ZipFile
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
object PortadaUtils {

    private val cache = ConcurrentHashMap<String, Bitmap?>()

    fun obtenerPortadaPdf(
        context: Context,
        uriString: String,
        anchoPx: Int,
        altoPx: Int
    ): Bitmap? {

        Log.d("PortadaUtils", "obtenerPortadaPdf INICIADO")
        Log.d("PortadaUtils", "URI: $uriString")

        cache[uriString]?.let {
            Log.d("PortadaUtils", "Portada del CACHE")
            return it
        }

        return try {
            var rutaReal = uriString
            var file = File(uriString)

            // Buscar archivo si no existe directamente
            if (!file.exists()) {
                Log.d("PortadaUtils", "Buscando archivo en rutas alternativas...")

                val posiblesRutas = listOf(
                    File(context.filesDir, uriString),
                    File(context.getExternalFilesDir(null), uriString),
                    File("/storage/emulated/0/Download/$uriString"),
                    File("/storage/emulated/0/Documents/$uriString"),
                    File("/storage/emulated/0/$uriString")
                )

                for (posible in posiblesRutas) {
                    if (posible.exists()) {
                        file = posible
                        rutaReal = posible.absolutePath
                        Log.d("PortadaUtils", "Encontrado en: $rutaReal")
                        break
                    }
                }
            }

            // Intentar assets
            if (!file.exists()) {
                try {
                    val nombre = File(uriString).name
                    val input = context.assets.open(nombre)

                    val temp = File(context.cacheDir, nombre)
                    input.use { i ->
                        temp.outputStream().use { o -> i.copyTo(o) }
                    }

                    file = temp
                    rutaReal = temp.absolutePath
                    Log.d("PortadaUtils", "Cargado desde assets: $rutaReal")

                } catch (e: Exception) {
                    Log.e("PortadaUtils", "No encontrado en assets")
                    return null
                }
            }

            if (!file.exists()) {
                Log.e("PortadaUtils", " Archivo no existe")
                return null
            }

            Log.d("PortadaUtils", " Archivo OK: ${file.name}")


            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fileDescriptor)

            if (renderer.pageCount <= 0) {
                renderer.close()
                fileDescriptor.close()
                return null
            }

            val page = renderer.openPage(0)

            // Crear bitmap
            val bitmap = Bitmap.createBitmap(anchoPx, altoPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            // Escalado correcto
            val scale = minOf(
                anchoPx.toFloat() / page.width,
                altoPx.toFloat() / page.height
            )

            val matrix = Matrix()
            matrix.postScale(scale, scale)

            page.render(bitmap, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            // Liberar recursos
            page.close()
            renderer.close()
            fileDescriptor.close()

            cache[uriString] = bitmap
            Log.d("PortadaUtils", "Portada PDF generada")

            bitmap

        } catch (e: Exception) {
            Log.e("PortadaUtils", "Error PDF: ${e.message}", e)
            null
        }
    }

    fun obtenerPortadaEpub(uriEpub: String): Bitmap? {
        cache[uriEpub]?.let { return it }

        return try {
            val file = File(uriEpub)
            if (!file.exists()) {
                Log.e("PortadaUtils", "Archivo EPUB no existe")
                return null
            }

            val zip = ZipFile(file)

            val coverEntry = zip.entries().asSequence().firstOrNull { entry ->
                entry.name.lowercase().contains("cover") &&
                        (entry.name.endsWith(".jpg") ||
                                entry.name.endsWith(".png") ||
                                entry.name.endsWith(".jpeg"))
            }

            val bitmap = coverEntry?.let {
                BitmapFactory.decodeStream(zip.getInputStream(it))
            }

            zip.close()

            if (bitmap != null) cache[uriEpub] = bitmap

            Log.d("PortadaUtils", "Portada EPUB: ${bitmap != null}")
            bitmap

        } catch (e: Exception) {
            Log.e("PortadaUtils", "Error EPUB: ${e.message}", e)
            null
        }
    }

    fun obtenerPortadaAudio(uriAudio: String, anchoPx: Int = 460, altoPx: Int = 550): Bitmap? {
        cache[uriAudio]?.let { return it }

        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(uriAudio)

            val data = mmr.embeddedPicture
            val bitmap = data?.let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }

            mmr.release()

            if (bitmap != null) cache[uriAudio] = bitmap

            Log.d("PortadaUtils", "Portada audio: ${bitmap != null}")
            bitmap


        } catch (e: Exception) {
            Log.e("PortadaUtils", "Error audio: ${e.message}", e)
            null
        }
    }


    fun limpiarCache() {
        cache.clear()
        Log.d("PortadaUtils", "Cache limpiado")
    }
}