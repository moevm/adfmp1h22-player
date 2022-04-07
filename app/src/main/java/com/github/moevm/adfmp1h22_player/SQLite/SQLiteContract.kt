package com.github.moevm.adfmp1h22_player.SQLite

object SQLiteContract {
    object AllStationsTable{
        const val TABLE_NAME = "AllStations"
        const val TABLE_NAME_NEW = "AllStations_new"
        const val COLUMN_ID = "id"
        const val COLUMN_CHANGEUUID = "changeuuid"
        const val COLUMN_STATIONUUID = "stationuuid"
        const val COLUMN_NAME = "name"
        const val COLUMN_FAVICON = "favicon"
        const val COLUMN_FAVICON_DATE = "faviconDate"
        const val COLUMN_STREAMURL = "streamUrl"
    }
    object AddedStationsTable{
        const val TABLE_NAME = "AddedStations"
        const val COLUMN_ID = "id"
        const val COLUMN_CHANGEUUID = "changeuuid"
        const val COLUMN_STATIONUUID = "stationuuid"
        const val COLUMN_NAME = "name"
        const val COLUMN_FAVICON = "favicon"
        const val COLUMN_FAVICON_DATE = "faviconDate"
        const val COLUMN_STREAMURL = "streamUrl"
    }
    object HistoryTable{}
}