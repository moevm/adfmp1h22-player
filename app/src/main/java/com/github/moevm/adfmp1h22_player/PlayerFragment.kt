package com.github.moevm.adfmp1h22_player

import androidx.fragment.app.activityViewModels

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_player.*

class PlayerFragment : Fragment(R.layout.fragment_player) {

    private var mState: PlaybackState = PlaybackState.STOPPED

    var onStopRequested: (() -> Unit)? = null

    private val playbackModel: PlaybackModel by activityViewModels()

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
        updateUiState()
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
            onStopRequested?.let { it() }
        }

        playbackModel.metadata.observe(viewLifecycleOwner) { s ->
            tv_tracktitle.setText(s)
        }
        playbackModel.state.observe(viewLifecycleOwner) { stt ->
            setPlayingState(stt)
        }

        updateUiState()
    }

    override fun onResume() {
        super.onResume()
        val state = playbackModel.state.value ?: PlaybackState.STOPPED
        setPlayingState(state)
    }
}
