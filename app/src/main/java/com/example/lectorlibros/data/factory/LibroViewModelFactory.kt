package com.example.lectorlibros.data.factory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.lectorlibros.data.db.BaseDatos
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.data.repository.ServicioDescargaPdf
import com.example.lectorlibros.data.factory.LibroViewModel

import kotlin.jvm.java

class LibroViewModelFactory(
    private val repository: LibroRepository
): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LibroViewModel::class.java)) {

            @Suppress("UNCHECKED_CAST")
            return LibroViewModel(repository) as T
        }

        throw IllegalArgumentException("Unknown ViewModel class")

    }
}