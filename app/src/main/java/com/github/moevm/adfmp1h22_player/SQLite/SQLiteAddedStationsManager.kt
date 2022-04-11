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
    fun insertRows(list: List<Station>){
        val db = db.writableDatabase
        db.beginTransaction()
        try {
            for(item in list){
                db.insertWithOnConflict(
                    SQLiteContract.AddedStationsTable.TABLE_NAME,
                    null,
                    contentValuesOf(
                        SQLiteContract.AddedStationsTable.COLUMN_CHANGEUUID to item.changeuuid,
                        SQLiteContract.AddedStationsTable.COLUMN_NAME to item.name,
                        SQLiteContract.AddedStationsTable.COLUMN_FAVICON to item.faviconUrl,
                        SQLiteContract.AddedStationsTable.COLUMN_FAVICON_DATE to System.currentTimeMillis().toInt()
                    ),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
            Log.d("TAG", "Add new row in _NEW table")
        }catch (e: SQLiteConstraintException){
            Log.d("TAG", "SQLiteConstraintException if SQLiteAllStationsManagement")
        }finally {
            db.endTransaction()
        }
    }
    fun parseStation(c:Cursor) : Station{
        return Station(
            changeuuid = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_CHANGEUUID)),
            name = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_NAME)),
            streamUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_STREAM_URL)),
            faviconUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_FAVICON))
        )
    }
    fun getData(): MutableList<Station> {
        val db = db.writableDatabase
        Log.d("TAG","in getData()")
        val c: Cursor = db.query(
            SQLiteContract.AllStationsTable.TABLE_NAME,
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
