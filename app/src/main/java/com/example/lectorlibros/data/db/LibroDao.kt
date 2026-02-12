package com.example.lectorlibros.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoCeleccion
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

    @Query("SELECT * FROM libro WHERE tipoLibro = :tipoDeLibro")
    suspend fun getLibrosPorTipo(tipoDeLibro: TipoDeLibro): LibroEntity?

    @Query("SELECT * FROM libro WHERE titulo = :titulo")
    suspend fun getLibroByTitulo(titulo: String): LibroEntity?


    @Query("SELECT * FROM libro WHERE autor = :autor")
    suspend fun getLibroByAutor(autor: String): LibroEntity?

    /**
     * OBTENEMOS OS LIBROS SEGÚN EL FORMATO DE ÉSTE: PDF, AUDIO, EPUB, ETC.,
     */
    @Query("SELECT * FROM libro WHERE tipoLibro = 'PDF'")
    fun getLibrosPDF(): Flow<List<LibroEntity>>

    @Query("SELECT * FROM libro WHERE tipoLibro = 'AUDIO'")
    fun getLibrosAudio(): Flow<List<LibroEntity>>

    /**
     * CONTADORES DE LIBROS: TODOS, DESCARGADOS, TERMINADOS, PDF, AUDIO
     * */
    @Query("SELECT COUNT(*) FROM libro")
    suspend fun countTotalLibros(): Int

    @Query("SELECT COUNT(*) FROM libro WHERE leido = '1'")
    suspend fun countLibrosLeidos(): Int

    @Query("SELECT COUNT(*) FROM libro WHERE estado = 'EN_PROGRESO'")
    suspend fun countLibrosEnProgreso(): Int

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
    suspend fun updateLibro( id: Long, titulo: String, autor: String)

    /**
     * Elimina un libro por su id
     * */
    @Query("DELETE FROM libro WHERE id = :id")
    suspend fun deleteLibroById(id: Long)


}