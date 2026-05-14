package com.example.lectorlibros.ui.players

import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.databinding.ActivityMainBinding
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.util.PortadaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val ARG_URI_PDF = "uri_pdf"

class PdfReader : Fragment() {

    lateinit var binding: ActivityMainBinding

    private var uriPdf: String? = null
    private lateinit var imgPortada: ImageView
    private var progressBar: ProgressBar? = null
    private var tvError: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            uriPdf = it.getString(ARG_URI_PDF)
            Log.d("PdfReader", "URI PDF recibida: $uriPdf")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_pdf_reader, container, false)
        imgPortada = view.findViewById(R.id.ivPortada)
        progressBar = view.findViewById(R.id.progressBar)
        tvError = view.findViewById(R.id.tvError)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Al tocar el fondo del lector
        binding.root.setOnClickListener {
            // Solo ejecutamos la alternancia si estamos en horizontal
            if(resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE){
                (activity as? MainActivity)?.alternarControlesLectura()
            }
        }

        progressBar?.visibility = View.VISIBLE
        tvError?.visibility = View.GONE
        imgPortada.setImageResource(R.drawable.book_open_book_read_icon)

        uriPdf?.let { pdf ->
            Log.d("PdfReader", "URI válida: $pdf")

            // Verificar que el archivo existe
            val file = File(pdf)
            Log.d("PdfReader", "Archivo existe: ${file.exists()}")
            Log.d("PdfReader", "Tamaño del archivo: ${file.length()} bytes")

            val anchoPx = (300 * resources.displayMetrics.density).toInt()
            val altoPx = (600 * resources.displayMetrics.density).toInt()
            Log.d("PdfReader", "Dimensiones: ${anchoPx}x${altoPx} px")

            lifecycleScope.launch {
                Log.d("PdfReader", "Iniciando carga de portada en background")

                val bitmap: Bitmap? = withContext(Dispatchers.IO) {
                    PortadaUtils.obtenerPortadaPdf(requireContext(), pdf, anchoPx, altoPx)
                }

                Log.d("PdfReader", "Carga completada. Bitmap: ${bitmap != null}")

                progressBar?.visibility = View.GONE

                if (bitmap != null) {
                    imgPortada.setImageBitmap(bitmap)
                    tvError?.visibility = View.GONE
                    Log.d("PdfReader", "Portada mostrada correctamente")
                } else {
                    tvError?.visibility = View.VISIBLE
                    tvError?.text = "No se pudo cargar la portada del PDF"
                    Log.e("PdfReader", "Bitmap es null")
                }
            }
        } ?: run {
            Log.e("PdfReader", "URI PDF es NULL")
            progressBar?.visibility = View.GONE
            tvError?.visibility = View.VISIBLE
            tvError?.text = "URI del PDF no proporcionada"
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance(uriPdf: String) =
            PdfReader().apply {
                Log.d("PdfReader", "newInstance llamado con: $uriPdf")
                arguments = Bundle().apply {
                    putString(ARG_URI_PDF, uriPdf)
                }
            }
    }
}