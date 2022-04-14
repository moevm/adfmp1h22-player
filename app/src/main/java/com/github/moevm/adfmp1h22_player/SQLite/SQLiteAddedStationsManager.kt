package com.github.moevm.adfmp1h22_player.SQLite

import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import com.github.moevm.adfmp1h22_player.Station

class SQLiteAddedStationsManager(
    private val db : SQLHelper
) {
    fun insertRow(item: Station){
        val db = db.writableDatabase
        db.insertWithOnConflict(
            SQLiteContract.AddedStationsTable.TABLE_NAME,
            null,
            contentValuesOf(
                SQLiteContract.AddedStationsTable.COLUMN_CHANGEUUID to item.changeuuid,
                SQLiteContract.AddedStationsTable.COLUMN_STATIONUUID to item.stationuuid,
                SQLiteContract.AddedStationsTable.COLUMN_NAME to item.name,
                SQLiteContract.AddedStationsTable.COLUMN_STREAMURL to item.streamUrl,
                SQLiteContract.AddedStationsTable.COLUMN_FAVICON to item.faviconUrl,
                SQLiteContract.AddedStationsTable.COLUMN_CODEC to item.codec,
                SQLiteContract.AddedStationsTable.COLUMN_FAVICON_DATE to System.currentTimeMillis().toInt()
            ),
            SQLiteDatabase.CONFLICT_REPLACE
        )
        Log.d("TAG", "Add new row in Added(main) table")
    }

    fun deleteRow(item : Station){
        val db = db.writableDatabase
        db.execSQL("DELETE FROM "+SQLiteContract.AddedStationsTable.TABLE_NAME+" WHERE ${SQLiteContract.AddedStationsTable.COLUMN_STATIONUUID} = ${item.stationuuid}");
    }

    fun parseStation(c:Cursor) : Station{
        return Station(
            changeuuid = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AddedStationsTable.COLUMN_CHANGEUUID)),
            stationuuid = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AddedStationsTable.COLUMN_STATIONUUID)),
            name = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AddedStationsTable.COLUMN_NAME)).trimStart(),
            streamUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AddedStationsTable.COLUMN_STREAM_URL)),
            codec = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AddedStationsTable.COLUMN_CODEC)),
            faviconUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AddedStationsTable.COLUMN_FAVICON)),
        )
    }
    fun getData(): MutableList<Station> {
        val db = db.writableDatabase
        Log.d("TAG","in getData()")
        val c: Cursor = db.query(
            SQLiteContract.AddedStationsTable.TABLE_NAME,
            null, null, null, null, null, null
        )
        return c.use {
            val list = mutableListOf<Station>()
            while (c.moveToNext()) {
                list.add(parseStation(c))
            }
            return@use list
        }
    }
}
