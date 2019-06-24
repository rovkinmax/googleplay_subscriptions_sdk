package com.apptilaus.sample

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.*
import com.apptilaus.subscriptions.ApptilausManager
import com.apptilaus.subscriptions.data.PurchaseEvent
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.roundToLong


class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var billingClient: BillingClient
    private var skuDetails: SkuDetails? = null

    @SuppressLint("ApplySharedPref")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        this.prefs = getSharedPreferences("ApptilausManager", Context.MODE_PRIVATE)

        this.billingClient = BillingClient.newBuilder(this).setListener { responseCode, purchases ->
            skuDetails?.also { details ->
                purchases?.find { it.sku == details.sku }?.also { purchase ->
                    ApptilausManager.purchase(
                        PurchaseEvent(
                            price = ((details.priceAmountMicros / 10000F).roundToLong() / 100F).toString(),
                            currency = details.priceCurrencyCode,
                            item = details.sku,
                            transactionId = purchase.orderId,
                            receipt = purchase.signature,
                            purchaseToken = purchase.purchaseToken,
                            transactionDate = purchase.purchaseTime.toString()
                        ),
                        parameters = mapOf("param1" to "value1", "param2" to "value2")
                    )
                }
            }
        }.build()

        optOutBtn.setOnClickListener {
             ApptilausManager.optOut()
        }

        subscriptionBtn.setOnClickListener {
            getSubsInfo("debug1") {
                this.skuDetails = it
                BillingFlowParams
                    .newBuilder()
                    .setSkuDetails(it)
                    .build()
                    .also { params -> billingClient.launchBillingFlow(this, params) }
            }
        }

        forceNewSessionBtn.setOnClickListener {
            prefs.edit().remove("PREFS_KEY_LAST_SESSION_ID").commit()
            ApptilausManager.registerSessions()
        }
    }

    private fun getSubsInfo(sku: String, callback: (SkuDetails) -> Unit) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingClient.BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingClient.BillingResponse.OK) {
                    SkuDetailsParams
                        .newBuilder()
                        .setSkusList(listOf(sku))
                        .setType(BillingClient.SkuType.SUBS)
                        .build()
                        .also { params ->
                            billingClient.querySkuDetailsAsync(params) { responseCode, skuDetailsList ->
                                if (responseCode == BillingClient.BillingResponse.OK) {
                                    skuDetailsList.firstOrNull()?.also { purchase: SkuDetails ->
                                        callback(purchase)
                                    }
                                }
                            }
                        }
                }
            }

            override fun onBillingServiceDisconnected() = Unit
        })
    }
}
