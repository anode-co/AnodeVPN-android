package co.anode.anodium

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import co.anode.anodium.databinding.ActivityMainBinding
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private val LOGTAG = "co.anode.anodium"
    private lateinit var binding: ActivityMainBinding
    private val REQUIRED_SDK_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.CHANGE_WIFI_STATE
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Error Handling
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable -> //Catch your exception
            exception(paramThrowable)
        }

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        initializeApp()
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)
    }

    private fun initializeApp() {
        AnodeClient.init(applicationContext, this)
        AnodeUtil.init(applicationContext)
        AnodeUtil.initializeApp()
        AnodeUtil.launchPld()
        AnodeUtil.launchCJDNS()
        AnodeUtil.serviceThreads()
        checkPermissions()
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        //If there is no username stored
        if (prefs.getString("username", "").isNullOrEmpty()) {
            //Generate a username
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var usernameResponse: String
            executor.execute {
                usernameResponse = AnodeClient.generateUsername()
                handler.post {
                    generateUsernameHandler(usernameResponse)
                }
            }
        }
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
            val permissions = missingPermissions
                .toTypedArray()
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

    private fun generateUsernameHandler(result: String) {
        Log.i(co.anode.anodium.support.LOGTAG,"Received from API: $result")
        if ((result.isBlank())) {
            return
        } else if (result.contains("400") || result.contains("401")) {
            val json = result.split("-")[1]
            var msg = result
            try {
                val jsonObj = JSONObject(json)
                msg = jsonObj.getString("username")
            } catch (e: JSONException) {
                msg += " Invalid JSON"
            }
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        } else if (result.contains("ERROR: ")) {
            Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
        } else {
            val jsonObj = JSONObject(result)
            if (jsonObj.has("username")) {
                val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString("username", jsonObj.getString("username"))
                    putBoolean("SignedIn", false)
                    commit()
                }
            }
        }
    }

    fun exception(paramThrowable: Throwable) {
        //Toast message before exiting app
        var type = "other"
        if (paramThrowable.toString().contains("CjdnsException") ) type = "cjdnsSocket"
        else if (paramThrowable.toString().contains("AnodeUtilException") ) type = "cjdroute"
        else if (paramThrowable.toString().contains("AnodeVPNException") ) type = "vpnService"
        else if (paramThrowable.toString().contains("LndRPCException") ) {
            type = "lnd"
            AnodeClient.storeFileAsError(AnodeUtil.context!!, type, "data/data/co.anode.anodium/files/pld.log")
        }
        // we'll post the error on next startup
        AnodeClient.storeError(AnodeUtil.context!!, type, paramThrowable)

        object : Thread() {
            override fun run() {
                Looper.prepare()
                Toast.makeText(AnodeUtil.context!!, "ERROR: " + paramThrowable.message, Toast.LENGTH_LONG).show()
                AnodeClient.mycontext = AnodeUtil.context!!
                Looper.loop()
            }
        }.start()
        try {
            // Let the Toast display and give some time to post to server before app will get shutdown
            Thread.sleep(10000)
        } catch (e: InterruptedException) {}
        exitProcess(1)
    }
}