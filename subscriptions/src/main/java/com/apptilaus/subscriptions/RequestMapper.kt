package com.apptilaus.subscriptions

import com.apptilaus.subscriptions.data.PurchaseEvent
import org.json.JSONObject

internal fun PurchaseEvent.toRequest(
    androidGps: String?,
    androidId: String,
    userId: String?,
    parameters: Map<String, String>
): JSONObject = JSONObject().apply {
    put("platform", "GooglePlay")
    put("android_id", androidId)
    androidGps?.also { put("android_gps", it) }
    put("price", price)
    put("currency", currency)
    put("sdk_version", BuildConfig.VERSION_NAME)
    put("item", item)
    put("transaction_id", transactionId)
    put("transaction_date", transactionDate)
    put("receipt", receipt)
    put("purchase_token", purchaseToken)
    userId?.also { put("user_id", it) }
    parameters.forEach { (key, value) -> put("dp_$key", value) }
}