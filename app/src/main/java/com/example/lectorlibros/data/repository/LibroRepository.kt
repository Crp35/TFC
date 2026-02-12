package com.example.lectorlibros.data.repository

import com.example.lectorlibros.data.db.LibroDao
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.ui.enums.TipoCeleccion
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.flow.Flow

class LibroRepository(private val libroDao: LibroDao) {


    //Insertar un libro en la BD
    suspend fun insertLibro(libro: LibroEntity){
        libroDao.insertLibro(libro)
    }

    //Obtener todos los libros
    fun getAllLibros(): Flow<List<LibroEntity>> = libroDao.getAllLibros()


    //Obtener libros en formato PDF
    fun getLibrosPDF(): Flow<List<LibroEntity>> = libroDao.getLibrosPDF()

    //Obtener libros en formato Audio
    fun getLibrosAudio(): Flow<List<LibroEntity>> = libroDao.getLibrosAudio()

    //Actualizar un libro
    suspend fun cambiarTituloAutor(libro: LibroEntity){
        libroDao.updateLibro(libro.id, libro.titulo, libro.autor)
        }

    //Obtener un libro por título
    suspend fun  getLibroByTitulo(titulo: String) = libroDao.getLibroByTitulo(titulo)

    //Obtener un libro por autor
    suspend fun getLibroByAutor(autor: String) = libroDao.getLibroByAutor(autor)

    //Contadores unificados
    suspend fun contarLibros(tipo: TipoCeleccion): Int{
        return when(tipo){
            TipoCeleccion.TODOS -> libroDao.countTotalLibros()
            TipoCeleccion.DESCARGADOS -> libroDao.countTotalLibrosDescargados()
            TipoCeleccion.TERMINADOS -> libroDao.countLibrosLeidos()
            TipoCeleccion.PDF -> libroDao.countLibrosTotalLibrosPDF()
            TipoCeleccion.AUDIO -> libroDao.countLibrosTotalLibrosAudio()
        }

    }



    //Función de prueba para insertar libros en la BD
    suspend fun pruebaInsertarLibros() {
        if (libroDao.countTotalLibros() == 0) {
            insertLibro(
                LibroEntity(
                    titulo = "El principito",
                    autor = "Antoine de Saint-Exupéry",
                    tipoLibro = TipoDeLibro.PDF
                )
            )

            insertLibro(
                LibroEntity(
                    titulo = "La llamada de Cthulhu",
                    autor = "H. P. Lovecraft",
                    tipoLibro = TipoDeLibro.AUDIO
                )
            )

            insertLibro(
                LibroEntity(
                    titulo = "El señor de los anillos",
                    autor = "J. R. R. Tolkien",
                    tipoLibro = TipoDeLibro.PDF
                )
            )

        }

    }

}








