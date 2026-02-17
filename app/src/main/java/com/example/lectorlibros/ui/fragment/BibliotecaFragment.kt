package com.example.lectorlibros.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.lectorlibros.R
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.databinding.FragmentPdfBinding

/**
 * A simple [Fragment] subclass.
 * Use the [BibliotecaFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BibliotecaFragment : Fragment() {
    private  var _binding: FragmentPdfBinding? = null

    lateinit var repository: LibroRepository

    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentPdfBinding.bind(view)

        android.util.Log.d("PDF_DEBUG", "onViewCreated")

        val tvEmpty = view.findViewById<TextView>(R.id.tvEmpty)

        tvEmpty.text = getString(R.string.mensaje_no_libros)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        //_binding = FragmentPdfBinding.inflate(inflater, container, false)
        _binding = FragmentPdfBinding.inflate(
            inflater,
            container,
            false
            )
        return binding.root

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(repository: LibroRepository) : BibliotecaFragment {
            val fragment = BibliotecaFragment()
            fragment.repository = repository
            return fragment
        }

    }

}