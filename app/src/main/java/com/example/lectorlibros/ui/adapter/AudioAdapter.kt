package com.example.lectorlibros.ui.adapter

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorlibros.R
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.databinding.ItemAudioBinding


class AudioAdapter(
    private val onItemClick: ((LibroEntity) -> Unit)? = null
) : ListAdapter<LibroEntity, AudioAdapter.AudioViewHolder>(DiffCallback()) {

    inner class AudioViewHolder(val binding: ItemAudioBinding) : RecyclerView.ViewHolder(
        binding.root) {

        fun bind(libro: LibroEntity) {
            binding.tvTitle.text = libro.titulo

            val ruta = libro.uriAudio
            if (!ruta.isNullOrEmpty()) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(ruta)
                    val art = retriever.embeddedPicture
                    if (art != null) {
                        val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                        binding.imgCover.setImageBitmap(bitmap)
                    } else {
                        binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
                    }
                    retriever.release()
                } catch (ex: Exception) {
                    Log.e("AudioAdapter", "Error obteniendo artwork del audio: ${ex.message}", ex)
                    binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
                }
            } else {
                binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
            }

            binding.root.setOnClickListener {
                Log.d("AudioAdapter", "Item clicked: id=${libro.id} title=${libro.titulo}")
                onItemClick?.invoke(libro)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val binding = ItemAudioBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AudioViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val libro = getItem(position)
        holder.binding.root.setOnClickListener { onItemClick?.invoke(libro) }
        //holder.bind(getItem(position))
    }

    class DiffCallback : DiffUtil.ItemCallback<LibroEntity>() {
        override fun areItemsTheSame(oldItem: LibroEntity, newItem: LibroEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LibroEntity, newItem: LibroEntity): Boolean {
            return oldItem == newItem
        }
    }
}
