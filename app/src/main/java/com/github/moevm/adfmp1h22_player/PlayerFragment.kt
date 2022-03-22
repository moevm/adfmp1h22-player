package com.github.moevm.adfmp1h22_player

import androidx.fragment.app.activityViewModels

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_player.*

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var mState: PlaybackState = PlaybackState.STOPPED

    var onStopListener: (() -> Unit)? = null
    var queryState: (() -> PlaybackState)? = null
    var setState: ((PlaybackState) -> Unit)? = null

    private val playbackState: PlaybackModel by activityViewModels()

    fun updateUiState() {
        when (mState) {
            PlaybackState.STOPPED -> ib_playpause.run {
                setImageResource(R.drawable.ic_play_64)
                setEnabled(false)
            }
            PlaybackState.LOADING -> {} // TODO
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

    fun setPlayingState(value: PlaybackState) {
        if (value == mState) {
            return
        }
        mState = value
        // TODO: setState
        updateUiState()
        if (mState == PlaybackState.STOPPED) {
            onStopListener?.let { it() }
        }
    }

    fun setMetaData(s: String) {
        tv_tracktitle.setText(s)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ib_savetracks.setOnClickListener {
            context?.run {
                startActivity(Intent(context, SaveTracksActivity::class.java))
            }
        }

        ib_playpause.setOnClickListener {
            setPlayingState(when (mState) {
                PlaybackState.PLAYING -> PlaybackState.PAUSED
                else -> PlaybackState.PLAYING
            })
        }

        ib_stop.setOnClickListener {
            setPlayingState(PlaybackState.STOPPED)
        }

        playbackState.metadata.observe(this, { s -> setMetaData(s) })

        updateUiState()
    }

    override fun onResume() {
        super.onResume()
        val state = queryState?.let { it() } ?: PlaybackState.STOPPED
        setPlayingState(state)
    }
}
