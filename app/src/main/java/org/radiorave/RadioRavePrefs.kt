package org.radiorave

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit


class RadioRavePrefs(context: Context) {

    public val RadioRavePrefs: SharedPreferences = context.getSharedPreferences("radiorave-Prefs", MODE_PRIVATE)

    public fun saveCurrentTrackInfo(artworkUrl: String, currentTrack: String) {
        RadioRavePrefs.edit {
            putString("radiorave-trackName", currentTrack)
            putString("radiorave-artworkUrl", artworkUrl)
        }
    }

    public fun getCurrentTrackTitle(): String? {
        val trackName = RadioRavePrefs.getString("radiorave-trackName", "loading...")
        return trackName
    }

    public fun getCurrentTrackArtworkUrl(): String? {
        val artworkUrl = RadioRavePrefs.getString("radiorave-artworkUrl", "UNKNOWN")
        return artworkUrl
    }

}
