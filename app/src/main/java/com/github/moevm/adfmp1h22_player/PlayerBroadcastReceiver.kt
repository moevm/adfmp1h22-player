package com.github.moevm.adfmp1h22_player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PlayerBroadcastReceiver : BroadcastReceiver() {
    companion object{
        val TAG = "BroadcastReceiver"

        val ACTION_STOP = "com.github.moevm.adfmp1h22_player.ACTION_STOP"
        val ACTION_RESUME = "com.github.moevm.adfmp1h22_player.ACTION_RESUME"
        val ACTION_PAUSE = "com.github.moevm.adfmp1h22_player.ACTION_PAUSE"
    }

    override fun onReceive(context: Context?, intent: Intent?){
        Log.d(TAG,"Intent Received")
        if (intent != null){
            when(intent.action){
                ACTION_STOP -> {
                    Log.d(TAG, intent.action.toString())

                }
                else -> {

                }
            }
        }
    }
}