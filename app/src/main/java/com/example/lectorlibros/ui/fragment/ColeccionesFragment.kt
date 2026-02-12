package com.example.lectorlibros.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.factory.LibroViewModelFactory
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.databinding.FragmentColeccionesBinding
import com.example.lectorlibros.ui.adapter.LibrosAdapter
import com.example.lectorlibros.ui.enums.TipoCeleccion
import com.example.lectorlibros.ui.viewModel.LibroViewModel
import kotlinx.coroutines.launch

/**
 * */
class ColeccionesFragment : Fragment() {

    private var _binding: FragmentColeccionesBinding? = null

    private var tipoCeleccion: TipoCeleccion = TipoCeleccion.TODOS

    //Añadimos un compañion object para pasar el tipo de colección
    companion object {
        private const val ARG_TIPO = "tipo_coleccion"
        fun newInstance(tipo: TipoCeleccion): ColeccionesFragment {
            val fragment = ColeccionesFragment()
            val bundle = Bundle()
            bundle.putString(ARG_TIPO, tipo.name)
            fragment.arguments = bundle
            return fragment
        }


    }

    private val binding get() = _binding!!

    private lateinit var viewModel: LibroViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(ARG_TIPO)?.let {
            tipoCeleccion = TipoCeleccion.valueOf(it)
        }

    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColeccionesBinding.inflate(
            inflater,
            container,
            false
        )
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

    }

    override  fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

       // val libroDao = BaseDatos.getDatabase(requireContext()).libroDao()
        val repository = LibroRepository(BaseDatos.getDatabase(
            requireContext())
            .libroDao())
        val factory = LibroViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[LibroViewModel::class.java]
        mostrarLibros(tipoCeleccion)


        //Asignar clic a cada boton
        binding.ivMenuColecciones.setOnClickListener {
            val popupMenu = PopupMenu(requireContext(), it)
            popupMenu.menu.add("Todos")
            popupMenu.menu.add("Terminados")
            popupMenu.menu.add("Descargados")
            popupMenu.menu.add("PDF")
            popupMenu.menu.add("Audiolibros")

            popupMenu.setOnMenuItemClickListener { menuItem ->
                val tipo = when (menuItem.title.toString()) {
                    "Todos" -> TipoCeleccion.TODOS
                    "Terminados" -> TipoCeleccion.TERMINADOS
                    "Descargados" -> TipoCeleccion.DESCARGADOS
                    "PDF" -> TipoCeleccion.PDF
                    "Audiolibros" -> TipoCeleccion.AUDIO
                    else -> TipoCeleccion.TODOS
                }
                mostrarLibros(tipo)
                true

            }

        }

    }



    private fun mostrarLibros(tipo: TipoCeleccion){
        lifecycleScope.launch {
            viewModel.obtenerLibrosPorColeccion(tipo).collect { libros ->
                if (libros.isEmpty()) {
                    binding.rvColecciones.visibility = View.GONE
                    binding.tvColeccionesMensaje.visibility = View.VISIBLE

                }else{
                    binding.rvColecciones.visibility = View.VISIBLE
                    binding.tvColeccionesMensaje.visibility = View.GONE
                    binding.rvColecciones.adapter = LibrosAdapter(libros)
                }
            }
        }
    }



}
