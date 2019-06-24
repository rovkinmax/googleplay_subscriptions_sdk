package com.apptilaus.subscriptions

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import com.apptilaus.subscriptions.data.Record

internal class DBHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Apptilaus.db"
        private const val DATABASE_VERSION = 2
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
                CREATE TABLE ${Record.TABLE_NAME} (
                    ${BaseColumns._ID} INTEGER PRIMARY KEY,
                    ${Record.Column.type} TEXT,
                    ${Record.Column.method} TEXT,
                    ${Record.Column.url} TEXT,
                    ${Record.Column.body} TEXT,
                    ${Record.Column.created} INTEGER
                )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${Record.TABLE_NAME}")
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS ${Record.TABLE_NAME}")
        onCreate(db)
    }
}