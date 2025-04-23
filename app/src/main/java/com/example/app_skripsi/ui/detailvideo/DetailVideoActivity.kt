package com.example.app_skripsi.ui.detailvideo

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import com.example.app_skripsi.R
import com.example.app_skripsi.data.model.VideoModel
import com.example.app_skripsi.databinding.ActivityDetailVideoBinding

class DetailVideoActivity : AppCompatActivity() {
    private var _binding: ActivityDetailVideoBinding? = null
    private val binding get() = _binding!!

    private var player: ExoPlayer? = null
    private var playWhenReady = true
    private var currentPosition = 0L
    private var playbackPosition = 0L

    private var videoModel: VideoModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        _binding = ActivityDetailVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left+v.paddingLeft, systemBars.top + v.paddingTop,
                systemBars.right +v.paddingRight, systemBars.bottom+v.paddingBottom)
            insets
        }

        // Hide system UI
        hideSystemUI()

        // Get video data from intent
        videoModel = intent.getParcelableExtra("VIDEO_MODEL")

        if (videoModel == null) {
            Toast.makeText(this, "Video tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set video data to views
        binding.tvDetailTitle.text = videoModel?.title
        binding.tvDescription.text = videoModel?.description

        // Show source if available
        if (!videoModel?.sourceUrl.isNullOrEmpty() && videoModel?.hasCopyright == true) {
            binding.tvSource.visibility = View.VISIBLE
            binding.tvSource.text = "Sumber: ${videoModel?.sourceUrl}"
        } else {
            binding.tvSource.visibility = View.GONE
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun initializePlayer() {
        val videoUrl = videoModel?.videoUrl

        if (videoUrl.isNullOrEmpty()) {
            Toast.makeText(this, "URL video tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }

        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                binding.playerView.player = exoPlayer

                val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                exoPlayer.setMediaItem(mediaItem)

                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(playbackPosition)
                exoPlayer.prepare()

                // Add player listener
                exoPlayer.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            binding.progressBarLoading.visibility = View.GONE
                        } else if (playbackState == Player.STATE_BUFFERING) {
                            binding.progressBarLoading.visibility = View.VISIBLE
                        }
                    }
                })
            }
    }

    private fun releasePlayer() {
        player?.let { exoPlayer ->
            playbackPosition = exoPlayer.currentPosition
            playWhenReady = exoPlayer.playWhenReady
            exoPlayer.release()
        }
        player = null
    }

    @OptIn(UnstableApi::class)
    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            initializePlayer()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24 || player == null) {
            initializePlayer()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
        }
    }

    @OptIn(UnstableApi::class)
    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
    }

    private fun hideSystemUI() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}