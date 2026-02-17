package com.example.lectorlibros.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.lectorlibros.R
import com.example.lectorlibros.databinding.FragmentPdfBinding

/**
 * A simple [Fragment] subclass.
 * Use the [PdfFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PdfFragment : Fragment() {

    private  var _binding: FragmentPdfBinding? = null
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
        return inflater.inflate(R.layout.fragment_pdf, container, false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}