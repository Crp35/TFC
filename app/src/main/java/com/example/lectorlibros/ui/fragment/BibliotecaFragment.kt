package com.example.lectorlibros.ui.fragment

import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.example.lectorlibros.R
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.databinding.FragmentBibliotecaBinding
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.adapter.LibrosAdapter
import com.example.lectorlibros.ui.enums.TipoDeLibro
import com.google.android.material.appbar.CollapsingToolbarLayout
import kotlinx.coroutines.launch

class BibliotecaFragment : Fragment() {

    private var _binding: FragmentBibliotecaBinding? = null
    private val binding get() = _binding!!

    lateinit var repository: LibroRepository
    private val listaLibros = mutableListOf<LibroEntity>()
    private lateinit var adapter: LibrosAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBibliotecaBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Función corregida para mostrar el menú CRUD
    fun showPopupMenu(libro: LibroEntity, ancla: View) {
        val popup = PopupMenu(requireContext(), ancla)
        // CAMBIO: Se eliminó el segundo argumento 'popup.menu' para evitar el error de compilación
        popup.inflate(R.menu.menu_item_libro)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.action_rename -> {
                    //Toast.makeText(requireContext(), "Renombrar: ${libro.titulo}", Toast.LENGTH_SHORT).show()
                    mostrarDialogoRenombrar(libro)
                    true
                }

                R.id.action_delete -> {
                    // Llamada a la función de eliminación real
                    confirmarEliminacion(libro)
                    true
                }
                else -> false
            }
        }
        popup.show()
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
        builder.setNegativeButton(abortar, null)
        builder.setView(container) // Si error en la compilación prueba a borrar
        builder.show()
    }

    // Fin método para mostrar diálogo renombrar libros

    // MEJORA: Lógica para eliminar el libro de la base de datos
    private fun confirmarEliminacion(libro: LibroEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Eliminar libro")
            // El usuario ve el título, así sabe qué está borrando
            .setMessage("¿Estás seguro de que quieres eliminar '${libro.titulo}'?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    // Usamos el ID internamente para que la DB sepa exactamente cuál es
                    repository.deleteLibro(libro)

                    Toast.makeText(requireContext(), "${libro.titulo} eliminado", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = LibrosAdapter(
            listaLibros,
            this,
            { libro ->
                abrirLibro(libro)
            },
            { libro, viewAncla ->
                // CAMBIO: Vinculación correcta con la función local
                showPopupMenu(libro, viewAncla)
            }
        )

        binding.rvBiblioteca.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvBiblioteca.adapter = adapter

        cargarLibros()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun cargarLibros() {
        viewLifecycleOwner.lifecycleScope.launch {
            repository.getAllLibros().collect { listaCompleta ->
                Log.d("BIBLIOTECA", "Datos recibidos: ${listaCompleta.size} libros")

                val filtrados = listaCompleta.filter { it.tipoLibro == TipoDeLibro.PDF || it.tipoLibro == TipoDeLibro.AUDIO }

                listaLibros.clear()
                listaLibros.addAll(filtrados)

                binding.tvEmpty.visibility = if (listaLibros.isEmpty()) View.VISIBLE else View.GONE
                binding.rvBiblioteca.visibility = if (listaLibros.isEmpty()) View.GONE else View.VISIBLE

                adapter.actualizarLibros(filtrados)
            }
        }
    }

    private fun abrirLibro(libro: LibroEntity) {
        val activity = activity as? MainActivity
        when (libro.tipoLibro) {
            TipoDeLibro.PDF -> activity?.cargarFragment(PdfFragment.newInstance(libro.titulo))
            TipoDeLibro.AUDIO -> activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id, repository))
            TipoDeLibro.EPUB -> {
                Toast.makeText(requireContext(), "Abrir EPUB: ${libro.titulo}", Toast.LENGTH_SHORT).show()
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