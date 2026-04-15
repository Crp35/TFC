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
        Log.d("DEBUG_FRAGMENT", "AudioLibrosFragment: ¡HE NACIDO!") // <--- Añade esto
        _binding = FragmentAudioLibrosBinding.inflate(inflater, container, false)
        return binding.root
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. INICIALIZACIÓN: Creamos el objeto ANTES de usarlo.
        // Esto elimina el error "lateinit property audioAdapter has not been initialized"
        audioAdapter = AudioAdapter(
            onItemClick = { libro -> handleItemClick(libro) },
            onItemLongClick = { libro -> mostrarDialogoRenombrar(libro) }
        )

        // 2. CONFIGURACIÓN DE VISTA: Ahora que audioAdapter ya existe, lo conectamos
        binding.recyclerViewAudio.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewAudio.adapter = audioAdapter

        // 3. CARGA DE DATOS: Tu lógica original intacta
        lifecycleScope.launch {
            repository.getLibrosAudio().collect { lista ->
                Log.d("DEBUG_AUDIO","LIBROS RECIBIDOS: ${lista.size}")
                if (lista.isEmpty()) {
                    binding.recyclerViewAudio.visibility = View.GONE
                    binding.tvNoAudio.visibility = View.VISIBLE
                    binding.tvNoAudio.text = getString(R.string.mensaje_audiolibros)
                    audioAdapter.submitList(lista)
                } else {
                    binding.recyclerViewAudio.visibility = View.VISIBLE
                    binding.tvNoAudio.visibility = View.GONE
                    Log.d("AudioLibrosFragment", "Lista de audiolibros: $lista")
                    audioAdapter.submitList(lista)
                    Log.d("DEBUG_AUDIO", "Enviados ${lista.size} libros al adaptador")
                }
            }
        }

        // 4. DEBUG Y POBLADO: Mantengo tu lógica de "Sutras (demo)" y raw
        lifecycleScope.launch {
            repository.getAllLibros().collect { todos ->
                Log.d("AudioLibrosFragment", "Todos los libros en BD: $todos")

                if (todos.isEmpty() && !seeded) {
                    seeded = true
                    try {
                        requireContext().resources.openRawResource(R.raw.sutras).use { input ->
                            repository.guardarAudio("Sutras (demo)", getString(R.string.nombreAutor), input, "sutras_demo.mp3")
                        }
                        Log.d("AudioLibrosFragment", "Se insertó audio de prueba desde raw")
                    } catch (e: Exception) {
                        Log.e("AudioLibrosFragment", "Error insertando audio demo: ${e.message}", e)
                    }
                }
            }
        }

        // 5. POBLADO AUTOMÁTICO: Si no hay audios, invoca poblarBDDesdeRaw
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

    private fun mostrarDialogoRenombrar(libro: LibroEntity) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle(getString(R.string.renombrar))

        val input = EditText(requireContext())
        input.setText(libro.titulo)
        builder.setView(input)

        builder.setPositiveButton(getString(R.string.gurdar_libro)){ _, _ ->
            val nuevoTitulo = input.text.toString()
            if(nuevoTitulo.isNotEmpty()){
                lifecycleScope.launch {
                    repository.renombrarLibros(libro.id, nuevoTitulo, libro.autor)
                    ToastHelper.showToast(requireContext(), getString(R.string.titulo_cambiado))
                }
            }
        }
        builder.setNegativeButton(getString(R.string.cancelar), null)
        builder.show()


    }

    private fun navigateToPlayer(libro: LibroEntity) {
        val activity = activity as? MainActivity

        // Ocultamos el menu de la actividad, si existe el binding
        activity?.binding?.ivMenu?.visibility = View.GONE
        activity?.binding?.layoutColecciones?.visibility = View.GONE

        // Actualizamos el título de la barra superior
        activity?.binding?.tvTitulo?.text = libro.titulo


        //DESCOMENTAR SI ERROR activity?.cargarFragment(AudioPlayerFragment.newInstance(libro.id, repository))


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