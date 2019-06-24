package com.apptilaus.subscriptions

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.WorkerThread
import java.io.BufferedOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.net.SocketTimeoutException
import java.net.URL
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

private const val CONNECT_TIMEOUT = 5000

internal object ConnectionHelper {

    sealed class ResponseResult {
        data class Success(val code: Int, val message: String) : ResponseResult()
        data class Error(val throwable: Throwable? = null, val responseCode: Int? = null) : ResponseResult()
    }

    @WorkerThread
    internal fun sendPost(url: String, token: String, data: String): ResponseResult =
        try {
            URL(url).let {
                if (it.protocol.toLowerCase() == "https") {
                    trustAllHosts()
                    it.openConnection() as HttpsURLConnection
                } else {
                    it.openConnection() as HttpURLConnection
                }
            }.let { connection ->
                connection.instanceFollowRedirects = true
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestProperty("App-Token", token)
                try {
                    connection.doOutput = true
                    connection.setChunkedStreamingMode(0)
                    BufferedOutputStream(connection.outputStream).use { outputStream ->
                        OutputStreamWriter(outputStream).use { writer ->
                            writer.write(data)
                            writer.flush()
                        }
                    }
                    if (connection.responseCode == HTTP_MOVED_PERM ||
                        connection.responseCode == HTTP_MOVED_TEMP ||
                        connection.responseCode == HTTP_SEE_OTHER
                    ) {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        sendPost(newUrl, token, data)
                    } else {
                        checkResponse(connection.responseCode, connection.responseMessage)
                    }
                } catch (e: Exception) {
                    ResponseResult.Error(e)
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            ResponseResult.Error(e)
        }

    @WorkerThread
    internal fun sendGet(url: String): ResponseResult =
        try {
            URL(url).let {
                if (it.protocol.toLowerCase() == "https") {
                    trustAllHosts()
                    it.openConnection() as HttpsURLConnection
                } else {
                    it.openConnection() as HttpURLConnection
                }
            }.let { connection ->
                connection.instanceFollowRedirects = true
                connection.connectTimeout = CONNECT_TIMEOUT
                connection.requestMethod = "GET"
                connection.setRequestProperty("Content-Type", "application/json")
                try {
                    if (connection.responseCode == HTTP_MOVED_PERM ||
                        connection.responseCode == HTTP_MOVED_TEMP ||
                        connection.responseCode == HTTP_SEE_OTHER
                    ) {
                        val newUrl = connection.getHeaderField("Location")
                        connection.disconnect()
                        sendGet(newUrl)
                    } else {
                        ResponseResult.Success(connection.responseCode, connection.responseMessage)
                    }
                } catch (e: SocketTimeoutException) {
                    Log.e(TAG_APTATIUS, "Request timeout", e)
                    ResponseResult.Error(e)
                } catch (e: Exception) {
                    Log.e(TAG_APTATIUS, "Can't send request", e)
                    ResponseResult.Error(e)
                } finally {
                    connection.disconnect()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG_APTATIUS, "Can't open connection", e)
            ResponseResult.Error(e)
        }

    internal fun checkResponse(code: Int, message: String): ResponseResult =
        if (code.toString()[0].toString().toInt() == 2) {
            try {
                ResponseResult.Success(code, message)
            } catch (e: Exception) {
                ResponseResult.Error(Exception("Parse response exception"), code)
            }
        } else {
            ResponseResult.Error(Exception("Response error\n$code: $message"), code)
        }

    private fun trustAllHosts() {
        try {
            HttpsURLConnection.setDefaultHostnameVerifier { hostname, session -> true }
            SSLContext.getInstance("TLS").also { sslContext ->
                sslContext.init(null, arrayOf(object : X509TrustManager {
                    @SuppressLint("TrustAllX509TrustManager")
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

                    @SuppressLint("TrustAllX509TrustManager")
                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                }), SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
            }
        } catch (e: Exception) { // should never happen
            e.printStackTrace()
        }

    }
}