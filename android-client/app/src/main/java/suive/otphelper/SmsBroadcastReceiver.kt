package suive.otphelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.preference.PreferenceManager
import androidx.work.*
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration

class SmsBroadcastReceiver : BroadcastReceiver() {
    companion object {
        private val otpRegex = Regex("[0-9]+")
        private const val KEY_OTP = "otp"
        private const val DEFAULT_SERVER_URL = "http://192.168.1.100:8678"
    }

    class SendOtpWorker(private val context: Context, workerParams: WorkerParameters) :
        Worker(context, workerParams) {
        override fun doWork(): Result {
            val preferences = PreferenceManager.getDefaultSharedPreferences(context)
            if (!preferences.getBoolean("sms_sending_enabled", true)) {
                return Result.success()
            }

            val serverUrl = requireNotNull(
                preferences.getString("server_url", DEFAULT_SERVER_URL)
            )
            val url = URL(serverUrl).toURI().resolve("/otp").toURL()

            val otp = inputData.getString(KEY_OTP) ?: return Result.failure()

            Log.i(javaClass.simpleName, "Sending code $otp to OTP helper server ($url)")
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
                val responseCode = connection.responseCode
                Log.i(javaClass.simpleName, "Response code: $responseCode")
                if (responseCode >= 500) {
                    return Result.retry()
                }
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "OTP sending work failed", e)
                return Result.retry()
            } finally {
                connection.disconnect()
            }

            return Result.success()
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        messages.forEach { message ->
            val otp = message.messageBody?.let { extractOtp(it) } ?: return@forEach

            val workRequest = OneTimeWorkRequest.Builder(SendOtpWorker::class.java)
                .setInputData(Data.Builder().putString(KEY_OTP, otp).build())
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(1L))
                .build()

            Log.i(javaClass.simpleName, "Enqueuing work to send OTP code $otp")
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    private fun extractOtp(message: String): String? = otpRegex.find(message)?.groups?.get(0)?.value
}