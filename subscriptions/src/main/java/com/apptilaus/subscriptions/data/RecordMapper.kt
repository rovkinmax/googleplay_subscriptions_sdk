package com.apptilaus.subscriptions.data

import android.database.Cursor
import android.provider.BaseColumns

internal fun Cursor.toRecord(): Record = Record(
    id = getLong(getColumnIndex(BaseColumns._ID)),
    type = Record.Type.valueOf(getString(getColumnIndex(Record.Column.type))),
    method = Record.Method.valueOf(getString(getColumnIndex(Record.Column.method))),
    url = getString(getColumnIndex(Record.Column.url)),
    body = getString(getColumnIndex(Record.Column.body)),
    created = getLong(getColumnIndex(Record.Column.created))
)