package com.example.lectorlibros.ui.adapter

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.databinding.ItemAudioBinding
import com.example.lectorlibros.ui.fragments.AudioPlayerFragment
import com.example.lectorlibros.data.repository.LibroRepository
import androidx.fragment.app.Fragment

class AudioAdapter(
    private val repository: LibroRepository
) : ListAdapter<LibroEntity, AudioAdapter.AudioViewHolder>(DiffCallback()) {

    inner class AudioViewHolder(val binding: ItemAudioBinding) : RecyclerView.ViewHolder(binding.root) {

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
                } catch (e: Exception) {
                    binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
                }
            } else {
                binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
            }

            // Este es el cambio, pasamos el repository al fragmento
            binding.root.setOnClickListener {
                val fragment = AudioPlayerFragment.newInstance(libro, repository) // Aquí pasamos el libro y el repository
                fragment.parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, fragment)
                    .addToBackStack(null)
                    .commit()
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
        holder.bind(getItem(position))
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
