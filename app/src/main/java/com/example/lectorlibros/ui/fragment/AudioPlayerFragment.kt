import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.lectorlibros.R
import com.example.lectorlibros.data.repository.LibroRepository
import com.example.lectorlibros.databinding.FragmentAudioPlayerBinding
import com.example.lectorlibros.entities.LibroEntity
import com.example.lectorlibros.ui.activity.MainActivity
import com.example.lectorlibros.ui.enums.EstadoLibro
import com.example.lectorlibros.util.ToastHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.scale

class AudioPlayerFragment(
    private val repository: LibroRepository
) : Fragment() {

    private var _binding: FragmentAudioPlayerBinding? = null
    private val binding get() = _binding!!

    private var libroIdInterno: Long = -1L
    private var mediaPlayer: MediaPlayer? = null
    private var libro: LibroEntity? = null
    private var updateJob: Job? = null
    private var isPrepared = false
    private var controlesVisibles = true

    @RequiresApi(Build.VERSION_CODES.Q)
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            setupMediaPlayer()
        } else {
            ToastHelper.showToast(
                context ?: return@registerForActivityResult,
                "Permiso necesario para audio externo"
            )
        }
    }

    companion object {
        private const val ARG_LIBRO_ID = "libro_id"

        fun newInstance(libroId: Long, repository: LibroRepository): AudioPlayerFragment {
            val fragment = AudioPlayerFragment(repository)
            fragment.arguments = Bundle().apply { putLong(ARG_LIBRO_ID, libroId) }
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioPlayerBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        libroIdInterno = arguments?.getLong(ARG_LIBRO_ID) ?: -1L

        binding.imgCover.setOnClickListener {
            if(!controlesVisibles){
                alternarControles() // Si los controles están ocultos, los muestra
            }
            reinciarTemporizadorOcultar() // Pase lo que pase, reinicia el temporizador
        }

        lifecycleScope.launch {
            libro = repository.getLibroById(libroIdInterno)
            val currentLibro = libro ?: return@launch

            // Imagen por defecto
            binding.imgCover.setImageResource(R.drawable.book_open_book_read_icon)

            // Cargar portada en segundo plano
            val anchoPx = (300 * resources.displayMetrics.density).toInt()
            val altoPx = (500 * resources.displayMetrics.density).toInt()
            lifecycleScope.launch {
                val bitmap = withContext(Dispatchers.IO) {
                    obtenerPortadaAudio(currentLibro.uriAudio ?: "", anchoPx, altoPx)
                }
                if (_binding != null && bitmap != null) {
                    binding.imgCover.setImageBitmap(bitmap)
                }
            }

            // Inicializar botón volver invisible
            binding.btnVolver.visibility = View.GONE

            // Listeners de botones
            binding.btnPlayPause.setOnClickListener { togglePlayPause() }
            binding.btnStop.setOnClickListener { stopAudio() }
            binding.btnAtras.setOnClickListener { seekBackward() }
            binding.btnAdelante.setOnClickListener { seekForward() }
            binding.btnVolver.setOnClickListener {
                mediaPlayer?.let { mp ->
                    mp.seekTo(0)
                    mp.start()
                    isPrepared = true
                    startUpdatingUI()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
                it.visibility = View.GONE
            }

            // SeekBar
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser && isPrepared) mediaPlayer?.seekTo(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })

            // Comprobar permisos y preparar MediaPlayer
            checkPermissionAndSetup()
        }
    }

    /**
     * Variable para temporizador de ocultar controles
     * */
    private val ocultarHandler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * Runnable para ocultar controles tras 2 segundos*/
    private val ocultarRunnable = Runnable {
        if(controlesVisibles) alternarControles()
    }

    /**
     * Método para reiniciar el temporizador de ocultar controles
     * */
    private fun reinciarTemporizadorOcultar(){
        ocultarHandler.removeCallbacks(ocultarRunnable)
        ocultarHandler.postDelayed(ocultarRunnable, 2000)
    }

    /**
     * Método para alternar la visibilidad de los controles
     * */
    private fun alternarControles(){
        val bloqueControles = binding.playerControlsBlock

        if(controlesVisibles){
            // Ocultamos los controles al deslizar hacia abajo
            bloqueControles.animate()
                .translationY(bloqueControles.height.toFloat())
                .alpha(0f)
                .setDuration(300)
                .withEndAction { bloqueControles.visibility = View.GONE }
        }else{
            // Mostramos los controles al deslizar hacia arriba
            bloqueControles.visibility = View.VISIBLE
            bloqueControles.animate()
                .translationY(0f)
                .alpha(1f)
                .setDuration(300)
                .start()

            // Si acabamos de mostrar los controles, hacemos que se oculten tras 3 segundos
            reinciarTemporizadorOcultar()
        }
        controlesVisibles = !controlesVisibles
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun checkPermissionAndSetup() {
        val path = libro?.uriAudio ?: return
        if (path.startsWith(requireContext().filesDir.absolutePath) ||
            ContextCompat.checkSelfPermission(requireContext(), getRequiredPermission()) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            setupMediaPlayer()
        } else {
            requestPermissionLauncher.launch(getRequiredPermission())
        }
    }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_AUDIO
        else
            Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private fun releaseMediaPlayer() {
        isPrepared = false
        stopUpdatingUI()
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            release()
        }
        mediaPlayer = null
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupMediaPlayer() {
        val audioPath = libro?.uriAudio ?: return
        if (mediaPlayer != null) releaseMediaPlayer()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioPath)

                setOnPreparedListener { mp ->
                    isPrepared = true
                    libro?.totalPagina = mp.duration
                    _binding?.let { binding ->
                        binding.seekBar.max = mp.duration
                        val pos = libro?.ultimaPosicion ?: 0
                        mp.seekTo(pos)
                        binding.tvTiempoActual.text = formatTime(pos)
                        binding.tvTiempoTotal.text = formatTime(mp.duration)
                    }

                    // Al abrir, si el libro estaba en COMPLETADO, pasa a EN_PROGRESO
                    libro?.let { currentLibro ->
                        if (currentLibro.estado == EstadoLibro.COMPLETADO) {
                            currentLibro.estado = EstadoLibro.EN_PROGRESO
                            lifecycleScope.launch {
                                Log.d(
                                    "AudioPlayer",
                                    "Cambiando a EN_PROGRESO: ${currentLibro.titulo}, estado=${currentLibro.estado}"
                                )
                                repository.actualizaLibros(currentLibro)
                            }
                        }
                    }

                    mp.start()
                    _binding?.btnPlayPause?.setImageResource(R.drawable.ic_pause)
                    startUpdatingUI()
                }

                setOnCompletionListener {
                    isPrepared = false
                    _binding?.btnPlayPause?.setImageResource(R.drawable.ic_play)
                    stopUpdatingUI()
                    _binding?.btnAtras?.visibility = View.VISIBLE

                    // Marcamos COMPLETADO al terminar
                    libro?.let { currentLibro ->
                        currentLibro.estado = EstadoLibro.COMPLETADO
                        currentLibro.leido = true
                        lifecycleScope.launch {
                            Log.d(
                                "AudioPlayer",
                                "Marcando COMPLETADO: ${currentLibro.titulo}," +
                                        " estado=${currentLibro.estado}"
                            )
                            repository.actualizaLibros(currentLibro)
                        }
                    }
                }

                prepareAsync()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun togglePlayPause() {
        val mp = mediaPlayer ?: return
        if (!isPrepared) return
        if (mp.isPlaying) {
            mp.pause()
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            stopUpdatingUI()
        } else {
            mp.start()
            binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            startUpdatingUI()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startUpdatingUI() {
        updateJob?.cancel()
        updateJob = viewLifecycleOwner.lifecycleScope.launch {
            while (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.let { mp ->
                    _binding?.let { b ->
                        val currentPos = mp.currentPosition
                        b.seekBar.progress = currentPos
                        b.tvTiempoActual.text = formatTime(currentPos)
                        libro?.ultimaPosicion = currentPos
                        libro?.posicionMs = currentPos.toLong()
                    }
                }
                libro?.let { currentLibro ->
                    lifecycleScope.launch {
                        try {
                            repository.actualizaLibros(currentLibro)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    private fun stopUpdatingUI() {
        updateJob?.cancel()
    }

    private fun stopAudio() {
        if (!isPrepared) return
        mediaPlayer?.pause()
        mediaPlayer?.seekTo(0)
        binding.btnPlayPause.setImageResource(R.drawable.ic_play)
        stopUpdatingUI()
    }

    private fun seekBackward() {
        if (!isPrepared) return
        val newPos = (mediaPlayer?.currentPosition ?: 0) - 10000
        mediaPlayer?.seekTo(newPos.coerceAtLeast(0))
    }

    private fun seekForward() {
        if (!isPrepared) return
        val newPos = (mediaPlayer?.currentPosition ?: 0) + 10000
        mediaPlayer?.seekTo(newPos.coerceAtMost(mediaPlayer?.duration ?: 0))
    }

    private fun formatTime(ms: Int): String {
        val minutos = (ms / 1000) / 60
        val segundos = (ms / 1000) % 60
        return String.format("%02d:%02d", minutos, segundos)
    }

    fun obtenerPortadaAudio(rutaAudio: String, ancho: Int, alto: Int): Bitmap? {
        return try {
            val mmr = MediaMetadataRetriever()
            mmr.setDataSource(rutaAudio)
            val data = mmr.embeddedPicture
            if (data != null) {
                val original = BitmapFactory.decodeByteArray(data, 0, data.size)
                original.scale(ancho, alto)
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onDestroyView() {
        val activity = activity as? MainActivity
        activity?.binding?.ivMenu?.visibility = View.VISIBLE
        activity?.binding?.layoutColecciones?.visibility = View.VISIBLE
        activity?.binding?.tvTitulo?.text = getString(R.string.app_name)

        mediaPlayer?.let {
            if (isPrepared) libro?.ultimaPosicion = it.currentPosition
            lifecycleScope.launch {
                libro?.let { currentLibro ->
                    repository.actualizaLibros(currentLibro)
                }
            }
            it.release()
        }
        mediaPlayer = null
        isPrepared = false
        stopUpdatingUI()
        _binding = null
        super.onDestroyView()
    }
}