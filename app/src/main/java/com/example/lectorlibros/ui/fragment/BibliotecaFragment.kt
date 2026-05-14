package com.example.lectorlibros.ui.fragment

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.databinding.FragmentBibliotecaBinding
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.adapter.LibrosAdapter
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class BibliotecaFragment : Fragment() {

    private var _binding: FragmentBibliotecaBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: LibroRepository
    private val listaLibros = mutableListOf<LibroEntity>()
    private lateinit var adapter: LibrosAdapter

    private var coleccionActual: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBibliotecaBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Función para mostrar el menú CRUD
    @RequiresApi(Build.VERSION_CODES.Q)
    fun showPopupMenu(libro: LibroEntity, ancla: View) {
        val popup = PopupMenu(requireContext(), ancla)

        popup.inflate(R.menu.menu_item_libro)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.action_rename -> {
                    // Llamada a la función de renombrar
                    mostrarDialogoRenombrar(libro)
                    true
                }

                R.id.action_delete -> {
                    // Llamada a la función de eliminación real
                    confirmarEliminacion(libro)
                    true
                }

                R.id.action_import -> {
                    // Llamada a la función de importación
                    importarArchivos.launch(
                        arrayOf(
                            "application/pdf",
                            "audio/mpeg",
                            "audio/wav",
                            "audio/mp3",
                            "audio/ogg",
                            "audio/aac",
                            "audio/flac",
                            "audio/midi",
                            "audio/x-midi",
                            "audio/m4a",
                            "application/epub+zip"
                        )
                    )
                    true
                }

                else -> false
            }
        }
        popup.show()
    }

    // Inicio método para mostrar diálogo renombrar libros

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mostrarDialogoRenombrar(libro: LibroEntity) {
        val abortar = getString(R.string.cancelar)
        val salvarEdicion = getString(R.string.gurdar_libro)
        val textString = getString(R.string.renombrar_libro)
        val builder = MaterialAlertDialogBuilder(requireContext())
        //DESCOMENTAR SI ERRORval builder = AlertDialog.Builder(requireContext())
        builder.setTitle(textString)

        // Creamos el campo de texto
        val input = EditText(requireContext())
        input.setText(libro.titulo) // Prerellenamos con el título actual
        input.selectAll() // Seleccionamos todo el texto para facilitar la edición

        // Un contenedor para un diseño más bonito
        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60,20,60,0) // Izquierda, Arriba, Derecha, Abajo
        input.layoutParams = params
        container.addView(input)
        builder.setPositiveButton(salvarEdicion) { _, _ ->
            val nuevoTitulo = input.text.toString().trim()
            if(nuevoTitulo.isNotEmpty()) {
                // Ejecutamos la actualización en una corrutina
                viewLifecycleOwner.lifecycleScope.launch {
                    // Comprobamos si el nuevo título ya existe
                    val tituloExiste = getString(R.string.ya_existe)
                    val yaExiste = listaLibros.any{
                        it.titulo.equals(nuevoTitulo, ignoreCase = true) && it.id != libro.id
                    }
                    if(yaExiste){
                        Toast.makeText(requireContext(), "$tituloExiste\" $nuevoTitulo\"",
                            Toast.LENGTH_LONG).show()
                        return@launch
                    }else{
                        repository.renombrarLibros(libro.id, nuevoTitulo, libro.autor)
                        Toast.makeText(requireContext(), getString(
                            R.string.titulo_cambiado),
                            Toast.LENGTH_SHORT).show()
                    }
                    repository.renombrarLibros(libro.id, nuevoTitulo, libro.autor)
                    Toast.makeText(requireContext(), getString(R.string.titulo_cambiado),
                        Toast.LENGTH_LONG).show()
                }
            }

        }
        builder.setNegativeButton(abortar, null)
        builder.setView(container) // Si error en la compilación prueba a borrar
        //DESCOMENTAR SI ERROR builder.show()
        val dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(android.graphics.Color.WHITE)

            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(android.graphics.Color.WHITE)
        }

        dialog.show()
    }

    // Fin método para mostrar diálogo renombrar libros

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun confirmarEliminacion(libro: LibroEntity) {

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar libro")
            .setMessage("¿Estás seguro de que quieres eliminar '${libro.titulo}'?")
            .setPositiveButton("Eliminar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {

            val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val negative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            // Forzamos estilo uniforme (evita color "error" de Material3)
            positive.setTextColor(android.graphics.Color.WHITE)
            negative.setTextColor(android.graphics.Color.WHITE)

            positive.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteLibro(libro)

                    Toast.makeText(
                        requireContext(),
                        "${libro.titulo} eliminado",
                        Toast.LENGTH_SHORT
                    ).show()

                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = LibroRepository(
            contexto = requireContext(),
            libroDao = BaseDatos.getDatabase(requireContext()).libroDao(),
            servicioDescargaPdf = ServicioDescargaPdf(requireContext())
        )

        adapter = LibrosAdapter(
            listaLibros,
            this,
            { libro ->
                abrirLibro(libro)
            },
            { libro, viewAncla ->
                // Vinculación con la función local
                showPopupMenu(libro, viewAncla)
            }
        )

        binding.rvBiblioteca.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvBiblioteca.adapter = adapter

        cargarLibros()
    }

    private var cargarJob: kotlinx.coroutines.Job? = null

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun cargarLibros() {

        if (!isAdded || view == null) return // Evitamos llamar a la UI cundo el Fragment no está activo
        cargarJob?.cancel() // evita múltiples collectors
        cargarJob = viewLifecycleOwner.lifecycleScope.launch {

            val pdf = getString(R.string.opcion_pdf)
            val audio = getString(R.string.opcion_audiolibros)
            val terminados = getString(R.string.opcion_terminados)
            val descargados = getString(R.string.descargados)

            repository.getAllLibros().collect { listaCompleta ->

                val listaFinal = when (coleccionActual) {
                    pdf -> listaCompleta.filter { it.tipoLibro == TipoDeLibro.PDF }
                    audio -> listaCompleta.filter { it.tipoLibro == TipoDeLibro.AUDIO }
                    descargados -> listaCompleta.filter { it.descargado }
                    terminados -> listaCompleta.filter { it.estado == EstadoLibro.COMPLETADO }
                    else -> listaCompleta
                }

                listaLibros.clear()
                listaLibros.addAll(listaFinal)

                binding.tvEmpty.visibility =
                    if (listaLibros.isEmpty()) View.VISIBLE else View.GONE

                binding.rvBiblioteca.visibility =
                    if (listaLibros.isEmpty()) View.GONE else View.VISIBLE

                adapter.actualizarLibros(listaLibros)
            }
        }
    }

    // Función que expone el launcher al exterior
    fun lanzarImportacion(){
        importarArchivos.launch(
            arrayOf(
                "application/pdf",
                "audio/mpeg",
                "audio/wav",
                "audio/mp3",
                "audio/ogg",
                "audio/aac",
                "audio/flac",
                "audio/midi",
                "audio/x-midi",
                "audio/m4a",
                "application/epub+zip"
            )
        )

    }

    // Función para importarAchivos
    private val importarArchivos = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let {importarArchivos(it)}
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun importarArchivos(uri: Uri) {
        val mimeType = requireContext().contentResolver.getType(uri)
        viewLifecycleOwner.lifecycleScope.launch {
            try{
                //Obtenemos el nombre real del archivo
                val nombre = obtenerNombreDesdeUri(uri)
                    ?: "Archivo_${System.currentTimeMillis()}"

                when{
                    // PDF
                    mimeType?.startsWith("application/pdf") == true -> {
                        val rutaLocal = withContext(Dispatchers.IO){
                            guardarArchivoEnInterno(uri, "PDF_${System.currentTimeMillis()}_$nombre.pdf")
                        }
                        val tituloLimpio = nombre.removeSuffix(".pdf")
                            .removeSuffix(".PDF")

                        val yaExiste = repository.existeLibroConTitulo(tituloLimpio)
                        if(yaExiste){
                            Toast.makeText(requireContext(), getString(R.string.ya_existe),
                                Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        repository.insertLibro(
                            LibroEntity(
                                titulo = tituloLimpio,
                                autor = getString(R.string.autor_desconocido),
                                uriPDF = rutaLocal,
                                tipoLibro = TipoDeLibro.PDF,
                                descargado = true,
                                estado = EstadoLibro.NUEVO,
                                leido = false,
                                paginaActual = 0,
                                totalPagina = 0,
                            )
                        )
                        Toast.makeText(requireContext(),
                            "${getString(R.string.libro_importado)} $tituloLimpio",
                            Toast.LENGTH_LONG).show()
                    }

                    // AUDIO
                    mimeType?.startsWith("audio/") == true ||
                        nombre.endsWith(".mp3", ignoreCase = true) ||
                            nombre.endsWith(".wav", ignoreCase = true) ||
                                nombre.endsWith(".ogg", ignoreCase = true) ||
                                    nombre.endsWith(".flac", ignoreCase = true) ||
                                        nombre.endsWith(".midi", ignoreCase = true) ||
                                            nombre.endsWith(".mid", ignoreCase = true) ||
                                                nombre.endsWith(".m4a", ignoreCase = true) -> {


                       val rutaLocal = withContext(Dispatchers.IO){
                           guardarArchivoEnInterno(uri, "AUDIO_${System.currentTimeMillis()}_$nombre")

                       }
                       val tituloLimpio = nombre
                           .replace(".mp3","", ignoreCase = true)
                           .replace(".wav", "", ignoreCase = true)
                           .replace(".ogg", "", ignoreCase = true)
                           .replace(".flac", "", ignoreCase = true)
                           .replace(".midi", "", ignoreCase = true)
                           .replace(".mid", "", ignoreCase = true)
                           .replace(".m4a", "", ignoreCase = true)

                        val yaExiste = repository.existeLibroConTitulo(tituloLimpio)
                        if(yaExiste){
                            Toast.makeText(requireContext(), getString(R.string.ya_existe),
                                Toast.LENGTH_LONG).show()
                            return@launch
                        }
                        repository.insertLibro(
                            LibroEntity(
                                titulo = tituloLimpio,
                                autor = getString(R.string.autor_desconocido),
                                uriAudio = rutaLocal,
                                tipoLibro = TipoDeLibro.AUDIO,
                                descargado = true,
                                estado = EstadoLibro.NUEVO,
                                leido = false,
                                posicionMs = 0L,
                                ultimaPosicion = 0
                            )
                        )
                        Toast.makeText(requireContext(),
                            "${getString(R.string.audiolibro_importado)}: $tituloLimpio",
                            Toast.LENGTH_LONG).show()
                    }

                    // EPUB
                    mimeType == "application/epub+zip" ||
                            nombre.endsWith(".epub", ignoreCase = true) -> {
                                val rutaLocal = withContext(Dispatchers.IO){
                                    guardarArchivoEnInterno(uri, "EPUB_${System.currentTimeMillis()}_$nombre")

                                }
                        val tituloLimpio = nombre.removeSuffix(".epub")
                        val yaExiste = repository.existeLibroConTitulo(tituloLimpio)
                        if(yaExiste){
                            Toast.makeText(requireContext(),
                                "${getString(R.string.ya_existe)}\"$tituloLimpio\"",
                                Toast.LENGTH_LONG).show()
                            return@launch
                        }

                        repository.insertLibro(
                            LibroEntity(
                                titulo = tituloLimpio,
                                autor = getString(R.string.autor_desconocido),
                                uriEpub = rutaLocal,
                                tipoLibro = TipoDeLibro.EPUB,
                                descargado = true,
                                estado = EstadoLibro.NUEVO,
                                leido = false,
                                paginaActual = 0,
                                totalPagina = 0,
                            )
                        )

                        Toast.makeText(requireContext(),
                            "${getString(R.string.epub_importado)}: $tituloLimpio",
                            Toast.LENGTH_LONG).show()
                    }

                    else ->{
                        Toast.makeText(requireContext(),
                            getString(R.string.epub_importado),
                            Toast.LENGTH_LONG).show()
                    }


                }
            }catch (e: Exception){
                Toast.makeText(requireContext(),
                    getString(R.string.mensaje_error,e.message),
                    Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun guardarArchivoEnInterno(uri: Uri, nombreDestino: String): String {
        val destino = File(requireContext().filesDir, nombreDestino)
        requireContext().contentResolver.openInputStream(uri)?.use { input ->
            destino.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw Exception(getString(R.string.mensaje_excepcion))
        return destino.absolutePath
    }

    private fun obtenerNombreDesdeUri(uri: Uri): String?{
        var nombre: String? = null
        if(uri.scheme == "content"){
            val cursor = requireContext().contentResolver.query(
                uri, null, null, null,null
            )
            cursor?.use {
                if(it.moveToFirst()){
                    val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) nombre = it.getString(index)
                }
            }
        }
        return nombre ?: uri.path.let {
            File(it).name
        }
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    fun filtrarPorColeccion(tipo: String) {
        coleccionActual = tipo
        if(!isAdded || view == null) return
        cargarLibros()
    }

    private fun abrirLibro(libro: LibroEntity) {
        val activity = activity as? MainActivity
        when (libro.tipoLibro) {
            TipoDeLibro.PDF -> activity?.cargarFragment(PdfFragment.newInstance(libro.titulo))
            TipoDeLibro.AUDIO -> activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id,
                repository))
            TipoDeLibro.EPUB -> {
                Toast.makeText(requireContext(), "Abrir EPUB: ${libro.titulo}",
                    Toast.LENGTH_SHORT).show()
            }
        }
        activity?.binding?.tvTitulo?.text = libro.titulo
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(repository: LibroRepository): BibliotecaFragment {
            val fragment = BibliotecaFragment()
            fragment.repository = repository
            return fragment
        }
    }
}