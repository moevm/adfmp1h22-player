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
        val createTable = "CREATE TABLE ${SQLiteContract.AllStationsTable.TABLE_NAME} (" +
                SQLiteContract.AllStationsTable.COLUMN_ID +
                " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SQLiteContract.AllStationsTable.COLUMN_CHANGEUUID + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_NAME + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_FAVICON + " TEXT," +
                SQLiteContract.AllStationsTable.COLUMN_FAVICON_DATE +
                " INTEGER NOT NULL)"

        db?.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {

    }

}