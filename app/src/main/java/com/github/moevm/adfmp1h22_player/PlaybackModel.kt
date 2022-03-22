package com.github.moevm.adfmp1h22_player

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PlaybackModel : ViewModel() {
    val station = MutableLiveData<Station>()
    val metadata = MutableLiveData<TrackMetaData>()
    val state = MutableLiveData<PlaybackState>(PlaybackState.STOPPED)
}
