package com.example.lectorlibros.ui.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lectorlibros.R
import com.example.lectorlibros.data.db.LibroEntity
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.databinding.FragmentAudioLibrosBinding
import com.example.lectorlibros.ui.adapter.AudioAdapter
import kotlinx.coroutines.launch

class AudioLibrosFragment(
    private val repository: LibroRepository
) : Fragment() {

    private var _binding: FragmentAudioLibrosBinding? = null
    private val binding get() = _binding!!

    private lateinit var audioAdapter: AudioAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioLibrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar el adaptador pasando solo repository
        audioAdapter = AudioAdapter(repository = repository)

        binding.recyclerViewAudio.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = audioAdapter
        }

        // Cargar lista de todos los audiolibros desde la base de datos
        lifecycleScope.launch {
            repository.getLibrosAudio().collect { lista ->
                if (lista.isEmpty()) {
                    binding.recyclerViewAudio.visibility = View.GONE
                    binding.tvNoAudio.visibility = View.VISIBLE
                } else {
                    binding.recyclerViewAudio.visibility = View.VISIBLE
                    binding.tvNoAudio.visibility = View.GONE
                    audioAdapter.submitList(lista)  // Actualiza la lista en el adaptador
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null  // Limpiamos el binding
        super.onDestroyView()
    }
}
