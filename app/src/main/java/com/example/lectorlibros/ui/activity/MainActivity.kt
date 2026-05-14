package com.example.lectorlibros.ui.activity

import ColeccionAdapter
import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import android.widget.Toast
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.factory.LibroViewModel
import com.example.lectorlibros.data.factory.LibroViewModelFactory
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.databinding.ActivityMainBinding
import com.example.lectorlibros.ui.fragment.*

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var repository: LibroRepository

    private val viewModel: LibroViewModel by viewModels {
        LibroViewModelFactory(repository)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Iniciamos el binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Inicio lógica opciones generales

        // Inicializamos el binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Usamos el binding con seguridad
        binding.ivOpcionesBiblioteca.setOnClickListener { ancla ->
            val popup = PopupMenu(this, ancla)
            popup.menu.apply {
                add(0,R.id.action_import,0,getString(R.string.importar_libro))
                    .setIcon(R.drawable.ic_a_adir)
                add(0,R.id.action_download,0,getString(R.string.descargar_libro))
                    .setIcon(R.drawable.ic_descargar)
            }
            popup.setOnMenuItemClickListener { menuItem ->
                when(menuItem.itemId){
                    R.id.action_import ->{
                        // Delegamos la importación al fragmento activo ( BibliotecaFragment )
                        val fragmentoActual = supportFragmentManager
                            .findFragmentById(R.id.fragmentContainer)
                        if (fragmentoActual is BibliotecaFragment){
                            fragmentoActual.lanzarImportacion()
                        }else{
                            // Si no estamos en Biblioteca, la cargamos y luego importamos
                            val fragmentoBiblioteca = BibliotecaFragment.newInstance(repository)
                            binding.root.post {
                                fragmentoBiblioteca.lanzarImportacion()
                            }
                        }
                        true
                    }
                    R.id.action_download ->{
                        // Aquí irá la lógica de descargar cuando sea implementada
                        Toast.makeText(this, getString(R.string.futura_implementacion),
                            Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }

        // Fin opciones generales

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Inicializamos DAO y Repository
        val libroDao = BaseDatos.getDatabase(this).libroDao()
        val servicioDescargaPdf = ServicioDescargaPdf(this)
        repository = LibroRepository(this, libroDao, servicioDescargaPdf)

        // Cargamos fragmento inicial
        cargarFragment(BibliotecaFragment.newInstance(repository))
        binding.tvTitulo.text = getString(R.string.biblioteca)

        // Configurar listeners
        binding.layoutColecciones.setOnClickListener {
            mostrarOpcionesColecciones()
        }

        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            onNavigationItemSelected(menuItem)
        }
        binding.bottomNavigation.selectedItemId = R.id.menu_biblioteca

        binding.ivMenu.setOnClickListener {
            mostrarOpcionesColecciones()
        }
        // LÓGICA BOTÓN VOLVER ATRÁS

        binding.ivVolverAtras.setOnClickListener {
            volverBiblioteca()
        }
        // Fin lógica botón volver atrás
    }
    //Inicio lógica botón volver atrás
    fun volverBiblioteca(){
        cargarFragment(BibliotecaFragment.newInstance(repository))
        binding.tvTitulo.text = getString(R.string.biblioteca)
        binding.bottomNavigation.selectedItemId = R.id.menu_biblioteca

        setMenuVisibility(true)
        // Nos aseguramos de que esta visible siempre
        binding.ivVolverAtras.visibility = View.VISIBLE
        binding.ivVolverAtras.alpha = 1f
    }
    // Fin lógica botón volver atrás

    // Carga cualquier fragment
    fun cargarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()

        // Nos aseguramos de que tras cada cambio de fragmento, el botón permanece
        binding.ivVolverAtras.visibility = View.VISIBLE
    }

    // FUNCIÓN PARA OCULTAR/MOSTRAR LAS BARRAS DE NAVEGACIÓN Y COLECCIONES
    fun setMenuVisibility(visible: Boolean) {
        val visibility = if(visible) View.VISIBLE else View.GONE
        binding.bottomNavigation.visibility = visibility
        binding.layoutColecciones.visibility = visibility
        binding.ivMenu.visibility = visibility

        // También ocultamos el ´titulo del fragmento
        binding.tvTitulo.visibility = visibility
    }
    // BottomNavigation
    @RequiresApi(Build.VERSION_CODES.Q)
    fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        val fragment: Fragment? = when (menuItem.itemId) {
            R.id.menu_biblioteca -> BibliotecaFragment.newInstance(repository)
            R.id.menu_audio -> {
                val bibliotecaFragment = BibliotecaFragment.newInstance(repository)
                bibliotecaFragment.filtrarPorColeccion(getString(R.string.opcion_audiolibros))
                bibliotecaFragment
            }
            R.id.menu_leidos -> LeidosFragment()
            R.id.menu_buscar -> BuscarFragment()
            else -> null
        }

        fragment?.let {
            cargarFragment(it)
            binding.tvTitulo.text = when (menuItem.itemId) {
                R.id.menu_biblioteca -> getString(R.string.biblioteca)
                R.id.menu_audio -> getString(R.string.opcion_audiolibros)
                R.id.menu_buscar -> getString(R.string.buscar)
                else -> getString(R.string.app_name)
            }
            return true
        }
        return false
    }

    // Inicio lógica de mostrar solo el indicador de páginas y botón volver y página(modo horizontal)
    fun alternarControlesLectura(){
        val visibilidadActual = binding.ivVolverAtras.visibility
        if(visibilidadActual == View.VISIBLE){
            // Si se ven, los ocultamos con una pequeña animación para evitar la brusquedad
            binding.ivVolverAtras.animate().alpha(0f).setDuration(200).withEndAction {
                binding.ivVolverAtras.visibility = View.GONE
            }
            binding.tvTitulo.animate().alpha(0f).setDuration(200).withEndAction {
                binding.tvTitulo.visibility = View.GONE
            }
        }else{
            // Sí están ocultos, los mostramos
            binding.ivVolverAtras.visibility = View.VISIBLE
            binding.tvTitulo.visibility = View.VISIBLE
            binding.ivVolverAtras.animate().alpha(1f).setDuration(200)
            binding.tvTitulo.animate().alpha(1f).setDuration(200)

        }
    }
    // Fin lógica de mostrar solo volver y página(modo horizontal)

    // Mostrar menú de colecciones
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mostrarOpcionesColecciones() {
        val tiposTitulos = arrayOf(
            getString(R.string.opcion_libros),
            getString(R.string.opcion_terminados),
            getString(R.string.pdf),
            getString(R.string.opcion_audiolibros),
            getString(R.string.descargados)
        )

        val tiposIconos = arrayOf(
            R.drawable.libros_colecciones,
            R.drawable.libro_leido_icono,
            R.drawable.archivo_pdf,
            R.drawable.cascos,
            R.drawable.circulo_de_descarga_en_la_nube
        )

        val adapter = ColeccionAdapter(this, tiposTitulos, tiposIconos)
        val builder = AlertDialog.Builder(this)
            .setTitle(getString(R.string.colecciones))
            .setAdapter(adapter, null)

        val alertDialog = builder.show()

        adapter.onItemClick = { position, titulo ->
            alertDialog.dismiss()

            // Buscamos si el fragmento actual es la Biblioteca
            val fragmentoActual = supportFragmentManager.findFragmentById(R.id.fragmentContainer)

            if (fragmentoActual is BibliotecaFragment) {
                // Si estamos en Biblioteca, simplemente le decimos que filtre
                fragmentoActual.filtrarPorColeccion(titulo)
                binding.tvTitulo.text = titulo
            } else {
                // Si no estamos en Biblioteca (ej. estamos en Buscar),
                // cargamos la Biblioteca y le pasamos el filtro (opcional)
                val fragmentoBiblioteca = BibliotecaFragment.newInstance(repository)
                cargarFragment(fragmentoBiblioteca)
                // Damos un pequeño tiempo para que se cree y luego filtramos
                binding.root.post {
                    fragmentoBiblioteca.filtrarPorColeccion(titulo)
                    binding.tvTitulo.text = titulo
                }
            }
        }
    }
}