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
    override fun onReceive(context: Context?, intent: Intent?) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (!preferences.getBoolean("sms_sending_enabled", true)) {
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

        val serverUrl = requireNotNull(
            preferences.getString("server_url", "http://192.168.1.100:8678")
        )
        val url = URL(serverUrl).toURI().resolve("/message").toURL()
        Log.i("SmsBroadcastReceiver", "Sending message to OTP helper server ($url)")
        thread(start = true) {
            messages.forEach { message ->
                withRetry(5) {
                    val body = message.messageBody ?: return@withRetry

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
                                write(body.toByteArray())
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

    private fun withRetry(attempts: Int, action: () -> Unit) {
        for (currentAttempt in 1..attempts) {
            try {
                action()
                return
            } catch (e: Exception) {
                Log.e(
                    javaClass.simpleName,
                    "Action failed, attempts left: ${attempts - currentAttempt}", e
                )
            }
        }
    }
}