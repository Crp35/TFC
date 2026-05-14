package com.example.lectorlibros.ui.fragment

import AudioPlayerFragment
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.lectorlibros.ui.activity.MainActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.lectorlibros.R
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.databinding.FragmentAudioLibrosBinding
import com.example.lectorlibros.ui.adapter.AudioAdapter
import com.example.lectorlibros.ui.enums.TipoColeccion
import com.example.lectorlibros.util.ToastHelper
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class AudioLibrosFragment(
    private val repository: LibroRepository
) : Fragment() {

    private var _binding: FragmentAudioLibrosBinding? = null
    private val binding get() = _binding!!

    private lateinit var audioAdapter: AudioAdapter

    // Pendiente para navegación tras conceder permiso
    private var pendingLibro: LibroEntity? = null

    private var seeded = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted: Boolean ->
        if (granted) {
            pendingLibro?.let { libro ->
                navigateToPlayer(libro)
            }
        } else {
            ToastHelper.showToast(requireContext(),
                "Permiso denegado: no se puede reproducir audio externo")
        }
        pendingLibro = null
    }

    companion object {
        private const val ARG_LIBRO_ID = "libro_id"

        fun newInstance(libroId: Long, repository: LibroRepository) =
            AudioPlayerFragment(repository).apply {
                arguments = Bundle().apply { putLong(ARG_LIBRO_ID, libroId) }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioLibrosBinding.inflate(inflater, container, false)
        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Inicializar el adaptador pasando AMBOS callbacks
        audioAdapter = AudioAdapter(
            onItemClick = { libro ->
                handleItemClick(libro)
            },
            onItemLongClick = { libro ->
                // Como el adaptador no nos da la vista del item, usamos la vista raíz del fragmento como ancla para el menú
                showPopupMenu(libro, binding.root)
            }
        )


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
                    Log.d("AudioLibrosFragment", "Lista de audiolibros: $lista")
                    audioAdapter.submitList(lista)  // Actualiza la lista en el adaptador
                }
            }
        }

        // Debug: también listamos todos los libros (por si el filtro falla)
        lifecycleScope.launch {
            repository.getAllLibros().collect { todos ->
                Log.d("AudioLibrosFragment", "Todos los libros en BD: $todos")

                // Si está vacío, insertar un audio de prueba desde raw una sola vez
                if (todos.isEmpty() && !seeded) {
                    seeded = true
                    try {
                        requireContext().resources.openRawResource(R.raw.sutras).use { input ->
                            repository.guardarAudio("Sutras (demo)", getString(R.string.autor_desconocido), input, "sutras_demo.mp3")
                        }
                        Log.d("AudioLibrosFragment", "Se insertó audio de prueba desde raw")
                    } catch (e: Exception) {
                        Log.e("AudioLibrosFragment", "Error insertando audio demo: ${e.message}", e)
                    }
                }
            }
        }

        // Si no hay audios, poblar desde raw (debug)
        lifecycleScope.launch {
            try {
                val cant = repository.contarLibros(TipoColeccion.AUDIO)
                Log.d("AudioLibrosFragment", "Cantidad de audios en BD: $cant")
                if (cant == 0) {
                    Log.d("AudioLibrosFragment", "Poblando BD desde raw para pruebas...")
                    repository.poblarBDDesdeRaw()
                }
            } catch (e: Exception) {
                Log.e("AudioLibrosFragment", "Error al poblar BD: ${e.message}", e)
            }
        }
    }

    // Función para mostrar el menú CRUD
    @RequiresApi(Build.VERSION_CODES.Q)
    fun showPopupMenu(libro: LibroEntity, ancla: View) {
        val popup = PopupMenu(requireContext(), ancla)
        popup.inflate(R.menu.menu_item_libro)

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {

                R.id.action_rename -> {
                    mostrarDialogoRenombrar(libro)
                    true
                }

                R.id.action_delete -> {
                    // Llamada a la función de eliminación real
                    confirmarEliminacion(libro)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // Método para mostrar diálogo renombrar libros

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun mostrarDialogoRenombrar(libro: LibroEntity) {
        val abortar = getString(R.string.cancelar)
        val salvarEdicion = getString(R.string.gurdar_libro)
        val textString = getString(R.string.renombrar_libro)
        val tituloCambiado = getString(R.string.titulo_cambiado)

        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(textString)

        val input = EditText(requireContext())
        input.setText(libro.titulo)
        input.selectAll()

        val container = FrameLayout(requireContext())
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(60, 20, 60, 0)
        input.layoutParams = params
        container.addView(input)

        builder.setPositiveButton(salvarEdicion) { _, _ ->
            val nuevoTitulo = input.text.toString().trim()
            if(nuevoTitulo.isNotEmpty()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    repository.renombrarLibros(libro.id, nuevoTitulo, libro.autor)

                    val listaActualizada = repository.getLibrosAudio().firstOrNull() ?: emptyList()
                    audioAdapter.submitList(listaActualizada.toList())

                    Toast.makeText(requireContext(), tituloCambiado,
                        Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.setNegativeButton(abortar, null)
        builder.setView(container)
        builder.show()
    }

    // Fin método para mostrar diálogo renombrar libros

    // Lógica para eliminar el libro de la base de datos
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun confirmarEliminacion(libro: LibroEntity) {
        val cancelar = getString(R.string.cancelar)
        val preguntaEliminar = getString(R.string.confirmar_eliminar)
        val eliminar = getString(R.string.eliminar)
        val elimino = getString(R.string.confirmar_eliminado)
        val eliminado = getString(R.string.eliminar_lib)
        AlertDialog.Builder(requireContext())
            .setTitle(eliminar)
            // El usuario ve el título, así sabe qué está borrando
            .setMessage("$preguntaEliminar\n'${libro.titulo}'?")
            .setPositiveButton(elimino) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    // Usamos el ID internamente para que la DB sepa exactamente cuál es
                    repository.deleteLibro(libro)

                    Toast.makeText(requireContext(), "${libro.titulo} $eliminado",
                        Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(cancelar, null)
            .show()
    }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private fun hasPermission(): Boolean {
        val permiso = getRequiredPermission()
        return ContextCompat.checkSelfPermission(requireContext(), permiso) == PackageManager.PERMISSION_GRANTED
    }

    private fun isInternalFile(path: String?): Boolean {
        if (path == null) return false
        val filesDir = requireContext().filesDir.absolutePath
        return path.startsWith(filesDir)
    }

    private fun handleItemClick(libro: LibroEntity) {
        Log.d("AudioLibrosFragment", "handleItemClick: id=${libro.id} title=${libro.titulo} uri=${libro.uriAudio}")
        val path = libro.uriAudio
        if (path == null) {
            ToastHelper.showToast(requireContext(), "Audio no disponible")
            return
        }

        if (isInternalFile(path)) {
            navigateToPlayer(libro)
            return
        }

        if (hasPermission()) {
            navigateToPlayer(libro)
            return
        }

        // Pedir permiso y guardar libro pendiente
        pendingLibro = libro
        requestPermissionLauncher.launch(getRequiredPermission())
    }

    private fun navigateToPlayer(libro: LibroEntity) {
        val activity = activity as? MainActivity

        // Ocultamos el menu de la actividad, si existe el binding
        activity?.binding?.ivMenu?.visibility = View.GONE
        activity?.binding?.layoutColecciones?.visibility = View.GONE

        // Actualizamos el título de la barra superior
        activity?.binding?.tvTitulo?.text = libro.titulo


        activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id, repository))


        Log.d("AudioLibrosFragment", "navigateToPlayer: id=${libro.id} title=${libro.titulo}")
        ToastHelper.showToast(requireContext(), "Seleccionado: ${libro.titulo}")
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, AudioPlayerFragment.newInstance(libro.id, repository))
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        _binding = null  // Limpiamos el binding
        super.onDestroyView()
    }
}