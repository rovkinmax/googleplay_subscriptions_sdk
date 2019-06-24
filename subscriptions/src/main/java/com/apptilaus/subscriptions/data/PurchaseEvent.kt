package com.apptilaus.subscriptions.data

data class PurchaseEvent(
    val price: String,
    val currency: String,
    val item: String,
    val transactionId: String,
    val transactionDate: String,
    val receipt: String,
    val purchaseToken: String
)