package com.github.moevm.adfmp1h22_player.SQLite

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class SQLHelper(
    private val context: Context
) : SQLiteOpenHelper(context, "RadioPlayerDataBase.db", null, 2) {
    override fun onCreate(db: SQLiteDatabase?) {
        Log.d("TAG", "TUT1")
        val createTableAllStations = "CREATE TABLE IF NOT EXISTS ${SQLiteContract.AllStationsTable.TABLE_NAME} (" +
                SQLiteContract.AllStationsTable.COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SQLiteContract.AllStationsTable.COLUMN_CHANGEUUID + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_NAME + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_FAVICON + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_FAVICON_DATE +
                " INTEGER NOT NULL)"
        db?.execSQL(createTableAllStations)

        val createTableAddedStations = "CREATE TABLE IF NOT EXISTS ${SQLiteContract.AddedStationsTable.TABLE_NAME} (" +
                SQLiteContract.AddedStationsTable.COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SQLiteContract.AddedStationsTable.COLUMN_CHANGEUUID + " TEXT," +
                SQLiteContract.AddedStationsTable.COLUMN_NAME + " TEXT," +
                SQLiteContract.AddedStationsTable.COLUMN_FAVICON + " TEXT," +
                SQLiteContract.AddedStationsTable.COLUMN_FAVICON_DATE +
                " INTEGER NOT NULL)"
        db?.execSQL(createTableAddedStations)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {

    }

}