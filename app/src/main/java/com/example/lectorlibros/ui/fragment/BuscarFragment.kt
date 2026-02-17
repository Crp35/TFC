package com.example.lectorlibros.ui.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.factory.LibroViewModelFactory
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.ui.enums.TipoCeleccion
import com.example.lectorlibros.data.factory.LibroViewModel
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorlibros.ui.adapter.LibrosAdapter


/**
 * A simple [Fragment] subclass.
 * Use the [BuscarFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BuscarFragment : Fragment() {

    //OBtenemos el ViewModel de la Activity
    private val viewModel: LibroViewModel by activityViewModels {
        LibroViewModelFactory(
            LibroRepository(
                requireContext(),
                BaseDatos.getDatabase(
                    requireContext()).libroDao(),
                ServicioDescargaPdf(requireContext())
        )
        )
    }

    private lateinit var buscaLocal: String
    private lateinit var buscaInternet: String
    private lateinit var preguntaBuscar: String

    private lateinit var  recyclerView: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: LibrosAdapter







    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        buscaLocal = getString(R.string.busquedaLocal)
        buscaInternet = getString(R.string.busquedaInternet)
        preguntaBuscar = getString(R.string.preguntaBuscar)
        mostrarDialogoBusqueda()
    }

   override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
       super.onViewCreated(view, savedInstanceState)

       recyclerView = view.findViewById(R.id.rvResultados)
       tvEmpty = view.findViewById(R.id.tvEmpty)

       recyclerView.layoutManager = LinearLayoutManager(requireContext())
       adapter = LibrosAdapter(emptyList())
       recyclerView.adapter = adapter


   }




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View{
        // Inflate the layout for this fragment
        return inflater.inflate(
            R.layout.fragment_buscar,
            container, false)
    }

    private fun mostrarDialogoBusqueda(){
        val opciones = arrayOf(
            buscaLocal,
            buscaInternet
        )
        AlertDialog.Builder(requireContext())
            .setTitle(preguntaBuscar)
            .setItems(opciones){ _, which ->
                when(which){
                    0 -> buscaEnLocal()
                    1 -> buscaEnInternet()
                }

            }
            .show()

    }

    private fun buscaEnLocal(){
        //Configuramos RecyclerView
        lifecycleScope.launchWhenStarted {
            viewModel.obtenerLibrosPorColeccion(TipoCeleccion.TODOS)
                .collect { libros ->
                    if(libros.isEmpty()){
                        tvEmpty.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }else{
                        tvEmpty.visibility = View.GONE
                        recyclerView.visibility = View.VISIBLE
                        adapter.actualizarLibros(libros)
                    }

                    /*libros.forEach {
                    Log.d("Busqueda_local", "${it.titulo} - ${it.autor}")*/
                }
            }
        }
        /*lifecycleScope.launch {
            val libros = viewModel.obtenerLibrosPorColeccion(TipoCeleccion.TODOS).first()
            libros.forEach { libro ->
                Log.d("Busqueda_local", "${libro.titulo} - ${libro.autor}")
                //viewModel.cambiarTituloAutor(libro)

                libros.forEach { libro ->
                    Log.d("Busqueda_local", "${libro.titulo} - ${libro.autor}")
                }
            }
        }*/


    private fun buscaEnInternet(){

    }

}





