package co.anode.anodium

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import co.anode.anodium.support.AnodeUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class PktMainActivity : AppCompatActivity() {
    private val REQUIRED_SDK_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.BIND_VPN_SERVICE
    )
    private val CHECK_PREMIUM_INTERVAL = 2 * 60 * 1000L // 2 minutes in milliseconds
    private val scope = MainScope()
    private var job: Job? = null
    private var notification10MinShown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pkt_main)

        checkPermissions()

        startNotificationsCheck()
        Timber.i("PktMainActivity onCreate")
    }

    private fun startNotificationsCheck() {
        stopNotificationsCheck()
        job = scope.launch {
            while(true) {
                checkPremium() // the function that should be ran every second
                delay(CHECK_PREMIUM_INTERVAL)
            }
        }
    }

    private fun stopNotificationsCheck() {
        job?.cancel()
        job = null
    }
    override fun onResume() {
        super.onResume()
        Timber.i("PktMainActivity onResume")
        startNotificationsCheck()
    }
    override fun onPause() {
        super.onPause()
        stopNotificationsCheck()
    }

    private fun checkPermissions() {
        val REQUEST_CODE_ASK_PERMISSIONS = 1
        val missingPermissions: MutableList<String> = ArrayList()
        // check all required dynamic permissions
        for (permission in REQUIRED_SDK_PERMISSIONS) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            // request all missing permissions
            val permissions = missingPermissions.toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE_ASK_PERMISSIONS)
        } else {
            val grantResults = IntArray(REQUIRED_SDK_PERMISSIONS.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(
                REQUEST_CODE_ASK_PERMISSIONS, REQUIRED_SDK_PERMISSIONS,
                grantResults
            )
        }
    }

    private fun checkPremium() {
        val sharedPreferences = getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        for ((key, value) in allEntries) {
            if (key.startsWith("Premium_")) {
                val premiumTime = sharedPreferences.getLong(key, 0L)
                val currentTime = Calendar.getInstance().timeInMillis
                if (premiumTime > currentTime) {
                    // Calculate the remaining time in milliseconds
                    val remainingTime = premiumTime - currentTime
                    // Check if there are 10 minutes remaining
                    if (!notification10MinShown && (remainingTime <= TimeUnit.MINUTES.toMillis(10))) {
                        val localDateTime = LocalDateTime.ofEpochSecond((premiumTime/1000), 0, ZoneOffset.systemDefault().rules.getOffset(Instant.now()) )
                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                        AnodeUtil.pushNotification("AnodeVPN", "${getString(com.pkt.core.R.string.premium_ends)} ${localDateTime.format(formatter)}")
                        notification10MinShown = true
                    }
                } else {
                    AnodeUtil.pushNotification("AnodeVPN", getString(R.string.notification_premium_ended))
                    sharedPreferences.edit().remove(key).apply()
                }
            }
        }
    }
}