package com.github.moevm.adfmp1h22_player

enum class PlaybackState {
    STOPPED, LOADING, PLAYING, PAUSED,
}

//[MainActivity]
//    |   [PlayerService]
//    |          |
//    v          v
// STOPPED -> LOADING -> (PLAYING <-> PAUSED)
//    ^          ^                 |
// [Stop]     [Switch]             |
//    `----------'-----------------'
//
// Basically, any transition except STOPPED -> (PLAYING|PAUSED) is allowed
//
