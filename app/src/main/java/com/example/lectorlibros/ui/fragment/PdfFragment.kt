package com.example.lectorlibros.ui.fragment

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
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

    private var paginaActual: Int = 0

    companion object {
        private const val ARG_URI_PDF = "uri_pdf"
        private const val KEY_PAGINA_ACTUAL = "key_pagina_actual"

        @JvmStatic
        fun newInstance(uriPdf: String) = PdfFragment().apply {
            arguments = Bundle().apply { putString(ARG_URI_PDF, uriPdf) }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.getString(ARG_URI_PDF, uriPdf)
        outState.putInt(KEY_PAGINA_ACTUAL, binding.pdfView.currentPage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uriPdf = arguments?.getString(ARG_URI_PDF)
            ?: arguments?.getString(ARG_URI_PDF)
        paginaActual = savedInstanceState?.getInt(KEY_PAGINA_ACTUAL)
            ?: paginaActual
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPdfBinding.inflate(inflater, container, false)
        return binding.root
    }

    // FUNCIÓN QUE MUESTRA Y OCULTA LOS ELEMENTOS DE CONTROL EN ORIENTACIÓN VERTICAL Y HORIZONTAL
     private fun alternarMenu() {
        // Aquí también ocultamos el marcador de páginas propio del PDF

        //INICIO MODO HORIZONTAL  Modo lectura limpia
        val esHorizontal = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (esHorizontal) return // No alternar en horizontal, siempre activo
        menusVisibles = !menusVisibles
        actualizarEstadoInterfaz(menusVisibles)
         // FIN MODO HORIZONTAL
    }

    /**
     * Actualiza la visibilidad de la interfaz principal del lector.
     *
     * Esta función controla qué elementos se muestran u ocultan tanto en la actividad principal
     * como dentro del fragmento de lectura, dependiendo del estado de visibilidad recibido.
     *
     * - En la actividad principal, muestra u oculta la barra de menú superior.
     * - En el fragmento, controla la visibilidad del indicador de página y la portada.
     *
     * @param visible Si es true, se muestran los controles de interfaz.
     *                Si es false, se ocultan para modo lectura limpia.
     */
    private fun actualizarEstadoInterfaz(visible: Boolean){
        // Controlar las barras de MainActvity
        (activity as? MainActivity)?.setMenuVisibility(visible)

        // Controlamos los elementos internos del Fragment(Marcador)
        val visibilidadInterna = if(visible) View.VISIBLE else View.GONE
        binding.tvPageIndicator.visibility = visibilidadInterna

        binding.ivPortada.visibility = visibilidadInterna
    }


   private var menusVisibles = false //Empezamos con la pantalla completa
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //  INCIO MODO HORIZONTAL Click del botón de volver atrás
        binding.btnVolverAtras.setOnClickListener {

            val mainActivity = activity as? MainActivity ?: return@setOnClickListener

            mainActivity.cargarFragment(
                BibliotecaFragment.newInstance(
                    com.example.lectorlibros.data.repository.LibroRepository(
                        requireContext(),
                        com.example.lectorlibros.data.db.BaseDatos
                            .getDatabase(requireContext())
                            .libroDao(),
                        com.example.lectorlibros.data.repository
                            .ServicioDescargaPdf(requireContext())
                    )
                )
            )

            mainActivity.binding.tvTitulo.text =
                getString(R.string.biblioteca)

            mainActivity.setMenuVisibility(true)

            mainActivity.binding.bottomNavigation.selectedItemId =
                R.id.menu_biblioteca
        }

        // FIN MODO HORIZONTAL

        // Al entrar, ocultamos todo por defecto, para así estar en pantalla completa
        (activity as? MainActivity)?.setMenuVisibility(false)

        // Configuramos el toque en el contenedor del PDF
        binding.rooPdf.setOnClickListener {
            //DESCOMENTAR SI ERROR alternarMenu()
        }

        binding.tvEmpty.text = uriPdf ?: getString(R.string.mensaje_no_libros)

        // DEPURACIÓN
        Log.d("PDF_DEBUG", "URI PDF RECIBIDO: $uriPdf")
        db = BaseDatos.getDatabase(requireContext())
        val dao = db.libroDao()

        lifecycleScope.launch {
            var libroEntity: LibroEntity? = uriPdf?.let { dao.getLibro(it) }
            val localPath = guardarPdfInterno(uriPdf!!)
            val fallback =
                File(localPath).name.removeSuffix(".pdf").removeSuffix(".PDF")
            // Insertar en BD si no existe
            if (libroEntity == null && uriPdf != null) {

                libroEntity = LibroEntity(
                    titulo = fallback,
                    autor = getString(R.string.autor_desconocido),
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

                val pdfPath = libro.uriPDF ?: return@let // Salimos si no hay PDF
                val pdfFile = File(pdfPath)


                // PORTADA (background)
                lifecycleScope.launch(Dispatchers.IO) {
                    val anchoPx = (300 * resources.displayMetrics.density).toInt()
                    val altoPx = (700 * resources.displayMetrics.density).toInt()

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

                    binding.pdfView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    binding.pdfView.fromFile(pdfFile)
                        .defaultPage(libro.paginaActual ?: 0)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .enableAnnotationRendering(true)
                        .onTap {
                            val mainActivity = (activity as? MainActivity)
                            mainActivity?.binding?.ivVolverAtras?.visibility = View.VISIBLE
                            mainActivity?.binding?.ivVolverAtras?.alpha = 1f
                            alternarMenu()
                            alternarInterfazLectura()
                            //ocultarControles()
                            true
                        }


                        // TOTAL DE PÁGINAS
                        .onLoad { pageCount ->
                            val pagina = libro.paginaActual?:0
                            binding.pdfView.post {
                                binding.pdfView.jumpTo(pagina, false)
                            }
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

                            libro.paginaActual = page
                            libro.totalPagina = pageCount
                            libro.estado = nuevoLibro.estado

                            binding.tvPageIndicator.text =
                                getString(R.string.indicador_pagina, page + 1, pageCount)

                            lifecycleScope.launch(Dispatchers.IO) {
                                dao.actualizarLibro(nuevoLibro)
                            }
                            paginaActual = page
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

    private fun alternarInterfazLectura() {

        val activity = activity as? MainActivity

        if (resources.configuration.orientation ==
            Configuration.ORIENTATION_LANDSCAPE) {

            val mostrar = binding.tvPageIndicator.visibility != View.VISIBLE

            // Indicador de páginas
            binding.tvPageIndicator.visibility =
                if (mostrar) View.VISIBLE else View.GONE

            // Nuevo botón flotante de volver
            binding.btnVolverAtras.visibility =
                if (mostrar) View.VISIBLE else View.GONE

            // Ocultamos  la interfaz completa
            activity?.binding?.bottomNavigation?.visibility = View.GONE
            activity?.binding?.layoutColecciones?.visibility = View.GONE
            activity?.binding?.tvTitulo?.visibility = View.GONE
        }
    }

    // Fin de alternancia

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
    override fun onResume() {
        super.onResume()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
    }

    override fun onPause() {
        super.onPause()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // AL salir del fragment, nos aseguramos de devolver los menús
        (activity as? MainActivity)?.setMenuVisibility(true)
        (activity as MainActivity).binding.ivVolverAtras.visibility = View.VISIBLE
        (activity as MainActivity).binding.ivVolverAtras.alpha = 1f
        _binding = null

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}