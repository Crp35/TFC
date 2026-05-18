package com.example.lectorlibros.ui.fragment

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.data.factory.LibroViewModelFactory
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.databinding.FragmentColeccionesBinding
import com.example.lectorlibros.ui.adapter.AudioAdapter
import com.example.lectorlibros.ui.adapter.LibrosAdapter
import com.example.lectorlibros.ui.enums.TipoColeccion
import com.example.lectorlibros.data.factory.LibroViewModel
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.enums.TipoDeLibro
import com.example.lectorlibros.util.ToastHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ColeccionesFragment : Fragment() {

    private var _binding: FragmentColeccionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LibroViewModel
    private lateinit var repository: LibroRepository
    private var fetchJob: Job? = null
    private var pendingLibro: LibroEntity? = null
    private val listaLibros = mutableListOf<LibroEntity>()

    private val audioAdapter by lazy {
        //AudioAdapter { libro -> abrirLibro(libro) }
        AudioAdapter(
            onItemClick = { libro -> handleAudioItemClick(libro) },
            onItemLongClick = {  }
        )
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) pendingLibro?.let { navigateToPlayer(it) }
        pendingLibro = null
    }

    companion object {
        private const val ARG_TIPO = "tipo_coleccion"

        @JvmStatic
        fun newInstance(tipo: TipoColeccion): ColeccionesFragment {
            val fragment = ColeccionesFragment()
            val bundle = Bundle()
            bundle.putString(ARG_TIPO, tipo.name)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColeccionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val servicioDescargaPdf = ServicioDescargaPdf(requireContext())
        repository = LibroRepository(
            requireContext(),
            BaseDatos.getDatabase(requireContext()).libroDao(),
            servicioDescargaPdf
        )
        val factory = LibroViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LibroViewModel::class.java]

        val tipoInicial = arguments?.getString(ARG_TIPO)?.let {
            TipoColeccion.valueOf(it)
        } ?: TipoColeccion.TODOS

        mostrarLibros(tipoInicial)

        binding.ivMenuColecciones.setOnClickListener { menuIcon ->
            val popupMenu = PopupMenu(requireContext(), menuIcon)
            popupMenu.menu.apply {
                add("Todos"); add("Terminados"); add("Descargados"); add("PDF"); add("Audiolibros")
            }

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val nuevoTipo = when (menuItem.title.toString()) {
                    "Todos" -> TipoColeccion.TODOS
                    "Terminados" -> TipoColeccion.TERMINADOS
                    "Descargados" -> TipoColeccion.DESCARGADOS
                    "PDF" -> TipoColeccion.PDF
                    "Audiolibros" -> TipoColeccion.AUDIO
                    else -> TipoColeccion.TODOS
                }
                mostrarLibros(nuevoTipo)
                true
            }
            popupMenu.show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mostrarLibros(tipo: TipoColeccion) {
        fetchJob?.cancel()
        fetchJob = viewLifecycleOwner.lifecycleScope.launch {
            viewModel.obtenerLibrosPorColeccion(tipo).collect { libros ->
                if (_binding == null) return@collect

                if (libros.isEmpty()) {
                    binding.rvColecciones.visibility = View.GONE
                    binding.tvColeccionesMensaje.visibility = View.VISIBLE
                    binding.tvColeccionesMensaje.text = "No se encontraron libros en ${tipo.name}"
                } else {
                    binding.rvColecciones.visibility = View.VISIBLE
                    binding.tvColeccionesMensaje.visibility = View.GONE

                    if (tipo == TipoColeccion.AUDIO) {
                        if (binding.rvColecciones.adapter !is AudioAdapter) {
                            binding.rvColecciones.adapter = AudioAdapter(
                                onItemClick = { libro -> abrirLibro(libro) },
                                onItemLongClick = {  }
                            )
                        }
                        (binding.rvColecciones.adapter as AudioAdapter).submitList(libros)
                    } else {
                        listaLibros.clear()
                        listaLibros.addAll(libros)

                        binding.rvColecciones.adapter = LibrosAdapter(
                            listaLibros,
                            this@ColeccionesFragment,
                            { libro -> abrirLibro(libro) },      // onItemClick
                            { libro, view -> showPopupMenu(libro, view) } // onOpcionesClick
                        )
                    }
                }
            }
        }
    }

    // Función necesaria para el menú CRUD
    @RequiresApi(Build.VERSION_CODES.Q)
    fun showPopupMenu(libro: LibroEntity, ancla: View) {
        val popup = PopupMenu(requireContext(), ancla)
        popup.inflate(R.menu.menu_item_libro)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename -> {
                    Toast.makeText(requireContext(), "Renombrar: ${libro.titulo}", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.action_delete -> {
                    confirmarEliminacion(libro)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Función para confirmar y ejecutar la eliminación
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun confirmarEliminacion(libro: LibroEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar libro")
            .setMessage("¿Estás seguro de eliminar '${libro.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteLibro(libro)
                    Toast.makeText(requireContext(), "Libro eliminado", Toast.LENGTH_SHORT).show()
                    // La lista se actualizará sola por el collect de obtenerLibrosPorColeccion
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirLibro(libro: LibroEntity) {
        val activity = activity as? MainActivity
        when (libro.tipoLibro) {
            TipoDeLibro.PDF -> activity?.cargarFragment(PdfFragment.newInstance(libro.titulo))
            TipoDeLibro.AUDIO -> {
                activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id, repository))
            }
            TipoDeLibro.EPUB -> {}
        }
        activity?.binding?.tvTitulo?.text = libro.titulo
    }

    private fun handleAudioItemClick(libro: LibroEntity) {
        abrirLibro(libro)
    }

    private fun navigateToPlayer(libro: LibroEntity) {
        val activity = activity as? MainActivity
        activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id, repository))
        activity?.binding?.tvTitulo?.text = libro.titulo
    }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), getRequiredPermission()) == PackageManager.PERMISSION_GRANTED
    }

    private fun isInternalFile(path: String?): Boolean {
        return path?.startsWith(requireContext().filesDir.absolutePath) ?: false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fetchJob?.cancel()
        _binding = null
    }
}