package com.github.moevm.adfmp1h22_player

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_player.*

class PlayerFragment : Fragment(R.layout.fragment_player) {

    enum class PlaybackState {
        STOPPED, PAUSED, PLAYING,
    }

    private var state: PlaybackState = PlaybackState.PAUSED

    var onStopListener: (() -> Unit)? = null

    fun setPlayingState(value: PlaybackState) {
        state = value
        when (value) {
            PlaybackState.STOPPED -> ib_playpause.run {
                setImageResource(R.drawable.ic_play_64)
                setEnabled(false)
            }
            PlaybackState.PAUSED -> ib_playpause.run {
                setImageResource(R.drawable.ic_play_64)
                setEnabled(true)
            }
            PlaybackState.PLAYING -> ib_playpause.run {
                setImageResource(R.drawable.ic_pause_64)
                setEnabled(true)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ib_savetracks.setOnClickListener {
            context?.run {
                startActivity(Intent(context, SaveTracksActivity::class.java))
            }
        }

        ib_playpause.setOnClickListener {
            setPlayingState(when (state) {
                PlaybackState.PLAYING -> PlaybackState.PAUSED
                else -> PlaybackState.PLAYING
            })
        }
        setPlayingState(PlaybackState.PAUSED)

        ib_stop.setOnClickListener {
            // TODO: set playback state to STOPPED
            onStopListener?.let { it() }
        }
    }
}
