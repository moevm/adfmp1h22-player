package com.github.moevm.adfmp1h22_player

import android.content.*
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.util.*

class PlayerBroadcastReceiver : BroadcastReceiver() {
    companion object {
        val TAG = "BroadcastReceiver"

        val ACTION_STOP = "com.github.moevm.adfmp1h22_player.ACTION_STOP"
        val ACTION_RESUME = "com.github.moevm.adfmp1h22_player.ACTION_RESUME"
        val ACTION_PAUSE = "com.github.moevm.adfmp1h22_player.ACTION_PAUSE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, intent.action + "Intent Received")
        when (intent.action) {
            ACTION_STOP -> {
                Log.d(TAG, intent.action.toString())
                val binder = peekService(
                    context,
                    Intent(context, PlayerService::class.java)
                ) as PlayerService.PlayerServiceBinder
                val srv = binder.service

                srv.stopPlayback()
            }
            ACTION_RESUME -> {
                Log.d(TAG, intent.action.toString())
                val binder = peekService(
                    context,
                    Intent(context, PlayerService::class.java)
                ) as PlayerService.PlayerServiceBinder
                val srv = binder.service

                srv.resumePlayback()
            }
            ACTION_PAUSE -> {
                Log.d(TAG, intent.action.toString())
                val binder = peekService(
                    context,
                    Intent(context, PlayerService::class.java)
                ) as PlayerService.PlayerServiceBinder
                val srv = binder.service

                srv.pausePlayback()
            }
            else -> {
                Log.d(TAG, "Cannot parse action")
            }
        }
    }
}