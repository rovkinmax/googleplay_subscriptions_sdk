package com.apptilaus.subscriptions

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log
import com.apptilaus.subscriptions.data.ApptilausRepository
import com.apptilaus.subscriptions.data.PurchaseEvent
import java.util.concurrent.TimeUnit

private const val SHARED_PREFS_NAME = "ApptilausManager"
private const val PREFS_KEY_LAST_SESSION_ID = "PREFS_KEY_LAST_SESSION_ID"
/** New session interval */
private const val SESSION_MINS = 30
const val TAG_APTATIUS = "Apptilaus"

object ApptilausManager {

    object Config {
        var baseUrl: String? = null
        var userId: String? = null
        internal var deviceBaseUrl: String = "https://device.apptilaus.com"
        internal var apiBaseUrl: String = "https://api.apptilaus.com"
        internal val sessionPathEndpoint = "v1/device"
        internal val optOutEndpoint = "v1/optout"
        internal val purchaseEndpoint = "v1/purchase"
    }

    data class AppParams(val appId: String, val appToken: String, val enableSessionTracking: Boolean)

    internal lateinit var params: AppParams
    private lateinit var prefs: SharedPreferences
    private lateinit var androidId: String
    internal lateinit var dbHelper: DBHelper
    internal var onGetAdvertisingId: (() -> String?)? = null

    private val repository: ApptilausRepository = ApptilausRepository()

    @SuppressLint("HardwareIds")
    fun setup(context: Context, params: AppParams, onGetAdvertisingId: (() -> String?)? = null) {
        this.params = params
        this.prefs = context.getSharedPreferences(SHARED_PREFS_NAME, Context.MODE_PRIVATE)
        this.androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        this.onGetAdvertisingId = onGetAdvertisingId
        this.dbHelper = DBHelper(context)
        if (params.enableSessionTracking) {
            registerSessions()
        }
    }

    // TODO make private
    fun registerSessions() {
        val sessionId = System.currentTimeMillis()
        val lastSessionId = prefs.getString(PREFS_KEY_LAST_SESSION_ID, null)?.toLong()
        if (lastSessionId?.let { sessionId.millisToMinutes() - it.millisToMinutes() >= SESSION_MINS } != false) {
            repository.registerSession(
                appId = params.appId,
                sessionId = (sessionId / 1000).toString(),
                install = sessionId != lastSessionId,
                androidId = androidId
            )
            prefs.edit().putString(PREFS_KEY_LAST_SESSION_ID, sessionId.toString()).apply()
        } else {
            Log.i(TAG_APTATIUS, "already registered session today")
        }
    }

    fun optOut() {
        repository.optOut(androidId = androidId)
    }

    fun purchase(event: PurchaseEvent, parameters: Map<String, String> = emptyMap()) {
        if (!event.price.matches(Regex("\\d+\\.\\d\\d"))) {
            Log.w(TAG_APTATIUS, "Wrong price format. Price must contain 2 digits after decimal point")
        }
        repository.purchase(
            event = event,
            androidId = androidId,
            parameters = parameters
        )
    }

    private fun Long.millisToMinutes(): Long = TimeUnit.MINUTES.convert(this, TimeUnit.MILLISECONDS)
}