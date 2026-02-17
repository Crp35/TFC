package com.example.lectorlibros.ui.fragment

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.databinding.FragmentAudioPlayerBinding

class AudioPlayerFragment : Fragment() {

    private var _binding: FragmentAudioPlayerBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null
    private var libro: LibroEntity? = null

    private var isPlaying = false

    companion object {
        private const val ARG_LIBRO = "libro"

        fun newInstance(libro: LibroEntity): AudioPlayerFragment {
            val fragment = AudioPlayerFragment()
            val args = Bundle().apply {
                putSerializable(ARG_LIBRO, libro)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Obtener el libro del argumento
        libro = arguments?.getSerializable(ARG_LIBRO) as? LibroEntity

        if (libro == null) {
            Toast.makeText(requireContext(), "No se encontró el libro", Toast.LENGTH_SHORT).show()
            return
        }

        // Mostrar información del libro
        binding.tvTitle.text = libro?.titulo
        // Cargar la portada del libro (si está disponible)
        cargarPortada(libro)

        // Configurar el MediaPlayer
        mediaPlayer = MediaPlayer().apply {
            setDataSource(libro?.uriAudio)
            setOnPreparedListener {
                binding.seekBar.max = duration
                it.start()
                this@AudioPlayerFragment.isPlaying = true
                actualizarBotones()
            }

            setOnCompletionListener {
                this@AudioPlayerFragment.isPlaying = false
                actualizarBotones()
            }

            prepareAsync()
        }

        // Configuración del SeekBar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    mediaPlayer?.seekTo(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Botón de reproducción / pausa
        binding.btnPlayPause.setOnClickListener {
            if (isPlaying) {
                mediaPlayer?.pause()
            } else {
                mediaPlayer?.start()
            }
            isPlaying = !isPlaying
            actualizarBotones()
        }
    }

    private fun actualizarBotones() {
        if (isPlaying) {
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
        } else {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        }
    }

    private fun cargarPortada(libro: LibroEntity?) {
        val rutaAudio = libro?.uriAudio
        if (rutaAudio != null) {
            try {
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(rutaAudio)
                val art = retriever.embeddedPicture
                if (art != null) {
                    val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    binding.imgCover.setImageBitmap(bitmap)
                } else {
                    binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
            }
        } else {
            binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        _binding = null
    }
}
