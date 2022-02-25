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

        val a = StationListAdapter {
            Toast.makeText(context, "Station: ${it.name}",
                           Toast.LENGTH_SHORT)
                .show()
        }

        station_list.adapter = a
        val l = mutableListOf(
            Station("828a9ec6-9d30-40e9-8c76-fbded56fdc94", "Europa Plus", "http://liveam.tv/img/2494.jpg"),
            Station("2f0c87d1-26e3-4201-a28f-c2e68da578f6", "Вести ФМ (Vesti FM)", ""),
            Station("219fc935-26c5-11e8-91bf-52543be04c81", "Шансон Радио", "https://www.radiobells.com/stations/chanson.jpg"),
            Station("299cfc26-0705-485e-a81a-572e0652d7a5", "ROCK FM", "https://lh3.googleusercontent.com/D3taObR7tfyhwDFY40VS8DIVri7iif5RuzI9C-mXxRwF41vGZ_dO_n6MWM57P-mZczFC=w300"),
            Station("74ba2dcb-c6ad-405e-aa2c-d09226e73920", "Радио «Комсомольская Правда» | КП Россия", "https://www.kp.ru/favicon.ico"),
            Station("352d202f-e753-47fb-aa94-5dd9230fce90", "Ретро FM", "http://retrofm.ru/favicon.ico"),
            Station("c254c600-7e58-443f-9257-e130290e01c7", "Echo Moskva HD", "https://echo.msk.ru/i/icons/apple-icon-120x120.png"),
            Station("fb02bc59-c985-4dce-af18-d3b40fcfe29b", "DFM RUSSIAN DANCE", ""),
            Station("6b54b9a1-2001-11e8-a334-52543be04c81", "дорожное радио (Dorognoe Radio)", "https://dorognoe.ru/thumb/og_image_600x315/sharing_image/2016/ff/91/57cd63d58643c_sharind_dorognoe.jpg"),
            Station("850a3029-7eab-4a61-bfa2-ebe72a3a245d", "Дискотека СССР", "https://cdn2.101.ru/vardata/modules/channel/image/c34932ae363a2b1386c0136d403a2274.png"),
        )
        a.submitList(l)
    }
}
