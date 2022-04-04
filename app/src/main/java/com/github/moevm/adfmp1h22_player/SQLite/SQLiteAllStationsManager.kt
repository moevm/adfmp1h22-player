package com.github.moevm.adfmp1h22_player.SQLite

import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.core.content.contentValuesOf
import com.github.moevm.adfmp1h22_player.Station

class SQLiteAllStationsManager(
    private val db : SQLHelper
) {
    fun createTable(sql: String){
        val db = db.writableDatabase
        db.execSQL(sql)
    }
    fun insertRows(list: List<Station>){
        val db = db.writableDatabase
        db.beginTransaction()
        try {
            for(item in list){
                db.insertWithOnConflict(
                    SQLiteContract.AllStationsTable.TABLE_NAME_NEW,
                    null,
                    contentValuesOf(
                        SQLiteContract.AllStationsTable.COLUMN_CHANGEUUID to item.changeuuid,
                        SQLiteContract.AllStationsTable.COLUMN_NAME to item.name,
                        SQLiteContract.AllStationsTable.COLUMN_FAVICON to item.faviconUrl,
                        SQLiteContract.AllStationsTable.COLUMN_FAVICON_DATE to System.currentTimeMillis().toInt()
                    ),
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
//            Log.d("TAG", "Add new row in _NEW table")
        }catch (e: SQLiteConstraintException){
            Log.d("TAG", "SQLiteConstraintException if SQLiteAllStationsManagement")
        }finally {
           db.endTransaction()
        }
    }

    fun replace(name1: String, name2: String){
        val db = db.writableDatabase
        db.execSQL("ALTER TABLE ${name1} RENAME TO ${name2}")
    }

    fun deleteTable(name: String){
        val db = db.writableDatabase
        db.execSQL("DROP TABLE ${name}")
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
