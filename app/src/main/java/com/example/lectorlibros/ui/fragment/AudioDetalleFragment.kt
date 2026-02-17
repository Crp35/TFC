package com.example.lectorlibros.ui.fragment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.lectorlibros.R

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [AudioDetalleFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class AudioDetalleFragment : Fragment() {
    companion object {

        private const val ARG_LIBRO_ID = "libro_id"

        fun newInstance(libroId: Int): AudioDetalleFragment {
            val fragment = AudioDetalleFragment()
            val args = Bundle()
            args.putInt(ARG_LIBRO_ID, libroId)
            fragment.arguments = args
            return fragment
        }
    }

    private var libroId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        libroId = arguments?.getInt(ARG_LIBRO_ID) ?: -1
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (libroId == -1) {
            return
        }

        // Aquí cargarás el libro desde base de datos usando libroId
    }
}