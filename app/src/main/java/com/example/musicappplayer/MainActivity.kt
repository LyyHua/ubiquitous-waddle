package com.example.musicappplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.musicappplayer.ui.theme.MusicAppPlayerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var mediaPlayer: android.media.MediaPlayer
    private var isPlaying by mutableStateOf(false)
    private var currentPosition by mutableStateOf(0)
    private var duration by mutableStateOf(0)
    private var fileName by mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        mediaPlayer = android.media.MediaPlayer() // Initialize mediaPlayer here
        setContent {
            MusicAppPlayerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainContent(
                        modifier = Modifier.padding(innerPadding),
                        onPickFile = { pickFileLauncher.launch("audio/*") },
                        isPlaying = isPlaying,
                        currentPosition = currentPosition,
                        duration = duration,
                        fileName = fileName,
                        onPlayPause = { togglePlayPause() },
                        onSeek = { seekTo(it) },
                        mediaPlayer = mediaPlayer
                    )
                }
            }
        }

        // Request storage permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            playAudio(it)
        }
    }

    private fun playAudio(uri: Uri) {
        try {
            mediaPlayer.reset() // Reset the media player before setting a new data source
            mediaPlayer.setDataSource(this, uri)
            mediaPlayer.prepare()
            mediaPlayer.start()
            isPlaying = true
            duration = mediaPlayer.duration
            fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "Unknown"
            mediaPlayer.setOnCompletionListener {
                isPlaying = false
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error playing audio", e)
        }
    }

    private fun togglePlayPause() {
        if (isPlaying) {
            mediaPlayer.pause()
        } else {
            mediaPlayer.start()
        }
        isPlaying = !isPlaying
    }

    private fun seekTo(position: Int) {
        mediaPlayer.seekTo(position)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayer.isInitialized) {
            mediaPlayer.release()
        }
    }
}

@Composable
fun MainContent(
    modifier: Modifier = Modifier,
    onPickFile: () -> Unit,
    isPlaying: Boolean,
    currentPosition: Int,
    duration: Int,
    fileName: String,
    onPlayPause: () -> Unit,
    onSeek: (Int) -> Unit,
    mediaPlayer: android.media.MediaPlayer
) {
    val scope = rememberCoroutineScope()
    var currentPos by remember { mutableStateOf(currentPosition) }

    // Continuously update `currentPos` while audio is playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            scope.launch {
                while (isPlaying) {
                    currentPos = mediaPlayer.currentPosition
                    delay(500) // Update every 500ms for smoothness
                }
            }
        }
    }

    // Calculate progress as a percentage
    val progress = if (duration > 0) currentPos / duration.toFloat() else 0f

    Column(modifier = modifier.padding(16.dp)) {
        Text(text = "File: $fileName")
        Spacer(modifier = Modifier.height(16.dp))
        SeekBar(
            progress = progress,
            onSeek = onSeek,
            duration = duration // Pass duration for scaling the seek position
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Text(text = formatTime(currentPos))
            Spacer(modifier = Modifier.weight(1f))
            Text(text = formatTime(duration))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row {
            Button(onClick = onPickFile) {
                Text(text = "Choose and Play MP3")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(onClick = onPlayPause) {
                Text(text = if (isPlaying) "Pause" else "Play")
            }
        }
    }
}

@Composable
fun SeekBar(progress: Float, onSeek: (Int) -> Unit, duration: Int) {
    Slider(
        value = progress,
        onValueChange = { newProgress ->
            onSeek((newProgress * duration).toInt()) // Scale progress with duration
        },
        valueRange = 0f..1f, // Keep range from 0 to 1
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    )
}


@SuppressLint("DefaultLocale")
fun formatTime(milliseconds: Int): String {
    val minutes = (milliseconds / 1000) / 60
    val seconds = (milliseconds / 1000) % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Preview(showBackground = true)
@Composable
fun MainContentPreview() {
    MusicAppPlayerTheme {
        MainContent(
            onPickFile = {},
            isPlaying = false,
            currentPosition = 0,
            duration = 0,
            fileName = "Sample.mp3",
            onPlayPause = {},
            onSeek = {},
            mediaPlayer = android.media.MediaPlayer()
        )
    }
}