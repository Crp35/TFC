package com.example.lectorlibros.ui.activity

import ColeccionAdapter
import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.factory.LibroViewModel
import com.example.lectorlibros.data.factory.LibroViewModelFactory
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.databinding.ActivityMainBinding
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.enums.TipoColeccion
import com.example.lectorlibros.ui.fragment.*

import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    private lateinit var repository: LibroRepository

    private val viewModel: LibroViewModel by viewModels {
        LibroViewModelFactory(repository)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicializamos DAO y Repository
        val libroDao = BaseDatos.getDatabase(this).libroDao()
        val servicioDescargaPdf = ServicioDescargaPdf(this)
        repository = LibroRepository(this, libroDao, servicioDescargaPdf)

        // Cargamos fragmento inicial
        cargarFragment(BibliotecaFragment.newInstance(repository))
        binding.tvTitulo.text = getString(R.string.biblioteca)

        // Insertar libros de prueba
        /*lifecycleScope.launch {
            repository.pruebaInsertarLibros()
        }*/

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
    }

    // Carga cualquier fragment
    fun cargarFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }




    // BottomNavigation
    @RequiresApi(Build.VERSION_CODES.Q)
    fun onNavigationItemSelected(menuItem: MenuItem): Boolean {
        val fragment: Fragment? = when (menuItem.itemId) {
            R.id.menu_biblioteca -> BibliotecaFragment.newInstance(repository)
            R.id.menu_leidos -> LeidosFragment()
            R.id.menu_audio -> AudioLibrosFragment(repository)
            R.id.menu_buscar -> BuscarFragment()
            else -> null
        }

        fragment?.let {
            cargarFragment(it)
            binding.tvTitulo.text = when (menuItem.itemId) {
                R.id.menu_biblioteca -> getString(R.string.biblioteca)
                R.id.menu_leidos -> getString(R.string.terminados)
                R.id.menu_audio -> getString(R.string.audiolibros)
                R.id.menu_buscar -> getString(R.string.buscar)
                else -> getString(R.string.app_name)
            }
            binding.layoutColecciones.visibility = View.VISIBLE
            binding.ivMenu.visibility = View.VISIBLE
            return true
        }
        return false
    }

    // Mostrar menú de colecciones
    private fun mostrarOpcionesColecciones() {
        val tiposTitulos = arrayOf(
            getString(R.string.opcion_libros),
            getString(R.string.opcion_terminados),
            getString(R.string.pdf),
            getString(R.string.audiolibros),
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
            val tipo = when (position) {
                0 -> TipoColeccion.TODOS
                1 -> TipoColeccion.TERMINADOS
                2 -> TipoColeccion.PDF
                3 -> TipoColeccion.AUDIO
                4 -> TipoColeccion.DESCARGADOS
                else -> TipoColeccion.TODOS
            }

            when (tipo) {
                TipoColeccion.PDF -> {
                    val libroSeleccionado = "UTF4.pdf" // Libro de prueba, reemplaza con libro real
                    cargarFragment(PdfFragment.newInstance(libroSeleccionado))
                    binding.tvTitulo.text = libroSeleccionado
                    binding.layoutColecciones.visibility = View.GONE
                    binding.ivMenu.visibility = View.GONE
                }
                TipoColeccion.AUDIO -> {
                    cargarFragment(AudioLibrosFragment(repository))
                    binding.tvTitulo.text = titulo
                    binding.layoutColecciones.visibility = View.GONE
                    binding.ivMenu.visibility = View.GONE
                }
                else -> {
                    cargarFragment(ColeccionesFragment.newInstance(tipo))
                    binding.tvTitulo.text = titulo
                    binding.layoutColecciones.visibility = View.VISIBLE
                    binding.ivMenu.visibility = View.VISIBLE
                }
            }
        }
    }
}