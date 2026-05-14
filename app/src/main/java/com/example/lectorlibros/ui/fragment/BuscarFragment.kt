package com.example.lectorlibros.ui.fragment

import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupMenu
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.factory.LibroViewModelFactory
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.data.factory.LibroViewModel
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.adapter.LibrosAdapter
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.Q)
class BuscarFragment : Fragment() {

    private val viewModel: LibroViewModel by activityViewModels {
        LibroViewModelFactory(
            LibroRepository(
                requireContext(),
                BaseDatos.getDatabase(requireContext()).libroDao(),
                ServicioDescargaPdf(requireContext())
            )
        )
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var searchView: SearchView
    private lateinit var adapter: LibrosAdapter

    private val listaLibros = mutableListOf<LibroEntity>()

    // Control de búsquedas activas
    private var collectJob: Job? = null

    private val repository by lazy {
        LibroRepository(
            requireContext(),
            BaseDatos.getDatabase(requireContext()).libroDao(),
            ServicioDescargaPdf(requireContext())
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_buscar, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar vistas
        recyclerView = view.findViewById(R.id.rvResultados)
        tvEmpty = view.findViewById(R.id.tvEmpty)
        searchView = view.findViewById(R.id.searchView)
        searchView.queryHint = getString(R.string.bucar_libro)

        // Configuración de 2 columnas
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // Adaptar la inicialización a los 4 parámetros del LibrosAdapter
        adapter = LibrosAdapter(
            listaLibros,
            this,
            { libro -> abrirLibro(libro) }, // onItemClick
            { libro, viewAncla -> showPopupMenu(libro, viewAncla) } // onOpcionesClick
        )
        recyclerView.adapter = adapter

        // Configurar búsqueda en tiempo real
        configurarBusqueda()

        // Capturar el botón de la X en el SearchView para que actúe como "Cancelar"
        val closeButton = searchView.findViewById<View>(
            androidx.appcompat.R.id.search_close_btn
        )

        closeButton.setOnClickListener {
            // Limpiar texto y quitar foco
            searchView.setQuery("", false)
            searchView.clearFocus()

            // Volver a la BibliotecaFragment
            val activity = activity as? MainActivity
            activity?.cargarFragment(BibliotecaFragment.newInstance(repository))
        }
    }

    // Nueva función para el menú CRUD dentro de BuscarFragment
    fun showPopupMenu(libro: LibroEntity, ancla: View) {
        val popup = PopupMenu(requireContext(), ancla)
        popup.inflate(R.menu.menu_item_libro)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename -> {
                    mostrarDialogoRenombrar(libro)
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

    // Inicio método para mostrar diálogo renombrar libros
    private fun mostrarDialogoRenombrar(libro: LibroEntity) {
        val salvarEdicion = getString(R.string.gurdar_libro)
        val textString = getString(R.string.renombrar_libro)
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(textString)

        // Creamos el campo de texto
        val input = EditText(requireContext())
        input.setText(libro.titulo) // Pre-rellenamos con el título actual
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
                    repository.renombrarLibros(libro.id, nuevoTitulo, libro.autor)
                    Toast.makeText(requireContext(), "Título cambiado", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton(getString(R.string.cancelar), null)
        builder.setView(container)
        builder.show()
    }

    // Nueva función para confirmar eliminación
    private fun confirmarEliminacion(libro: LibroEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar libro")
            .setMessage("¿Estás seguro de eliminar '${libro.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteLibro(libro)
                    Toast.makeText(requireContext(), "Libro eliminado", Toast.LENGTH_SHORT).show()
                    // Refrescar resultados
                    buscaEnLocal(searchView.query.toString())
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun configurarBusqueda() {
        // Mantiene el SearchView expandido y pide el foco para mostrar el teclado de inmediato
        searchView.isIconified = false
        searchView.requestFocus()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                // Al pulsar "Enter", ejecutamos la búsqueda
                query?.let { buscaEnLocal(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText?.trim() ?: ""

                if (texto.isEmpty()) {
                    // Cancelar búsquedas activas
                    collectJob?.cancel()

                    // Limpiar lista y mostrar mensaje de vacío
                    listaLibros.clear()
                    adapter.actualizarLibros(emptyList())
                    tvEmpty.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                    return true
                }

                // Ejecutar búsqueda en local en tiempo real (teclado permanece abierto)
                buscaEnLocal(texto)
                return true
            }
        })
    }

    private fun buscaEnLocal(titulo: String) {
        collectJob?.cancel()

        // Lanzar el trabajo de búsqueda en una corrutina para recoger resultados en tiempo real
        collectJob = lifecycleScope.launch {
            viewModel.buscarLibrosPorTitulo("%$titulo%") // Búsqueda parcial
                .collectLatest { libros ->
                    if (libros.isEmpty()) {
                        tvEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    } else {
                        tvEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE

                        listaLibros.clear()
                        listaLibros.addAll(libros)
                        adapter.actualizarLibros(libros)
                    }
                }
        }
    }

    private fun abrirLibro(libro: LibroEntity) {
        val activity = activity as? MainActivity

        when (libro.tipoLibro) {
            TipoDeLibro.PDF -> activity?.cargarFragment(PdfFragment.newInstance(libro.titulo))
            TipoDeLibro.AUDIO -> activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id, repository))
            TipoDeLibro.EPUB -> {
                // Futuro: EpubFragment
            }
        }

        activity?.binding?.tvTitulo?.text = libro.titulo
    }

    private fun buscaEnInternet() {
        // Futuro: Implementar búsqueda en internet
    }
}
