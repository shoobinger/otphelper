package suive.otphelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.preference.PreferenceManager
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SmsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private val otpRegex = Regex("[0-9]+")
        private const val RETRY_ATTEMPTS_NUM = 5
        private const val DEFAULT_SERVER_URL = "http://192.168.1.100:8678"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (!preferences.getBoolean("sms_sending_enabled", true)) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

        val serverUrl = requireNotNull(
            preferences.getString("server_url", DEFAULT_SERVER_URL)
        )
        val url = URL(serverUrl).toURI().resolve("/otp").toURL()
        thread(start = true) {
            messages.forEach { message ->
                val otp = message.messageBody?.let { extractOtp(it) } ?: return@forEach
                Log.i(javaClass.simpleName, "Sending code $otp to OTP helper server ($url)")

                withRetry(RETRY_ATTEMPTS_NUM) {
                    val connection = url.openConnection() as HttpURLConnection
                    try {
                        connection.doOutput = true
                        connection.doInput = true
                        connection.setChunkedStreamingMode(0)
                        connection.requestMethod = "POST"
                        connection.setRequestProperty("Content-Type", "text/plain")
                        connection.setRequestProperty("Accept", "text/plain")
                        BufferedOutputStream(connection.outputStream)
                            .apply {
                                write(otp.toByteArray())
                                flush()
                            }
                        val responseCode = connection.responseCode.toString()
                        Log.i(javaClass.simpleName, "Response code: $responseCode")
                    } finally {
                        connection.disconnect()
                    }
                }
            }
        }
    }

    private fun extractOtp(message: String): String? = otpRegex.find(message)?.groups?.get(0)?.value

    private fun withRetry(attempts: Int, action: () -> Unit) {
        for (currentAttempt in 1..attempts) {
            try {
                action()
            } catch (e: Exception) {
                Log.e(
                    javaClass.simpleName,
                    "Action failed, attempts left: ${attempts - currentAttempt}", e
                )
            }
        }
    }
}