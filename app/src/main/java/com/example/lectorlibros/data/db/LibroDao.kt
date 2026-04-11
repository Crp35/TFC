package com.example.lectorlibros.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import kotlinx.coroutines.flow.Flow

@Dao
interface LibroDao {
    /**
     * Inserta un libro(PDF o AUDIO)
     * */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibro(libro: LibroEntity)

    /**
     * FILTROS: TODOS, POR TIPO, TÍTULO,AUTOR
     * */
    @Query("SELECT * FROM libro ")
    fun getAllLibros(): Flow<List<LibroEntity>>

    @Query("SELECT * FROM libro WHERE titulo = :titulo")
    suspend fun getLibroByTitulo(titulo: String): LibroEntity?


    @Query("SELECT * FROM libro WHERE autor = :autor")
    suspend fun getLibroByAutor(autor: String): LibroEntity?

    /**
     * OBTENEMOS LOS LIBROS SEGÚN EL FORMATO: PDF, AUDIO, EPUB, ETC.
     */
    @Query("SELECT * FROM libro WHERE tipoLibro = 'PDF'")
    fun getLibrosPDF(): Flow<List<LibroEntity>>

    // Obtenemos por parámetro el tipo (ej. TipoDeLibro.AUDIO)
    @Query("SELECT * FROM libro WHERE tipoLibro = :tipo")
    fun getLibrosAudio(tipo: TipoDeLibro): Flow<List<LibroEntity>>

    // Nuevo: obtener libro por id
    @Query("SELECT * FROM libro WHERE id = :id")
    suspend fun getLibroById(id: Long): LibroEntity?

    /**
     * CONTADORES DE LIBROS: TODOS, DESCARGADOS, TERMINADOS, PDF, AUDIO
     * */
    @Query("SELECT COUNT(*) FROM libro")
    suspend fun countTotalLibros(): Int

    @Query("SELECT COUNT(*) FROM libro WHERE leido = '1'")
    suspend fun countLibrosLeidos(): Int

    @Query("SELECT COUNT(*) FROM libro WHERE estado = 'EN_PROGRESO'")
    suspend fun countLibrosEnProgreso(): Int

    @Query("SELECT * FROM libro WHERE estado = :estado ")
    suspend fun getLibrosPorEstado(estado: EstadoLibro): List<LibroEntity>


    @Query("SELECT COUNT(*) FROM libro WHERE tipoLibro = 'AUDIO'")
    suspend fun countLibrosTotalLibrosAudio(): Int

    @Query("SELECT COUNT(*) FROM libro WHERE descargado = '1'")
    suspend fun countTotalLibrosDescargados(): Int
    @Query("SELECT COUNT(*) FROM libro WHERE tipoLibro = 'PDF'")
    suspend fun countLibrosTotalLibrosPDF(): Int


    /**
     * ACTUALIZAMOS SOLO EL TÍTULO Y EL AUTOR DE UN LIBRO
     * */
 @Query("""
        UPDATE 
                libro
        SET 
                titulo = :titulo,
                autor = :autor
        WHERE 
                id = :id
    """)
    suspend fun actualizarLibro(id: Long, titulo: String, autor: String)

    /**
     * Elimina un libro por su id
     * */
    @Query("DELETE FROM libro WHERE id = :id")
    suspend fun eliminarLibros(id: Long)

    /**
     * Método que actualiza la posición del audio
     * */
    @Query("UPDATE libro SET posicionMs = :posicionMs WHERE id = :id")
    suspend fun actualizarPosicionAudio(id: Long, posicionMs: Long)

    @Query("SELECT * FROM libro WHERE titulo = :titulo LIMIT 1")
    suspend fun getLibro(titulo: String): LibroEntity?

    //@Query("SELECT * FROM libro WHERE titulo LIKE '%' || :texto || '%'")
    @Query("SELECT * FROM libro WHERE titulo LIKE :texto COLLATE NOCASE")
    fun buscarLibrosPorTitulo(texto: String): Flow<List<LibroEntity>>




    @Update
    suspend fun actualizarLibro(libro: LibroEntity)


}