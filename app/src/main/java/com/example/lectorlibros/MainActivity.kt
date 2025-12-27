package com.example.lectorlibros

import android.os.Bundle
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.example.lectorlibros.databinding.ActivityMainBinding
import com.example.lectorlibros.fragments.AudioLibrosFragment
import com.example.lectorlibros.fragments.BibliotecaFragment
import com.example.lectorlibros.fragments.BuscarFragment
import com.example.lectorlibros.fragments.LeidosFragment

class MainActivity : AppCompatActivity() {
    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Llamos a la función para cargar el fragment
        cargarFragment()

        //Listener de la barra de navegación inferior
        binding.bottomNavigation.setOnItemSelectedListener { menuItem ->
            //Llamamos a la función onNavigationItemSelected
            onNavigationItemSelected(menuItem)
        }

    }



    //Función para cargar el fragment inicial
    fun cargarFragment(){
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, BibliotecaFragment())
            .commit()
        binding.bottomNavigation.selectedItemId = R.id.menu_biblioteca

    }

    //Función para el listener de la barra de navegación inferior
    fun onNavigationItemSelected(menuItem: MenuItem): Boolean {

        //Elegir qué fragment cargar
        val fragment = when (menuItem.itemId) {
            R.id.menu_biblioteca -> BibliotecaFragment()
            R.id.menu_leidos -> LeidosFragment()
            R.id.menu_audio -> AudioLibrosFragment()
            R.id.menu_buscar -> BuscarFragment()
            else -> null
        }

        //Si fragment no es nulo, cargarlo( hacemos el reemplazo)
        fragment?.let {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, it )
                .commit()
            return true //Manejamos el click
        }

        return false
    }


}