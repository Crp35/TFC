package com.example.lectorlibros.ui.viewModel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoCeleccion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * Esta es la clase que habla con Fragmente;
 * ya que esta última no debería hablar directamente con Room
 * */
class LibroViewModel(private val repository: LibroRepository) : ViewModel() {

    val librosPDF : Flow<List<LibroEntity>> = repository.getLibrosPDF()
    val librosAudio : Flow<List<LibroEntity>> = repository.getLibrosAudio()
    val todosLosLibros : Flow<List<LibroEntity>> = repository.getAllLibros()


    //Función para filtrar libros según su tipo
    fun obtenerLibrosPorColeccion(tipo: TipoCeleccion): Flow<List<LibroEntity>>{
        return when(tipo){
            TipoCeleccion.TODOS -> todosLosLibros
            TipoCeleccion.PDF -> librosPDF
            TipoCeleccion.AUDIO -> librosAudio
            TipoCeleccion.DESCARGADOS -> flow {
                emit(repository.getAllLibros().first().filter { it.descargado})
            }
            TipoCeleccion.TERMINADOS -> flow {
                emit(repository.getAllLibros().first().filter {
                    it.estado == EstadoLibro.COMPLETADO})
            }

        }
    }

    fun cambiarTituloAutor(libro: LibroEntity){

        viewModelScope.launch {
            repository.cambiarTituloAutor(libro)
        }

    }

    //Obtener por su titulo
    suspend fun obtenerLibro(titulo: String): LibroEntity? {
        return repository.getLibroByTitulo(titulo)

    }

    //Obtener por su autor
    suspend fun obtenerLibroPorAutor(autor: String): LibroEntity? {
        return repository.getLibroByAutor(autor)
    }

    /*//Obtener el número total de libros en progreso
   suspend fun librosEnProgreso() = repository.countLibrosEnProgreso()



  //Obtener el número total de libros descargados
   suspend fun librosDescargados() = repository.getLibrosDescargados()

   //Obtener el número total de libros
   suspend fun numeroTotalLibros() = repository.getNumeroTotalLibros()

   //Obtener el número de libros leídos
   suspend fun  librosLeidos() = repository.getLibrosLeidos()*/

}


