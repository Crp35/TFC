package com.example.lectorlibros.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.databinding.ItemLibroBinding

class LibrosAdapter(private var libros: List<LibroEntity>
): RecyclerView.Adapter<LibrosAdapter.LibroViewHolder>() {

    inner class LibroViewHolder(val binding: ItemLibroBinding):
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
        holder.binding.tvTitulo.text = libro.titulo
        holder.binding.tvTipo.text= libro.tipoLibro.toString()
    }

    override fun getItemCount(): Int{
        return libros.size
    }

    fun actualizarLibros(nuevosLibros: List<LibroEntity>) {

        libros = nuevosLibros
        notifyDataSetChanged()

    }


}