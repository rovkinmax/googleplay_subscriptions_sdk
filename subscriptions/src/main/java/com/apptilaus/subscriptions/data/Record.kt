package com.apptilaus.subscriptions.data

import android.provider.BaseColumns

internal data class Record(
    val id: Long,
    val type: Type,
    val method: Method,
    val url: String,
    val body: String?,
    val created: Long
) {
    companion object {
        const val TABLE_NAME = "track_records"
    }

    object Column : BaseColumns {
        const val type = "rec_type"
        const val method = "method"
        const val url = "url"
        const val body = "body"
        const val created = "created"
    }

    enum class Type {
        SESSION, PURCHASE, OUT_OUT;
    }

    enum class Method {
        GET, POST;
    }
}