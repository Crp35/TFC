package com.example.lectorlibros.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.factory.LibroViewModelFactory
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.databinding.ActivityMainBinding
import com.example.lectorlibros.ui.adapter.ColeccionAdapter
import com.example.lectorlibros.ui.enums.TipoCeleccion
import com.example.lectorlibros.ui.fragments.AudioLibrosFragment
import com.example.lectorlibros.ui.fragment.BibliotecaFragment
import com.example.lectorlibros.ui.fragment.BuscarFragment
import com.example.lectorlibros.ui.fragment.ColeccionesFragment
import com.example.lectorlibros.ui.fragment.LeidosFragment
import com.example.lectorlibros.ui.fragment.PdfFragment
import com.example.lectorlibros.data.factory.LibroViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding
    private lateinit var repository: LibroRepository



    private val viewModel: LibroViewModel by viewModels {
        LibroViewModelFactory(repository)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Iniciamos DAO y Repository
        val libroDao = BaseDatos.getDatabase(this).libroDao()
        val servicioDescargaPdf = ServicioDescargaPdf(this)
        //val database = BaseDatos.getDatabase(this)
        repository = LibroRepository(this,libroDao, servicioDescargaPdf)

        setContentView(binding.root)
        //Cargamos el fragment inicial
        cargarFragment(BibliotecaFragment())








        //Llamamos a insertar datos de pueba
        lifecycleScope.launch {
            repository.pruebaInsertarLibros()
        }

        //Añadir un listener
        binding.layoutColecciones.setOnClickListener {
            mostrarOpcionesColecciones()
        }

        //Llamos a la función para cargar el fragment
        /*cargarFragment(BibliotecaFragment()) DESCOMENTAR SI ERROR*/
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem)

        }
        binding.bottomNavigation.selectedItemId = R.id.menu_biblioteca

        binding.ivMenu.setOnClickListener {
            mostrarOpcionesColecciones()
        }

        //Listener del botón de menú
        binding.ivMenu.setOnClickListener {
            mostrarOpcionesColecciones()
        }


    }

    //Función para cargar el fragment inicial
    fun cargarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    //Función para el listener de la barra de navegación inferior
    fun onNavigationItemSelected(menuItem: MenuItem): Boolean {

        //Elegir qué fragment cargar
        val fragment = when (menuItem.itemId) {
            R.id.menu_biblioteca -> BibliotecaFragment.newInstance(repository)
            R.id.menu_leidos -> LeidosFragment()
            R.id.menu_audio -> AudioLibrosFragment(repository)
            R.id.menu_buscar -> BuscarFragment()
            else -> null
        }

        //Si fragment no es nulo, cargarlo( hacemos el reemplazo)
        fragment?.let {
            cargarFragment(it)
            return true //Manejamos el click
        }

        return false
    }

    //Función que carge coleccionesFragment
    private fun cargarColeccionesFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, ColeccionesFragment())
            .addToBackStack(null)//Para poder volver atrás con el botón de retroceso
            .commit()
    }


    /**
     * Función para mostrar opciones de colecciones
     * */
    private fun mostrarOpcionesColecciones() {
        val libros = getString(R.string.opcion_libros)
        val terminados = getString(R.string.opcion_terminados)
        val audiolibros = getString(R.string.audiolibros)
        val pdf = getString(R.string.pdf)
        val descargados = getString(R.string.descargados)

        val tiposTitulos = arrayOf(
            libros,
            terminados,
            pdf,
            audiolibros,
            descargados
        )

        val tiposIconos = arrayOf(
            R.drawable.libros_colecciones,
            R.drawable.libro_leido_icono,
            R.drawable.archivo_pdf,
            R.drawable.cascos,
            R.drawable.circulo_de_descarga_en_la_nube
        )

        val adapter = ColeccionAdapter(
            this,
            tiposTitulos,
            tiposIconos
        )

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.colecciones))
            .setAdapter(adapter) { _, which ->
                //Creamos el índice del elemento con whitch
                val tipo = when (which) {
                    0 -> TipoCeleccion.TODOS
                    1 -> TipoCeleccion.TERMINADOS
                    2 -> TipoCeleccion.PDF
                    3 -> TipoCeleccion.AUDIO
                    4 -> TipoCeleccion.DESCARGADOS
                    else -> TipoCeleccion.TODOS
                }

                mostrarColeccionFragment(tipo)

            }
            .show()
    }

    private fun mostrarColeccionFragment(tipo: TipoCeleccion){
        when(tipo){
            TipoCeleccion.TODOS -> binding.tvTitulo.text = getString(R.string.todos)
            TipoCeleccion.TERMINADOS -> binding.tvTitulo.text = getString(R.string.terminados)
            TipoCeleccion.PDF -> binding.tvTitulo.text = getString(R.string.pdf)
            TipoCeleccion.AUDIO -> binding.tvTitulo.text = getString(R.string.audiolibros)
            TipoCeleccion.DESCARGADOS -> binding.tvTitulo.text = getString(R.string.descargados)
            else -> binding.tvTitulo.text = getString(R.string.todos)
        }
        android.util.Log.d("PDF_DEBUG", "TIPO SELECCIONADO: $tipo")
        if (tipo == TipoCeleccion.PDF){
            android.util.Log.d("PDF_DEBUG", "CARGANDO PDF")
            cargarFragment(PdfFragment())
        }else{
            val fragment = ColeccionesFragment.newInstance(tipo)
            cargarFragment(fragment)
        }

    }


}