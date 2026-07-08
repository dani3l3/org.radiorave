package org.radiorave

import android.content.ComponentName
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_BROWSABLE
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import coil.ImageLoader
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import coil.load
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import org.radiorave.databinding.ActivityMainBinding
import org.woheller69.freeDroidWarn.FreeDroidWarn


//class MainActivity : AppCompatActivity(), Player.Listener {
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private lateinit var controller: MediaController
    private lateinit var sessionToken: SessionToken





    override fun onCreate(savedInstanceState: Bundle?) {
        // SPLASH SCREEN
        val splashScreen = installSplashScreen()

        // REAL INIT
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setTransparentStatusBar()

        FreeDroidWarn.showWarningOnUpgrade(this, 1);


        // handler for status and track changes
        updateTrackMetadata(getCurrentTrackFromPrefs(), getCurrentArtworkFromPrefs())
        val mStatusHandler = Handler()
        val mStatusRunnable = object : Runnable {
            override fun run() {
                //Log.d("radiorave-debug","mStatusPrefsRunnable running")
                updateTrackMetadata(getCurrentTrackFromPrefs(), getCurrentArtworkFromPrefs())
                // Repeat this the same runnable code block again another 60 seconds
                mStatusHandler.postDelayed(this, 10000)
            }
        }
        // Start the initial runnable task by posting through the handler
        mStatusHandler.postDelayed(mStatusRunnable, 10000)

        // app logic
        loadBackgroundGif()

        // button click listener
        binding.btnPlayPause.setOnClickListener {
            controller.let { player ->
                player.let {
                    if (player.playbackState == Player.STATE_READY && player.playWhenReady) {
                        pauseMedia()
                    } else {
                        resumeMedia()
                    }
                }
            }
        }



    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()

        // TV disable browsable links
        if (isTelevision()) {
            binding.homeTextRow3.isVisible = false
            binding.homeTextRow3.isClickable = false
            binding.homeTextRow3.focusable = 0
            binding.tvSoundExample.focusable = 0
        }

        sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                if (controllerFuture.isDone) {
                    controller = controllerFuture.get()
                    initController()
                }
            }, MoreExecutors.directExecutor()
        )
    }


    private fun createMediaItem(): MediaItem {
        val media = MediaItem.Builder()
            .setMediaId("RadioRave_audio_session")
            .setUri(resources.getString(R.string.stream_url))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtist("Radio Rave")
                    .setTitle(getCurrentTrackFromPrefs())
                    .setArtworkUri(getCurrentArtworkFromPrefs()!!.toUri())
                    .build()
            )
            .build()

        return media
    }

    private fun playMedia(url: String) {

        val media = createMediaItem()

        if (controller.mediaMetadata.title.toString() != getCurrentTrackFromPrefs()) {
            controller.replaceMediaItem(0, media)
        }

        if (!controller.isPlaying()) {
            //log("setMediaItem in playMedia")
            // this method is SLOW and causes a playback hiccup; use only if/when strictly necessary
            controller.setMediaItem(media)
            controller.prepare()
            controller.play()
        } else {
            binding.btnPlayPause.setBackgroundResource(R.drawable.ic_pause)
        }
    }

    private fun pauseMedia() {
        binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play)
        controller.pause()
    }

    private fun resumeMedia() {
        binding.btnPlayPause.setBackgroundResource(R.drawable.ic_pause)
        controller.play()
    }

    private fun stopMedia() {
        controller.stop()
        controller.playWhenReady = false
        MediaController.releaseFuture(controllerFuture)
    }


    @androidx.annotation.OptIn(UnstableApi::class)
    private fun initController() {
        controller.addListener(object : Player.Listener {

            @Deprecated("Deprecated in Java")
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY && controller.playWhenReady) {
                    binding.btnPlayPause.setBackgroundResource(R.drawable.ic_pause)
                } else {
                    binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play)
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
                super.onMediaMetadataChanged(mediaMetadata)
                //log("onMediaMetadataChanged=$mediaMetadata")
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                //log("onIsPlayingChanged=$isPlaying")
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                //log("onPlaybackStateChanged=${getStateName(playbackState)}")

                if (Player.STATE_BUFFERING == playbackState) {
                    //log("STATE buffering")
                    binding.btnPlayPause.setBackgroundResource(R.drawable.ic_downloading)
                    binding.btnPlayPause.isClickable = false
                }

                if (Player.STATE_READY == playbackState) {
                    //log("STATE ready")
                    binding.btnPlayPause.setBackgroundResource(R.drawable.ic_pause)
                    binding.btnPlayPause.isClickable = true
                }

                if (Player.STATE_IDLE == playbackState || Player.STATE_ENDED == playbackState) {
                    //log("STATE idle or ended")
                    binding.btnPlayPause.setBackgroundResource(R.drawable.ic_play)
                    binding.tvSoundExample.text = ""
                    binding.btnPlayPause.isClickable = true
                }

            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
                //log("onPlayerError=${error.stackTraceToString()}")
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                super.onPlayerErrorChanged(error)
                //log("onPlayerErrorChanged=${error?.stackTraceToString()}")
            }
        })

        // FIRE!!!
        playMedia(getString(R.string.stream_url))

    }


    private fun getStateName(i: Int): String? {
        return when (i) {
            1 -> "STATE_IDLE"
            2 -> "STATE_BUFFERING"
            3 -> "STATE_READY"
            4 -> "STATE_ENDED"
            else -> null
        }
    }


    private fun updateTitleTextView(s: String?) {
        this@MainActivity.runOnUiThread {
            val titleView = findViewById<View?>(R.id.tv_sound_example) as TextView

            if (titleView.text != s) {
                titleView.invalidate()
                titleView.text = s
                titleView.isSelected = true
            }
        }
    }



    private fun updateTrackMetadata(currentTrack: String?, artwork: String?) {

        // Log.d("radiorave-debug" , "updateTrackMetadata track:$currentTrack")

        this@MainActivity.runOnUiThread {
            val titleView = findViewById<View?>(R.id.tv_sound_example) as TextView
            if (titleView.text != currentTrack) {
                updateTitleTextView(currentTrack)

                if (artwork != "UNKNOWN") {
                    loadArtwork(artwork)
                }

                // duplicate shit but we need the controller here... TODO refactor
                val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
                controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
                controllerFuture.addListener(
                    {
                        if (controllerFuture.isDone) {
                            controller = controllerFuture.get()
                            initController()

                            // updates mediasession
                            controller.replaceMediaItem(0, createMediaItem())
                        }
                    }, MoreExecutors.directExecutor()
                )
            }
        }


    }

    private fun log(message: String) {
        Log.d("radiorave-debug", message)
    }


    private fun getCurrentTrackFromPrefs(): String? {
        val RadioRavePrefs = RadioRavePrefs(applicationContext);
        val currentTrack = RadioRavePrefs.getCurrentTrackTitle()
        return currentTrack
    }
    private fun getCurrentArtworkFromPrefs(): String? {
        val RadioRavePrefs = RadioRavePrefs(applicationContext);
        val artwork = RadioRavePrefs.getCurrentTrackArtworkUrl()
        return artwork
    }


    private fun loadBackgroundGif() {
        val imageLoader = ImageLoader.Builder(this)
            .components {
                if (SDK_INT >= 28) {
                    add(ImageDecoderDecoder.Factory())
                } else {
                    add(GifDecoder.Factory())
                }
            }
            .crossfade(enable = true)
            .build()

        binding.videoViewFullscreen.load(data = R.drawable.background, imageLoader = imageLoader)
    }


    private fun loadArtwork(artworkUrl: String?) {
        this@MainActivity.runOnUiThread {
            val imageLoader = ImageLoader.Builder(this)
                .components {
                    if (SDK_INT >= 28) {
                        add(ImageDecoderDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .crossfade(enable = true)
                .build()

            binding.imgSound.load(data = R.drawable.logo_radiorave, imageLoader = imageLoader)
        }
    }



    private fun isTelevision(): Boolean {
        val uiMode: Int = applicationContext.resources.configuration.uiMode
        return (uiMode and Configuration.UI_MODE_TYPE_MASK) === Configuration.UI_MODE_TYPE_TELEVISION
    }



}
