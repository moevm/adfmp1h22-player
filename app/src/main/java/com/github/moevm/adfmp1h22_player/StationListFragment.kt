package com.github.moevm.adfmp1h22_player

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_station_list.*

class StationListFragment : Fragment(R.layout.fragment_station_list) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        add_fab.setOnClickListener {
            Toast.makeText(context, "No Add station activity yet",
                           Toast.LENGTH_LONG)
                .show()
        }
    }
}