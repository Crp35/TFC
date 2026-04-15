package com.example.lectorlibros.ui.fragment

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.databinding.FragmentPdfBinding
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import com.example.lectorlibros.util.PortadaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import androidx.core.net.toUri

class PdfFragment : Fragment() {

    private var _binding: FragmentPdfBinding? = null
    private val binding get() = _binding!!

    private var uriPdf: String? = null
    private lateinit var db: BaseDatos

    companion object {
        private const val ARG_URI_PDF = "uri_pdf"

        @JvmStatic
        fun newInstance(uriPdf: String) = PdfFragment().apply {
            arguments = Bundle().apply { putString(ARG_URI_PDF, uriPdf) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uriPdf = arguments?.getString(ARG_URI_PDF)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun alternarMenu() {

        menusVisibles = !menusVisibles
       actualizarEstadoInterfaz(menusVisibles)

        // Aquí también ocultamos el marcador de páginas propio del PDF
        binding.tvPageIndicator.visibility = if (menusVisibles) View.VISIBLE else View.GONE

    }

    private fun actualizarEstadoInterfaz(visible: Boolean){
        // Controlar las barras de MainActvity
        (activity as? MainActivity)?.setMenuVisibility(visible)

        // Controlamos los elementos internos del Fragment(Marcador)
        val visibilidadInterna = if(visible) View.VISIBLE else View.GONE
        binding.tvPageIndicator.visibility = visibilidadInterna

        // DESCOMENTAR SI ERROR( OCULATAMOS LA PORTADA)
        binding.ivPortada.visibility = visibilidadInterna
    }

    private var menusVisibles = false //Empezamos con la pantalla completa
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Al entrar, ocultamos todo por defecto, para así estar en pantalla completa
        (activity as? MainActivity)?.setMenuVisibility(false)

        // Configuramos el toque en el contenedor del PDF
        binding.rooPdf.setOnClickListener {
            alternarMenu()
        }

        binding.tvEmpty.text = uriPdf ?: getString(R.string.mensaje_no_libros)

        db = BaseDatos.getDatabase(requireContext())
        val dao = db.libroDao()

        lifecycleScope.launch {
            var libroEntity: LibroEntity? = uriPdf?.let { dao.getLibro(it) }

            // Insertar en BD si no existe
            if (libroEntity == null && uriPdf != null) {
                val localPath = guardarPdfInterno(uriPdf!!)
                libroEntity = LibroEntity(
                    titulo = File(localPath).name,
                    autor = getString(R.string.nombreAutor),
                    tipoLibro = TipoDeLibro.PDF,
                    uriPDF = localPath,
                    leido = false,
                    descargado = true,
                    estado = EstadoLibro.NUEVO,
                    paginaActual = 0,
                    totalPagina = 0
                )
                dao.insertLibro(libroEntity)
            }

            libroEntity?.let { libro ->

                // Título en la UI
                (activity as? MainActivity)?.binding?.tvTitulo?.text = libro.titulo

                val pdfFile = File(libro.uriPDF)

                // PORTADA (background)
                lifecycleScope.launch(Dispatchers.IO) {
                    val anchoPx = (300 * resources.displayMetrics.density).toInt()
                    val altoPx = (600 * resources.displayMetrics.density).toInt()

                    val portadaBitmap = if (pdfFile.exists()) {
                        PortadaUtils.obtenerPortadaPdf(
                            requireContext(),
                            pdfFile.absolutePath,
                            anchoPx,
                            altoPx
                        )
                    } else null

                    launch(Dispatchers.Main) {
                        if (portadaBitmap != null) {
                            binding.ivPortada.setImageBitmap(portadaBitmap)
                        } else {
                            binding.ivPortada.setImageResource(R.drawable.ic_libro_abierto)
                        }
                    }
                }

                // ABRIR PDF REAL
                if (pdfFile.exists()) {

                    binding.pdfView.fromFile(pdfFile)
                        .defaultPage(libro.paginaActual ?: 0)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .enableAnnotationRendering(true)
                        .onTap {
                            alternarMenu()
                            true
                        }

                        // TOTAL DE PÁGINAS
                        .onLoad { pageCount ->
                            Log.d("PDF_DEBUG", "Total páginas: $pageCount")

                            libro.totalPagina = pageCount

                            lifecycleScope.launch(Dispatchers.IO) {
                                dao.actualizarLibro(libro)
                            }
                        }

                        // PROGRESO DE PÁGINA EN LOS LIBROS
                        .onPageChange { page, pageCount ->
                            val terminado = (page + 1) >= pageCount
                            val nuevoLibro = libro.copy(
                                paginaActual = page,
                                totalPagina = pageCount,
                                estado = if (terminado) EstadoLibro.COMPLETADO else EstadoLibro.EN_PROGRESO
                            )

                           // DESCOMENTAR SI ERROR  libro.paginaActual = page

                            binding.tvPageIndicator.text =
                                getString(R.string.indicador_pagina, page + 1, pageCount)

                            lifecycleScope.launch(Dispatchers.IO) {
                                dao.actualizarLibro(nuevoLibro)
                            }
                        }

                        .load()

                    binding.tvEmpty.visibility = View.GONE

                } else {
                    Log.e("PDF_ERROR", "El archivo no existe: ${libro.uriPDF}")
                    binding.tvEmpty.visibility = View.VISIBLE
                }
            } ?: run {
                binding.tvEmpty.visibility = View.VISIBLE
            }
        }
    }

    // Guardar PDF externo a interno si viene de otra app
    private fun guardarPdfInterno(uriString: String): String {
        val uri = uriString.toUri()
        val file = File(uriString)

        return if (!file.exists() && uri.scheme == "content") {

            val destino = File(
                requireContext().filesDir,
                "PDF_" + System.currentTimeMillis() + ".pdf"
            )

            try {
                requireContext().contentResolver.openInputStream(uri)?.use { input ->
                    destino.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            destino.absolutePath

        } else {
            uriString
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // AL salir del fragment, nos aseguramos de devolver los menús
        (activity as? MainActivity)?.setMenuVisibility(true)
        _binding = null
    }
}