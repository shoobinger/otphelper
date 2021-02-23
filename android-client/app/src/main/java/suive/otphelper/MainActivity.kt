package suive.otphelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS),
            10
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        val granted = if (checkPermissionGranted(
                requestCode,
                permissions,
                grantResults
            )
        ) "permission granted" else "permission not granted"
        Toast.makeText(this, granted, Toast.LENGTH_SHORT).show()
    }

    fun checkPermissionGranted(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ): Boolean {
        when (requestCode) {
            10 -> {
                // If request is cancelled, the result arrays are empty.
                return (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            }
        }
        return false
    }
}