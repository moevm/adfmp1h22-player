package com.github.moevm.adfmp1h22_player.SQLite

object SQLiteContract {
    object AllStationsTable{
        const val TABLE_NAME = "AllStations"
        const val TABLE_NAME_NEW = "AllStations_new"
        const val COLUMN_ID = "id"
        const val COLUMN_CHANGEUUID = "changeuuid"
        const val COLUMN_NAME = "name"
        const val COLUMN_STREAM_URL = "streamurl"
        const val COLUMN_FAVICON = "favicon"
        const val COLUMN_FAVICON_DATE = "faviconDate"
    }
    object AddedStationsTable{
        const val TABLE_NAME = "AddedStations"
        const val COLUMN_ID = "id"
        const val COLUMN_CHANGEUUID = "changeuuid"
        const val COLUMN_NAME = "name"
        const val COLUMN_STREAM_URL = "streamurl"
        const val COLUMN_FAVICON = "favicon"
        const val COLUMN_FAVICON_DATE = "faviconDate"
    }
    object HistoryTable{}
    object RecordingsTable {
        const val TABLE_NAME = "RecordingsTable"
        const val COLUMN_UUID = "uuid"
        const val COLUMN_TRACK_ORIGTITLE = "track_origtitle"
        const val COLUMN_TRACK_ARTIST = "track_artist"
        const val COLUMN_TRACK_TITLE = "track_title"

        const val COLUMN_TIMESTAMP = "timestamp"
        // Java timestamp: ms from Epoch

        const val COLUMN_MIME_TYPE = "mime"

        const val COLUMN_STATE = "state"
        // 0: recording
        // 1: done
        // 2: saved

        // TODO: duration
        // TODO: station info
    }
}
