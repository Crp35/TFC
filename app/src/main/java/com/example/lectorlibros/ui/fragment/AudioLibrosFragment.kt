package com.example.lectorlibros.ui.fragment

import android.media.MediaPlayer
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.lectorlibros.R
import com.example.lectorlibros.databinding.FragmentAudioLibrosBinding
import com.example.lectorlibros.databinding.FragmentPdfBinding

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AudioLibrosFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AudioLibrosFragment : Fragment() {
    private var _binding: FragmentAudioLibrosBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAudioLibrosBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvEmpty.text = getString(R.string.mensaje_no_libros)

        binding.btnPlay.setOnClickListener {
            playAudio()
        }

        binding.btnPause.setOnClickListener {
            pauseAudio()
        }

        binding.btnStop.setOnClickListener {
            stopAudio()
        }



        _binding = FragmentAudioLibrosBinding.bind(view)

        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        tvEmpty.text = getString(R.string.mensaje_no_libros)

    }

    private fun playAudio() {
        if (mediaPlayer?.isPlaying == null) {
            mediaPlayer = MediaPlayer.create(requireContext(), R.raw.sonido)
        }

        mediaPlayer?.start()
    }

    private fun pauseAudio() {
        if (mediaPlayer?.isPlaying == true) {
            mediaPlayer?.pause()
        }

    }

    private fun stopAudio() {
        mediaPlayer?.release()
        mediaPlayer = null
    }


    override fun onDestroyView() {
        stopAudio()
        _binding = null
        super.onDestroyView()
    }
}



