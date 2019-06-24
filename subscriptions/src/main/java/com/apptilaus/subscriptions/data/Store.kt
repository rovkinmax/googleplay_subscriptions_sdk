package com.apptilaus.subscriptions.data

import android.content.ContentValues
import android.provider.BaseColumns
import android.util.Log
import com.apptilaus.subscriptions.ApptilausManager
import com.apptilaus.subscriptions.TAG_APTATIUS

internal object Store {
    fun storeRecord(type: Record.Type, method: Record.Method, url: String, body: String?) {
        ApptilausManager.dbHelper.writableDatabase.use { db ->
            ContentValues().apply {
                put(Record.Column.type, type.name)
                put(Record.Column.method, method.name)
                put(Record.Column.url, url)
                put(Record.Column.body, body)
                put(Record.Column.created, System.currentTimeMillis())
            }.also { values ->
                val inserted = db?.insert(Record.TABLE_NAME, null, values)
                inserted?.also {
                    Log.i(TAG_APTATIUS, "Created track record Type: ${type.name}, id: $inserted")
                } ?: Log.e(TAG_APTATIUS, "Can't create track record Type: ${type.name}")
            }
        }
    }

    fun processRecordsToSend(handler: (record: Record) -> Boolean) {
        ApptilausManager.dbHelper.readableDatabase.use { db ->
            db.query(
                Record.TABLE_NAME,
                arrayOf(
                    BaseColumns._ID,
                    Record.Column.type,
                    Record.Column.method,
                    Record.Column.url,
                    Record.Column.body,
                    Record.Column.created
                ),
                null,
                null,
                null,
                null,
                "${BaseColumns._ID} DESC",
                "5"
            ).use { cursor ->
                val wreckedRecords = mutableSetOf<Long>()
                mutableListOf<Record>().apply {
                    while (cursor.moveToNext()) {
                        try {
                            cursor.toRecord().also { add(it) }
                        } catch (e: Exception) {
                            wreckedRecords.add(cursor.getLong(cursor.getColumnIndex(BaseColumns._ID)))
                            Log.e(TAG_APTATIUS, "Map cursor to record error", e)
                        }
                    }
                }.let { Pair(it, wreckedRecords) }
            }
        }.also { (records, wreckedRecords) ->
            ApptilausManager.dbHelper.writableDatabase.use { db ->
                // Remove wrecked records
                wreckedRecords.takeIf { it.isNotEmpty() }?.run {
                    val elements = joinToString(",")
                    Log.i(TAG_APTATIUS, "Delete sent record $elements")
                    if (db.delete(Record.TABLE_NAME, "${BaseColumns._ID} IN (?)", arrayOf(elements)) > 0) {
                        Log.i(TAG_APTATIUS, "Records $elements was deleted")
                    }
                }
                // Process normal records
                records.forEach { record ->
                    if (handler(record)) {
                        Log.i(TAG_APTATIUS, "Delete sent record ${record.id}")
                        db.delete(
                            Record.TABLE_NAME,
                            "${BaseColumns._ID} = ?",
                            arrayOf(record.id.toString())
                        ).also { deletedCount ->
                            if (deletedCount > 0) Log.i(TAG_APTATIUS, "Record ${record.id} was deleted")
                        }
                    }
                }
            }
        }
    }
}