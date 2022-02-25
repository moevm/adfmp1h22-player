package com.github.moevm.adfmp1h22_player

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_player.*

class PlayerFragment : Fragment(R.layout.fragment_player) {
    var isPlaying: Boolean = false

    var onStopListener: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ib_savetracks.setOnClickListener {
            context?.run {
                startActivity(Intent(context, SaveTracksActivity::class.java))
            }
        }

        ib_playpause.setOnClickListener {
            isPlaying = !isPlaying
            // TODO: toggle isPlaying, replace icon
        }

        ib_stop.setOnClickListener {
            onStopListener?.let { it() }
        }
    }
}
