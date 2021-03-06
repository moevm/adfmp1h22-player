package com.github.moevm.adfmp1h22_player

import androidx.fragment.app.activityViewModels

import android.util.Log
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_player.*

class PlayerFragment : Fragment(R.layout.fragment_player) {

    var onStopRequested: (() -> Unit)? = null
    var onPauseRequested: (() -> Unit)? = null
    var onResumeRequested: (() -> Unit)? = null

    private val playbackModel: PlaybackModel by activityViewModels()

    companion object {
        val TAG = "PlayerFragment"
    }

    fun updateUiState() {
        Log.d(TAG, "updateUiState")
        when (playbackModel.state.value) {
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
            else -> throw IllegalArgumentException("Invalid state")
        }
    }

    private fun setStrings(title: String, artist: String?) {
        tv_tracktitle.setText(title)
        if (artist == null) {
            tv_trackartist.setVisibility(View.INVISIBLE)
        } else {
            tv_trackartist.setText(artist)
            tv_trackartist.setVisibility(View.VISIBLE)
        }
    }

    private fun setStringsStation(station: Station?) {
        if (station != null) {
            setStrings(station.name, null)
        } else {
            setStrings("", null)
        }
    }

    private fun setStringsMetaData(m: TrackMetaData) {
        setStrings(m.title, m.artist)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ib_savetracks.setOnClickListener {
            context?.run {
                startActivity(Intent(context, SaveTracksActivity::class.java))
            }
        }

        ib_playpause.setOnClickListener {
            when (playbackModel.state.value) {
                PlaybackState.PLAYING -> onPauseRequested?.let { it() }
                PlaybackState.PAUSED -> onResumeRequested?.let { it() }
                else -> {
                    Log.e(TAG, "Play/pause button was clicked in unusual state")
                }
            }
        }

        ib_stop.setOnClickListener {
            onStopRequested?.let { it() }
        }

        playbackModel.station.observe(viewLifecycleOwner) { station ->
            setStringsStation(station)
        }
        playbackModel.metadata.observe(viewLifecycleOwner) { m ->
            if (m != null) {
                setStringsMetaData(m)
            } else {
                setStringsStation(playbackModel.station.value)
            }
        }
        playbackModel.state.observe(viewLifecycleOwner) {
            updateUiState()
        }

        // TODO: remove?
        updateUiState()
    }

    override fun onResume() {
        super.onResume()
        // TODO: handled by lifecycle observer?
        updateUiState()
    }
}
