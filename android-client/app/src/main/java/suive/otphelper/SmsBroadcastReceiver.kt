package suive.otphelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

class SmsBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent);

        val url = URL("http://192.168.1.100:8678/message")
        thread(start = true) {
            messages.forEach { message ->
                val body = message.messageBody ?: return@forEach

                val connection = url.openConnection() as HttpURLConnection;
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
                    Log.i("SmsBroadcastReceiver", "Response code: $responseCode")
                } finally {
                    connection.disconnect()
                }
            }
        }
    }
}