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

    fun emptyTable(tableName : String) : Boolean{
        val db = db.writableDatabase
        var empty : Boolean = true
        val cursor : Cursor = db.rawQuery("SELECT count(*) FROM ${tableName}", null)
        cursor.moveToFirst()
        val d : Int = cursor.getInt(0)
        if(d > 0){
            empty = false
        }
        return empty
    }

    fun createTable(sql: String){
        val db = db.writableDatabase
        Log.d("TAG", Thread.currentThread().name.toString())
        db.execSQL(sql)
    }
    fun insertRows(list: List<Station>){
        Log.d("TAG", Thread.currentThread().name.toString())
        val db = db.writableDatabase
        Log.d("TAG", list.size.toString())
        db.beginTransaction()
        try {
            for(item in list){
                db.insertWithOnConflict(
                    SQLiteContract.AllStationsTable.TABLE_NAME,
                    null,
                    contentValuesOf(
                        SQLiteContract.AllStationsTable.COLUMN_CHANGEUUID to item.changeuuid,
                        SQLiteContract.AllStationsTable.COLUMN_STATIONUUID to item.stationuuid,
                        SQLiteContract.AllStationsTable.COLUMN_NAME to item.name,
                        SQLiteContract.AllStationsTable.COLUMN_STREAMURL to item.streamUrl,
                        SQLiteContract.AllStationsTable.COLUMN_FAVICON to item.faviconUrl,
                        SQLiteContract.AllStationsTable.COLUMN_FAVICON_DATE to System.currentTimeMillis().toInt()
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
            stationuuid = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_STATIONUUID)),
            name = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_NAME)),
<<<<<<< HEAD
            streamUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_STREAMURL)),
            faviconUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_FAVICON)),
=======
            streamUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_STREAM_URL)),
            faviconUrl = c.getString(c.getColumnIndexOrThrow(SQLiteContract.AllStationsTable.COLUMN_FAVICON))
>>>>>>> main
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
