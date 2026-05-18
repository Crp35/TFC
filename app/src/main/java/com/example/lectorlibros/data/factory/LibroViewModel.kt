package com.example.lectorlibros.data.factory

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoColeccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Esta es la clase que habla con Fragmente;
 * ya que esta última no debería hablar directamente con Room
 * */
class LibroViewModel(
    private val repository: LibroRepository
) : ViewModel() {

    @RequiresApi(Build.VERSION_CODES.Q)
    val librosPDF: Flow<List<LibroEntity>> = repository.getLibrosPDF()

    @RequiresApi(Build.VERSION_CODES.Q)
    val librosAudio: Flow<List<LibroEntity>> = repository.getLibrosAudio()

    @RequiresApi(Build.VERSION_CODES.Q)
    val todosLosLibros: Flow<List<LibroEntity>> = repository.getAllLibros()

    @RequiresApi(Build.VERSION_CODES.Q)
    fun descargarLibro(libro: LibroEntity, urlPDF: String) {
        viewModelScope.launch {
            repository.descargarLibro(libro, urlPDF)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun buscarLibrosPorTitulo(titulo: String): Flow<List<LibroEntity>> {
        return repository.buscarLibrosPorTitulo(titulo)
    }

    // Función para filtrar libros según su tipo
    @RequiresApi(Build.VERSION_CODES.Q)
    fun obtenerLibrosPorColeccion(tipo: TipoColeccion): Flow<List<LibroEntity>> {
        return when (tipo) {
            TipoColeccion.TODOS -> todosLosLibros

            TipoColeccion.PDF -> librosPDF

            TipoColeccion.AUDIO -> librosAudio

            TipoColeccion.DESCARGADOS -> flow {
                emit(repository.getAllLibros().first().filter { it.descargado })
            }

            TipoColeccion.TERMINADOS -> flow {
                emit(
                    repository.getAllLibros().first().filter {
                        it.estado == EstadoLibro.COMPLETADO
                    }
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun buscarLibrosPorTituloSeguro(titulo: String): Flow<List<LibroEntity>> {
        return repository.buscarLibrosPorTitulo(titulo)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun obtenerLibro(titulo: String): LibroEntity? {
        return repository.getLibroByTitulo(titulo)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun obtenerLibroPorAutor(autor: String): LibroEntity? {
        return repository.getLibroByAutor(autor)
    }
}