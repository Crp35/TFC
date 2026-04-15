package com.example.lectorlibros.ui.adapter

import android.provider.Settings.Global.getString
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorlibros.R
import com.example.lectorlibros.databinding.ItemLibroBinding
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.ui.enums.TipoDeLibro
import com.example.lectorlibros.util.PortadaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibrosAdapter(
    private var libros: List<LibroEntity>,
    private val fragment: Fragment,
    private val onItemClick: (LibroEntity) -> Unit,
    private val onOpcionesClick: (LibroEntity, View) -> Unit
) : RecyclerView.Adapter<LibrosAdapter.LibroViewHolder>() {

    class LibroViewHolder(val binding: ItemLibroBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibroViewHolder {
        val binding = ItemLibroBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LibroViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LibroViewHolder, position: Int) {
        val libro = libros[position]
        val context = holder.itemView.context
        Log.d("LibrosAdapter", "Cargando libro: ${libro.titulo}")

        holder.binding.ivPortada.setImageResource(R.drawable.ic_libro_abierto)

        val anchoPx = (300 * holder.itemView.resources.displayMetrics.density).toInt()
        val altoPx = (400 * holder.itemView.resources.displayMetrics.density).toInt()

        fragment.lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                when (libro.tipoLibro) {
                    TipoDeLibro.PDF -> PortadaUtils.obtenerPortadaPdf(
                        context,
                        libro.uriPDF ?: "",
                        anchoPx,
                        altoPx
                    )

                    TipoDeLibro.EPUB -> PortadaUtils.obtenerPortadaEpub(
                        libro.uriPDF ?: ""
                    )

                    TipoDeLibro.AUDIO -> PortadaUtils.obtenerPortadaAudio(
                        libro.uriAudio ?: "",
                        anchoPx,
                        altoPx
                    )
                }
            }

            if (bitmap != null) {
                holder.binding.ivPortada.setImageBitmap(bitmap)
            }
        }

        val porcentaje = when (libro.tipoLibro) {
            TipoDeLibro.PDF, TipoDeLibro.EPUB -> {
                val total = libro.totalPagina ?: 0
                val actual = libro.paginaActual ?: 0
                if (total > 0) ((actual + 1) * 100 / total).coerceAtMost(100) else 0
            }

            TipoDeLibro.AUDIO -> {
                val duracionMs = libro.totalPagina?.toLong() ?: 1L
                ((libro.posicionMs ?: 0L) * 100 / duracionMs).toInt().coerceAtMost(100)
            }
        }
         //holder.binding.tvEstado.text = if (porcentaje > 0) "$porcentaje%" else context.getString(R.string.estado_nuevo)
        // Aquí manejamos los cambios en el estado del libro(Nuevo, en progreso y leído)
        holder.binding.tvEstado.text = when{
            libro.estado == EstadoLibro.COMPLETADO ->
                holder.itemView.context.getString(R.string.estado_leido)
            porcentaje > 0 -> "$porcentaje%"
            else -> holder.itemView.context.getString(R.string.estado_nuevo)
        }

        holder.binding.ivOpciones.setOnClickListener { opc ->
            onOpcionesClick(libro, opc) // opc es la view que actúa de ancla

        }

        holder.itemView.setOnClickListener {
            onItemClick(libro)
        }
    }

    override fun getItemCount(): Int = libros.size

    fun actualizarLibros(nuevosLibros: List<LibroEntity>) {
        libros = nuevosLibros
        notifyDataSetChanged()
    }
}