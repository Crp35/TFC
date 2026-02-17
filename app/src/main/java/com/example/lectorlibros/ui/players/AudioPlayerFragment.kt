package com.example.lectorlibros.ui.fragments

import android.media.MediaPlayer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.databinding.FragmentAudioPlayerBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AudioPlayerFragment(
    private val repository: LibroRepository
) : Fragment() {

    private var _binding: FragmentAudioPlayerBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null
    private var libro: LibroEntity? = null

    private var updateJob: Job? = null
    private var isPrepared = false

    companion object {
        private const val ARG_LIBRO = "libro"
        fun newInstance(libro: LibroEntity, repository: LibroRepository) =
            AudioPlayerFragment(repository).apply {
                arguments = Bundle().apply {
                    putSerializable(ARG_LIBRO, libro)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        libro = arguments?.getSerializable(ARG_LIBRO) as? LibroEntity

        libro?.let {
            binding.imgCover.setImageResource(R.drawable.ic_icon2_background)
            binding.tvTitle.text = it.titulo

            binding.btnPlayPause.setOnClickListener { togglePlayPause() }
            binding.btnStop.setOnClickListener { stopAudio() }
            binding.btnAtras.setOnClickListener { seekBackward() }
            binding.btnAdelante.setOnClickListener { seekForward() }
        }
    }

    private fun togglePlayPause() {

        if (mediaPlayer == null) {

            val audioPath = libro?.uriAudio ?: return

            mediaPlayer = MediaPlayer().apply {

                try {
                    setDataSource(audioPath)

                    setOnPreparedListener { mp ->
                        isPrepared = true

                        binding.seekBar.max = mp.duration
                        binding.tvTiempoTotal.text = formatTime(mp.duration)

                        // Restaurar posición guardada desde BD
                        val posicionGuardada = libro?.ultimaPosicion ?: 0
                        if (posicionGuardada > 0) {
                            mp.seekTo(posicionGuardada)
                        }

                        mp.start()
                        binding.btnPlayPause.setImageResource(R.drawable.ic_pause)

                        startUpdatingUI()
                    }

                    setOnCompletionListener {
                        binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                        binding.seekBar.progress = 0
                        binding.tvTiempoActual.text = formatTime(0)

                        libro?.ultimaPosicion = 0
                        savePositionToDb() // Guardar en BD
                        stopUpdatingUI()
                    }

                    prepareAsync()

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            return
        }

        mediaPlayer?.let { mp ->

            if (!isPrepared) return

            if (mp.isPlaying) {
                mp.pause()

                // Guardar posición en memoria y BD
                libro?.ultimaPosicion = mp.currentPosition
                savePositionToDb()

                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                stopUpdatingUI()
            } else {
                mp.start()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                startUpdatingUI()
            }
        }
    }

    private fun startUpdatingUI() {
        updateJob?.cancel()

        updateJob = lifecycleScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                val position = mediaPlayer?.currentPosition ?: 0
                binding.seekBar.progress = position
                binding.tvTiempoActual.text = formatTime(position)
                delay(500)
            }
        }
    }

    private fun stopUpdatingUI() {
        updateJob?.cancel()
    }

    private fun stopAudio() {
        mediaPlayer?.let {
            if (isPrepared) {
                it.pause()
                it.seekTo(0)

                libro?.ultimaPosicion = 0
                savePositionToDb()

                binding.seekBar.progress = 0
                binding.tvTiempoActual.text = formatTime(0)
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)

                stopUpdatingUI()
            }
        }
    }

    private fun seekBackward() {
        val newPosition = (mediaPlayer?.currentPosition ?: 0) - 10000
        mediaPlayer?.seekTo(newPosition.coerceAtLeast(0))
    }

    private fun seekForward() {
        val newPosition = (mediaPlayer?.currentPosition ?: 0) + 10000
        mediaPlayer?.seekTo(newPosition.coerceAtMost(mediaPlayer?.duration ?: 0))
    }

    private fun formatTime(ms: Int): String {
        val minutos = (ms / 1000) / 60
        val segundos = (ms / 1000) % 60
        return String.format("%02d:%02d", minutos, segundos)
    }

    private fun savePositionToDb() {
        // Llamada segura a repository para persistir posición
        libro?.let { libro ->
            lifecycleScope.launch {
                repository.updateLibro(libro)
            }
        }
    }

    override fun onDestroyView() {
        // Guardar posición antes de salir
        mediaPlayer?.let {
            if (isPrepared) {
                libro?.ultimaPosicion = it.currentPosition
                savePositionToDb()
            }
        }

        stopUpdatingUI()
        mediaPlayer?.release()
        mediaPlayer = null
        isPrepared = false
        _binding = null

        super.onDestroyView()
    }
}
