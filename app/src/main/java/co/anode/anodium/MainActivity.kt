package co.anode.anodium

import android.Manifest
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import co.anode.anodium.databinding.ActivityMainBinding
import co.anode.anodium.support.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import timber.log.Timber.Forest.plant
import java.util.*
import java.util.concurrent.Executors
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val REQUIRED_SDK_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_NETWORK_STATE,
        Manifest.permission.ACCESS_WIFI_STATE,
        Manifest.permission.INTERNET,
        Manifest.permission.CHANGE_NETWORK_STATE,
        Manifest.permission.CHANGE_WIFI_STATE,
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    private fun initTimber() {
        if (BuildConfig.DEBUG) {
            plant(Timber.DebugTree())
        } else {
            plant(object : Timber.Tree() {
                override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                    // TODO
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //Refuse to start if another instance is already running
        /*if(AnodeUtil.isCjdnsAlreadyRunning()) {
            Toast.makeText(this, "Anode is already running", Toast.LENGTH_LONG).show()
            finish()
            exitProcess(0)
        }*/
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

        //start thread to look for Pkt.cube wifi
        searchForInternetSharing()
    }

    private fun initializeApp() {
        //Initialize Util before Client
        AnodeUtil.init(applicationContext)
        AnodeClient.init(applicationContext, this)
        AnodeUtil.initializeApp()
        AnodeUtil.launchCJDNS()
        AnodeUtil.launchPld()
        AnodeUtil.serviceThreads()
        checkPermissions()
        val prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE)
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
        AnodeUtil.addCjdnsPeers()
        createNotificationChannel()
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
        Log.i(BuildConfig.APPLICATION_ID,"Received from API: $result")
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
                val prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString("username", jsonObj.getString("username"))
                    putBoolean("SignedIn", false)
                    commit()
                }
            }
        }
    }

    private fun searchForInternetSharing() {
        //Only for >Q versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //Init CubeWifi
            CubeWifi.init(applicationContext)
            //Search for Pkt.cube wifi
            Thread({
                //wait for 1min before start searching
                Thread.sleep(60000)
                while (AnodeUtil.enablePktCubeConnection) {
                    //search for available wifi ssid
                    AnodeUtil.isInternetSharingAvailable = CubeWifi.isCubeNetworkAvailable()
                    Thread.sleep(30000)
                }
            }, "VPNFragment.SearchForCubeWifi").start()
        }
    }

    fun createNotificationChannel() {
        // Create the NotificationChannel
        val name = "anodium_channel_01"
        val descriptionText = "anodium_channel_01"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val mChannel = NotificationChannel(AnodeUtil.CHANNEL_ID, name, importance)
        mChannel.description = descriptionText
        // Register the channel with the system; you can't change the importance
        // or other notification behaviors after this
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

    fun exception(paramThrowable: Throwable) {
        //Toast message before exiting app
        var type = "other"
        if (paramThrowable.toString().contains("CjdnsException") ) type = "cjdnsSocket"
        else if (paramThrowable.toString().contains("AnodeUtilException") ) type = "cjdroute"
        else if (paramThrowable.toString().contains("AnodeVPNException") ) type = "vpnService"
        else if (paramThrowable.toString().contains("LndRPCException") ) {
            type = "lnd"
            AnodeClient.storeFileAsError(AnodeUtil.context!!, type, "data/data/${BuildConfig.APPLICATION_ID}/files/pld.log")
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