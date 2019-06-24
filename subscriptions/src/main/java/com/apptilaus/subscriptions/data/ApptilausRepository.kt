package com.apptilaus.subscriptions.data

import android.net.Uri
import android.util.Log
import androidx.annotation.WorkerThread
import com.apptilaus.subscriptions.ApptilausManager
import com.apptilaus.subscriptions.ConnectionHelper
import com.apptilaus.subscriptions.TAG_APTATIUS
import com.apptilaus.subscriptions.toRequest
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

private const val SENDER_INITIAL_DELEAY_SEC = 10L
private const val SENDER_DELEAY_SEC = 20L

class ApptilausRepository {

    private val threadCount: AtomicInteger = AtomicInteger()
    private val storeExecutor: ExecutorService = Executors.newCachedThreadPool {
        Thread(it, "Apptilaus-Store-${threadCount.incrementAndGet()}")
    }
    private val sendExecutor: ExecutorService = Executors.newSingleThreadScheduledExecutor {
        Thread(it, "Apptilaus-Send-${threadCount.incrementAndGet()}")
    }.apply {
        scheduleWithFixedDelay(::handleStored, SENDER_INITIAL_DELEAY_SEC, SENDER_DELEAY_SEC, TimeUnit.SECONDS)
    }
    private var lastAdId: String? = null

    fun registerSession(
        appId: String,
        sessionId: String,
        install: Boolean,
        androidId: String
    ) {
        storeExecutor.submit {
            run {
                ApptilausManager.Config.deviceBaseUrl
            }
                .let(Uri::parse)
                .buildUpon()
                .appendEncodedPath(ApptilausManager.Config.sessionPathEndpoint)
                .appendPath(appId)
                .appendPath("")
                .apply {
                    getAdId().also { appendQueryParameter("android_gps", it) }
                    if (install) {
                        appendQueryParameter("dp_activity", "install")
                        appendQueryParameter("dp_install", sessionId)
                    } else {
                        appendQueryParameter("dp_activity", "session")
                        appendQueryParameter("dp_session", sessionId)
                    }
                }
                .appendQueryParameter("android_id", androidId)
                .build()
                .also { uri ->
                    Store.storeRecord(
                        type = Record.Type.SESSION,
                        method = Record.Method.GET,
                        url = uri.toString(),
                        body = null
                    )
                }
        }
    }

    fun optOut(androidId: String) {
        storeExecutor.submit {
            run {
                ApptilausManager.Config.deviceBaseUrl
            }
                .let(Uri::parse)
                .buildUpon()
                .appendEncodedPath(ApptilausManager.Config.optOutEndpoint)
                .apply {
                    getAdId().also { appendQueryParameter("android_gps", it) }
                }
                .appendQueryParameter("android_id", androidId)
                .build()
                .also { uri ->
                    Store.storeRecord(
                        type = Record.Type.OUT_OUT,
                        method = Record.Method.GET,
                        url = uri.toString(),
                        body = null
                    )
                }
        }
    }

    fun purchase(
        androidId: String,
        event: PurchaseEvent,
        parameters: Map<String, String> = emptyMap()
    ) {
        storeExecutor.submit {
            run {
                ApptilausManager.Config.baseUrl ?: ApptilausManager.Config.apiBaseUrl
            }
                .let(Uri::parse)
                .buildUpon()
                .appendEncodedPath(ApptilausManager.Config.purchaseEndpoint)
                .appendPath(ApptilausManager.params.appId)
                .appendPath("")
                .build()
                .also { uri ->
                    Store.storeRecord(
                        type = Record.Type.PURCHASE,
                        method = Record.Method.POST,
                        url = uri.toString(),
                        body = event.toRequest(
                            androidGps = getAdId(),
                            androidId = androidId,
                            userId = ApptilausManager.Config.userId,
                            parameters = parameters
                        ).toString()
                    )
                }
        }
    }

    @WorkerThread
    private fun getAdId(): String =
        lastAdId
            ?: ApptilausManager.onGetAdvertisingId?.invoke()?.also { this.lastAdId = it }
            ?: "00000000-0000-0000-0000-000000000000"

    @WorkerThread
    private fun handleStored() {
        Store.processRecordsToSend { record ->
            try {
                Log.d(TAG_APTATIUS, "Try to send record ${record.id}")
                if (record.method == Record.Method.POST) {
                    ConnectionHelper.sendPost(
                        url = record.url,
                        token = ApptilausManager.params.appToken,
                        data = record.body ?: ""
                    )
                } else {
                    ConnectionHelper.sendGet(url = record.url)
                }.let { respose ->
                    when (respose) {
                        is ConnectionHelper.ResponseResult.Success -> {
                            logSendSuccess(record.type)
                            true
                        }
                        is ConnectionHelper.ResponseResult.Error -> {
                            logSendError(record.type, respose.throwable)
                            respose.responseCode == 400 || respose.responseCode == 422
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG_APTATIUS, "Process record error", e)
                false
            }
        }
    }

    private fun logSendSuccess(type: Record.Type) {
        Log.i(TAG_APTATIUS, "${type.name} processed")
    }

    private fun logSendError(type: Record.Type, throwable: Throwable?) {
        Log.e(TAG_APTATIUS, "${type.name} was not registered, due to error:\n${throwable?.message}")
    }
}