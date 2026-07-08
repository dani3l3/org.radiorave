package org.radiorave

import android.content.ComponentName
import android.os.Handler
import android.util.Log
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.MediaSession
import org.radiorave.R
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException


class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val mClient = OkHttpClient()

    private lateinit var controllerFuture: ListenableFuture<MediaController>
    private lateinit var controller: MediaController


    // Create your Player and MediaSession in the onCreate lifecycle event
    override fun onCreate() {

        // Log.d("radiorave-debug" ,"Service starting (onCreate)...")

        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).setId("RadioRave_audio_session").build()




        fetchRadioStatus(mClient)
        // handler for status and track changes
        val mStatusHandler = Handler()
        val mStatusRunnable = object : Runnable {
            override fun run() {
                // Log.d("radiorave-debug","mStatusRunnable running")

                fetchRadioStatus(mClient)
                // Repeat this the same runnable code block again another 60 seconds
                mStatusHandler.postDelayed(this, 30000)
            }
        }
        // Start the initial runnable task by posting through the handler
        mStatusHandler.postDelayed(mStatusRunnable, 30000)



    }

    // Remember to release the player and media session in onDestroy
    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession




    public fun fetchRadioStatus(mClient: OkHttpClient ) {

        val request = Request.Builder()
            .url(getString(R.string.status_url))
            .build()

        mClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                // do nothing in production, we'll try next time...
                //Log.e("radiorave-err",e.printStackTrace().toString())

            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        // do nothing in production, we'll try next time... this can be useful for debugging
                        //throw IOException("Unexpected code $response")
                        //Log.e("radiorave-err","Unexpected code $response")
                    } else {

                        try {
                            val jCurrentTtrack = JSONObject(response.body!!.string()).getJSONObject("now_playing").getJSONObject("song")

                            if (jCurrentTtrack.has("text")) {

                                val RadioRavePrefs = RadioRavePrefs(applicationContext);
                                if (RadioRavePrefs.getCurrentTrackTitle() != jCurrentTtrack.getString("text")) {
                                    //Log.d("radiorave-debug" ,"updating track preferences...")

                                    RadioRavePrefs.saveCurrentTrackInfo(
                                        jCurrentTtrack.getString("art"),
                                        jCurrentTtrack.getString("text")
                                    )

                                } else {
                                    //Log.d("radiorave-debug" ,"current track has NOT changed from previous status polling")
                                }

                            } else {
                                //Log.w("radiorave-warn" ,"current track does NOT have a title!!! You should never see this message...")
                            }

                        } catch (e: JSONException) {
                            //Log.e("radiorave-err" ,"JSON parsing error from the status API")
                        }
                    }
                }
            }
        })

    }





}