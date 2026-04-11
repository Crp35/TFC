package com.example.lectorlibros.ui.fragment

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.adapter.LibrosAdapter
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LeidosFragment : Fragment() {

    private lateinit var recycler: RecyclerView
    private lateinit var adapter: LibrosAdapter
    private lateinit var db: BaseDatos

    private lateinit var repository: LibroRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_leidos, container, false)
        recycler = view.findViewById(R.id.recyclerLeidos)
        return view
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = BaseDatos.getDatabase(requireContext())

        repository = LibroRepository(
            contexto = requireContext(),
            libroDao = db.libroDao(),
            servicioDescargaPdf = ServicioDescargaPdf(requireContext())
        )

        // SOLUCIÓN: Inicialización con los 4 parámetros requeridos
        adapter = LibrosAdapter(
            emptyList<LibroEntity>(),
            this,
            { libro ->
                abrirLibro(libro) // Click normal
            },
            { libro, viewAncla ->
                showPopupMenu(libro, viewAncla) // Click en opciones (Menu CRUD)
            }
        )

        // Configuración de la cuadrícula
        recycler.layoutManager = GridLayoutManager(requireContext(), 2)
        recycler.adapter = adapter

        cargarLibros()
    }

    // Función para abrir el libro (Extraída para mayor claridad)
    private fun abrirLibro(libro: LibroEntity) {
        val activity = activity as? MainActivity
        when (libro.tipoLibro) {
            TipoDeLibro.PDF -> activity?.cargarFragment(PdfFragment.newInstance(libro.titulo))
            TipoDeLibro.AUDIO -> activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id, repository))
            TipoDeLibro.EPUB -> { /* Futuro */ }
        }
        activity?.binding?.tvTitulo?.text = libro.titulo
    }

    // Función necesaria para mostrar el menú CRUD
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

    // Función necesaria para confirmar eliminación
    private fun confirmarEliminacion(libro: LibroEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar libro")
            .setMessage("¿Estás seguro de eliminar '${libro.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.deleteLibro(libro)
                    Toast.makeText(requireContext(), "Libro eliminado", Toast.LENGTH_SHORT).show()
                    cargarLibros() // Recargar la lista tras borrar
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Inicion método para mostrar diálogo renombrar libros

    private fun mostrarDialogoRenombrar(libro: LibroEntity) {
        val abortar = getString(R.string.cancelar)
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
        builder.setView(container) // Si error en la compilación prueba a borrar
        builder.show()
    }
    // Fin método para mostrar diálogo renombrar libros

    private fun cargarLibros() {
        viewLifecycleOwner.lifecycleScope.launch {
            val libros = withContext(Dispatchers.IO) {
                db.libroDao().getLibrosPorEstado(EstadoLibro.COMPLETADO)
            }
            adapter.actualizarLibros(libros)
        }
    }

    override fun onResume() {
        super.onResume()
        cargarLibros()
    }
}