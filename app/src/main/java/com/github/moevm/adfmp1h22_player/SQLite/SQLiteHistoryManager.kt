package com.github.moevm.adfmp1h22_player.SQLite

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import com.github.moevm.adfmp1h22_player.TrackMetaData

class SQLiteHistoryManager(
    private val db : SQLHelper
) {
    fun insertRow(item: TrackMetaData){
        val db = db.writableDatabase
        db.insertWithOnConflict(
            SQLiteContract.HistoryTable.TABLE_NAME,
            null,
            contentValuesOf(
                SQLiteContract.HistoryTable.COLUMN_TRACK_ORIGTITLE to item.original,
                SQLiteContract.HistoryTable.COLUMN_TRACK_ARTIST to item.artist,
                SQLiteContract.HistoryTable.COLUMN_TRACK_TITLE to item.title,
            ),
            SQLiteDatabase.CONFLICT_REPLACE
        )
        Log.d("TAG", "Add new row in Added(main) table")
    }

    fun parseTrackMetaData(c:Cursor) : TrackMetaData{
        return TrackMetaData(
            original = c.getString(c.getColumnIndexOrThrow(SQLiteContract.HistoryTable.COLUMN_TRACK_ORIGTITLE)),
            artist = c.getString(c.getColumnIndexOrThrow(SQLiteContract.HistoryTable.COLUMN_TRACK_ARTIST)),
            title = c.getString(c.getColumnIndexOrThrow(SQLiteContract.HistoryTable.COLUMN_TRACK_TITLE))
        )
    }

    fun getData(): MutableList<TrackMetaData> {
        val db = db.writableDatabase
        Log.d("TAG","in getData()")
        val c: Cursor = db.query(
            SQLiteContract.HistoryTable.TABLE_NAME,
            null, null, null, null, null, null
        )
        return c.use {
            val list = mutableListOf<TrackMetaData>()
            while (c.moveToNext()) {
                list.add(parseTrackMetaData(c))
            }
            return@use list
        }
    }
}