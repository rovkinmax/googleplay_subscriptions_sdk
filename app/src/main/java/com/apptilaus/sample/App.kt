package com.apptilaus.sample

import android.app.Application
import android.util.Log
import com.apptilaus.subscriptions.ApptilausManager
import com.apptilaus.subscriptions.TAG_APTATIUS

private const val APPTILAUS_APP_ID = "aab9a8ef-66ba-498e-a218-a2e44627880a"
private const val APPTILAUS_APP_TOKEN = "336e18f3-b4af-4e9b-80c6-f6fb3213def7"

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        if (APPTILAUS_APP_ID.isBlank() || APPTILAUS_APP_TOKEN.isBlank()) {
            Log.e(TAG_APTATIUS, "Invalid initializer parameters")
        } else {
            ApptilausManager.Config.baseUrl = "https://request.teamcore.us/1nmwyev1/"
            ApptilausManager.Config.userId = "example@gmail.com"
            ApptilausManager.setup(
                context = this,
                params = ApptilausManager.AppParams(
                    appId = APPTILAUS_APP_ID,
                    appToken = APPTILAUS_APP_TOKEN,
                    enableSessionTracking = true
                )
            ) {
                // Return AdvertisingId from here
                null
            }
        }
    }
}