package com.example.lectorlibros.ui.adapter

import android.content.Context
import android.provider.Settings.Global.getString
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.example.lectorlibros.R

class ColeccionAdapter(
    private val context: Context,
    private val titulos: Array<String>,
    private val iconos: Array<Int>
) : ArrayAdapter<String>(context, R.layout.item_coleccion, titulos) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val inflater = LayoutInflater.from(context)
        val view = convertView ?: inflater.inflate(R.layout.item_coleccion,
            parent, false)

        val imageIcono = view.findViewById<ImageView>(R.id.icIcono)
        val textView = view.findViewById<TextView>(R.id.tvNombre)

        imageIcono.setImageResource(iconos[position])
        textView.text = titulos[position]

        return view
    }


}


