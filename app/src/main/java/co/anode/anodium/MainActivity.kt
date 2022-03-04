@file:Suppress("DEPRECATION")

package co.anode.anodium

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.VpnService
import android.os.*
import android.text.Html
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private val buttonStateDisconnected = 0
    private val buttonStateConnecting = 1
    private val buttonStateConnected = 2
    private var mainMenu: Menu? = null
    private var publicIpThreadSleep: Long = 10
    private var uiInForeground = true
    private var previousPublicIPv4 = ""
    val myPermissionsRequestWriteExternal = 1
    private val vpnConnectionWaitingInterval = 30000L
    val h = Handler()

    companion object {
        private const val LOGTAG = "co.anode.anodium"
    }

    fun startBackgroundThreads() {
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        prefs.edit().putBoolean("lndwalletopened", false).apply()

        //Check internet connectivity & public IP
        Thread({
            while (true) {
                if ((internetConnection() == false) && (uiInForeground)) {
                    runOnUiThread {
                        Toast.makeText(this, getString(R.string.toast_no_internet), Toast.LENGTH_LONG).show()
                    }
                }
                Thread.sleep(3000)
            }
        }, "MainActivity.CheckInternetConnectivity").start()

        //Get v4 public IP
        Thread({
            while (true) {
                if (internetConnection() && uiInForeground) {
                    val textPublicIP = findViewById<TextView>(R.id.v4publicip)
                    val publicIP = getPublicIPv4()
                    if (!publicIP.contains("Error")) {
                        publicIpThreadSleep = 10000
                    }
                    runOnUiThread {
                        textPublicIP.text = Html.fromHtml("<b>" + this.resources.getString(R.string.text_publicipv4) + "</b>&nbsp;" + publicIP)
                        if ((AnodeClient.vpnConnected) &&
                            (previousPublicIPv4 != publicIP) &&
                            ((publicIP != "None") || (!publicIP.contains("Error")))) {
                            bigbuttonState(buttonStateConnected)
                        }
                    }
                    previousPublicIPv4 = publicIP
                }
                Thread.sleep(publicIpThreadSleep)
            }
        }, "MainActivity.GetPublicIPv4").start()

        //Get v6 public IP
        Thread({
            while (true) {
                if (internetConnection() && uiInForeground) {
                    val textPublicIP = findViewById<TextView>(R.id.v6publicip)
                    val publicIP = getPublicIPv6()
                    if (!publicIP.contains("Error")) {
                        publicIpThreadSleep = 10000
                    }
                    runOnUiThread {
                        textPublicIP.text = Html.fromHtml("<b>" + this.resources.getString(R.string.text_publicipv6) + "</b>&nbsp;" + publicIP)
                        if (AnodeClient.vpnConnected) {
                            bigbuttonState(buttonStateConnected)
                        }
                    }
                }
                Thread.sleep(publicIpThreadSleep)
            }
        }, "MainActivity.GetPublicIPv4").start()

        //Check for event log files daily
        Thread({
            Log.i(LOGTAG, "MainActivity.UploadEventsThread startup")
            val filesDir = this.filesDir.toString()
            while (true) {
                AnodeClient.ignoreErr {
                    //Check if 24 hours have passed since last log file submitted
                    if ((System.currentTimeMillis() - prefs.getLong("LastEventLogFileSubmitted", 0) > 86400000) or
                        (System.currentTimeMillis() - prefs.getLong("LastRatingSubmitted", 0) > 86400000)) {
                        val bEvents = File(filesDir + "/anodium-events.log").exists()
                        val bRatings = File(filesDir + "/anodium-rating.json").exists()
                        var timetosleep: Long = 60000
                        if ((!bEvents) or (!bRatings)) {
                            Log.d(LOGTAG, "No events or ratings to report, sleeping")
                            Thread.sleep((60 * 60000).toLong())
                        } else if (!AnodeClient.checkNetworkConnection()) {
                            // try again in 10 seconds, waiting for internet
                            Log.i(LOGTAG, "Waiting for internet connection to report events")
                            Thread.sleep(10000)
                        } else {
                            if (bEvents) {
                                Log.i(LOGTAG, "Reporting an events log file")
                                if (AnodeClient.httpPostEvent(this.filesDir).contains("Error")) {
                                    timetosleep = 60000
                                } else {
                                    //Log posted, sleep for a day
                                    timetosleep = (60000 * 60 * 24).toLong()
                                }
                            }
                            if (bRatings) {
                                Log.i(LOGTAG, "Reporting ratings")
                                if (AnodeClient.httpPostRating().contains("Error")) {
                                    timetosleep = 60000
                                } else {
                                    with(prefs.edit()) {
                                        putLong("LastRatingSubmitted", java.lang.System.currentTimeMillis())
                                        commit()
                                    }
                                    timetosleep = (60000 * 60 * 24).toLong()
                                }
                            }
                            Thread.sleep(timetosleep)
                        }
                    }
                    Thread.sleep((60 * 60000).toLong())
                }
            }
        }, "MainActivity.UploadEventsThread").start()
        //Check for uploading Errors
        Thread({
            Log.i(LOGTAG, "MainActivity.UploadErrorsThread startup")
            while (true) {
                AnodeClient.ignoreErr {
                    val erCount = AnodeClient.errorCount(this)
                    if (erCount == 0) {
                        // Wait for errors for 30 seconds
                        Log.d(LOGTAG, "No errors to report, sleeping")
                        Thread.sleep(30000)
                    } else if (!AnodeClient.checkNetworkConnection()) {
                        // try again in a second, waiting for internet
                        Log.i(LOGTAG, "Waiting for internet connection to report $erCount errors")
                        Thread.sleep(1000)
                    } else {
                        Log.i(LOGTAG, "Reporting a random error out of $erCount")
                        if (AnodeClient.httpPostError(application.filesDir).contains("Error")) {
                            // There was an error posting, lets wait 1 minute so as not to generate
                            // tons of crap
                            Log.i(LOGTAG, "Error reporting error, sleep for 60 seconds")
                            Thread.sleep(60000)
                        }
                    }
                }
            }
        }, "MainActivity.UploadErrorsThread").start()
        //Check for updates every 5min
        Thread({
            Log.i(LOGTAG, "MainActivity.CheckUpdates")
            while (true) {
                AnodeClient.checkNewVersion(false)
                if (AnodeClient.downloadFails > 1) {
                    //In case of >1 failure delete old apk files and retry after 20min
                    AnodeClient.deleteFiles(application?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString(), ".apk")
                    Thread.sleep((20 * 60000).toLong())
                } else if (AnodeClient.downloadingUpdate) {
                    Thread.sleep((20 * 60000).toLong())
                } else {
                    //check for new version every 5min
                    Thread.sleep((5 * 60000).toLong())
                }
            }
        }, "MainActivity.CheckUpdates").start()
    }

    fun exception(paramThrowable: Throwable) {
        //Toast message before exiting app
        var type = "other"
        if (paramThrowable.toString().contains("CjdnsException") ) type = "cjdnsSocket"
        else if (paramThrowable.toString().contains("AnodeUtilException") ) type = "cjdroute"
        else if (paramThrowable.toString().contains("AnodeVPNException") ) type = "vpnService"
        else if (paramThrowable.toString().contains("LndRPCException") ) {
            type = "lnd"
            AnodeClient.storeFileAsError(application, type, "data/data/co.anode.anodium/files/pld.log")
        }
        // we'll post the error on next startup
        AnodeClient.storeError(application, type, paramThrowable)

        object : Thread() {
            override fun run() {
                Looper.prepare()
                Toast.makeText(baseContext, "ERROR: " + paramThrowable.message, Toast.LENGTH_LONG).show()
                AnodeClient.mycontext = baseContext
                Looper.loop()
            }
        }.start()
        try {
            // Let the Toast display and give some time to post to server before app will get shutdown
            Thread.sleep(10000)
        } catch (e: InterruptedException) {}
        exitProcess(1)
    }

    fun initNotifications() {
        //Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelID = "anodium_channel_01"
            val name = "anodium_channel"
            val descriptionText = "Anodium channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun startVPNService() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
            //Get list of peering lines and add them as peers
            AnodeClient.GetPeeringLines().execute()
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        //Disable night mode (dark mode)
        //AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        prefs.edit().putBoolean("lndwalletopened", false).apply()
        AnodeUtil.init(applicationContext)
        AnodeClient.mycontext = applicationContext
        //Error Handling
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable -> //Catch your exception
            exception(paramThrowable)
        }

        /*
        with (prefs.edit()) {
            putString("walletpassword", "pktwallet")
            putString("lndwalletaddress", "")
            putString("username","")
            putBoolean("Registered",false)
            putBoolean("SignedIn",false)
            commit()
        }
       */

        //Start the log file
        AnodeUtil.logFile()
        //Initialize App
        AnodeUtil.initializeApp()
        AnodeUtil.launch()

        AnodeClient.statustv = findViewById(R.id.textview_status)
        AnodeClient.connectButton = findViewById(R.id.buttonconnectvpns)
        AnodeClient.mainActivity = this
        val minClickInterval: Long = 1000
        var mLastClickTime: Long = 0
        val buttonConnectVPNs = findViewById<ToggleButton>(R.id.buttonconnectvpns)
        buttonConnectVPNs.setOnClickListener() {
            AnodeUtil.preventTwoClick(it)
            //avoid accidental double clicks
            if (SystemClock.elapsedRealtime() - mLastClickTime > minClickInterval) {
                mLastClickTime = SystemClock.elapsedRealtime()
                if (!buttonConnectVPNs.isChecked) {
                    disconnectVPN(true)
                } else {
                    AnodeClient.AuthorizeVPN().execute(prefs.getString("LastServerPubkey", "hsrk7rrwssgpzv7jqxv95wmnx9c435s8jtf0k0w7v4rupymdj9k0.k"))
                    bigbuttonState(buttonStateConnecting)
                }
            }
        }
        val buttonVPNList = findViewById<Button>(R.id.buttonVPNList)
        buttonVPNList.setOnClickListener() {
            val vpnListActivity = Intent(this, VpnListActivity::class.java)
            startActivityForResult(vpnListActivity, 0)
        }
        //Starting VPN Service
        startVPNService()
        initNotifications()
        //Delete old APK files
        AnodeClient.deleteFiles(this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString(), ".apk")
        AnodeClient.downloadFails = 0
        val status = findViewById<TextView>(R.id.textview_status)
        AnodeClient.init(applicationContext, status, buttonConnectVPNs, this)
        //Get storage permission for downloading APK
        checkStoragePermission()
        //Start background threads for checking public IP, new version, uploading errors etc
        startBackgroundThreads()
        //Initialize VPN connecting waiting dialog
        VpnConnectionWaitingDialog.init(h, this@MainActivity)
    }

    private fun bigbuttonState(state: Int) {
        val status = findViewById<TextView>(R.id.textview_status)
        val buttonconnectvpns = findViewById<ToggleButton>(R.id.buttonconnectvpns)
        when(state) {
            buttonStateDisconnected -> {
                AnodeClient.eventLog("Main button status DISCONNECTING")
                h.removeCallbacks(VpnConnectionWaitingDialog)
                //Status bar
                status.text = ""
                //Show disconnected on toast so it times out
                Toast.makeText(applicationContext, getString(R.string.status_disconnected), Toast.LENGTH_LONG).show()
                //Button
                buttonconnectvpns.alpha = 1.0f
            }
            buttonStateConnecting -> {
                AnodeClient.eventLog("Main button status for CONNECTING")
                //Start 30sec timer
                h.postDelayed(VpnConnectionWaitingDialog, vpnConnectionWaitingInterval)
                status.text = resources.getString(R.string.status_connecting)
                buttonconnectvpns.text = getString(R.string.button_cancel)
                buttonconnectvpns.alpha = 0.5f
            }
            buttonStateConnected -> {
                AnodeClient.eventLog("Main button status for CONNECTED")
                h.removeCallbacks(VpnConnectionWaitingDialog)
                status.text = ""
                buttonconnectvpns.alpha = 1.0f
                /*val toast = Toast.makeText(applicationContext, getString(R.string.status_connected), Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 200)
                toast.show()*/
                AnodeClient.connectButton.isChecked = true
                status.text = resources.getString(R.string.status_connected)
            }
        }
    }

    fun setUsernameTopBar() {
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        val signedin = prefs.getBoolean("SignedIn", false)
        val topUsername: TextView = findViewById(R.id.top_username)
        if (signedin) {
            val username = prefs.getString("username", "")
            topUsername.text = username
            //Remove sign in and sign up from menu
            if (mainMenu != null) {
                mainMenu!!.findItem(R.id.action_signin).setVisible(false)
                //mainMenu!!.findItem(R.id.action_account_settings).setVisible(false)
                mainMenu!!.findItem(R.id.action_logout).setVisible(true)
                //mainMenu!!.findItem(R.id.action_deleteaccount).setVisible(true)
            }
            //Set username on title
            //this.title = "Anodium - $username"
        } else {
            topUsername.text = ""
            //Add sign in and sing up to menu
            if (mainMenu != null) {
                mainMenu!!.findItem(R.id.action_signin).setVisible(true)
                //mainMenu!!.findItem(R.id.action_account_settings).setVisible(true)
                mainMenu!!.findItem(R.id.action_logout).setVisible(false)
                //mainMenu!!.findItem(R.id.action_deleteaccount).setVisible(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnodeClient.eventLog("Resume MainActivity")
        AnodeClient.statustv = findViewById(R.id.textview_status)
        uiInForeground = true
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        val signedin = prefs.getBoolean("SignedIn", false)
        val nickname_backpressed = prefs.getBoolean("NicknameActivity_BackPressed", false)
        val signin_backpressed = prefs.getBoolean("SignInActivity_BackPressed", false)
        //Exit app if user is not signed in
        if (!signedin and (nickname_backpressed or signin_backpressed)) {
            with(prefs.edit()) {
                putBoolean("NicknameActivity_BackPressed", false)
                putBoolean("SignInActivity_BackPressed", false)
                commit()
            }
            // User may have pressed back without signing in.
            // Notify user that they need to sign in or exit the app
            if (prefs.getString("username", "").isNullOrEmpty()) {
                val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder.setTitle("Sign in")
                builder.setMessage("Please sign in to use the application")
                builder.setPositiveButton("OK") { dialog, _ ->
                    startActivity(accountNicknameActivity)
                    dialog.dismiss()
                }
                builder.setNegativeButton("Exit") { dialog, _ ->
                    dialog.dismiss()
                    //Close app
                    finishAffinity()
                    exitProcess(0)
                }
                val alert: AlertDialog = builder.create()
                alert.show()
            } else {
                //Close app
                finishAffinity()
                exitProcess(0)
            }
        }
        setUsernameTopBar()

        if (mainMenu != null) {
            //Show/Hide Registration on menu
            mainMenu!!.findItem(R.id.action_account_settings).isVisible = !prefs.getBoolean("Registered", false)
        }
        //Set button to correct status
        val status = findViewById<TextView>(R.id.textview_status)
        status.text = ""
        if (AnodeClient.isVpnActive()) {
            bigbuttonState(buttonStateConnected)
        }
    }

    override fun onStart() {
        super.onStart()
        uiInForeground = true
        AnodeClient.eventLog("Application launched")
    }

    override fun onPause() {
        super.onPause()
        uiInForeground = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        mainMenu = menu
        setUsernameTopBar()
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
// automatically handle clicks on the Home/Up button, so long
// as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_account_settings) {
            Log.i(LOGTAG, "Start registration")
            val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
            if (prefs.getString("username", "").isNullOrEmpty()) {
                val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
                startActivity(accountNicknameActivity)
            } else {
                val accountMainActivity = Intent(applicationContext, AccountMainActivity::class.java)
                startActivityForResult(accountMainActivity, 0)
            }
            return true
        } else if (id == R.id.action_signin) {
            Log.i(LOGTAG, "Start sign in activity")
            val signInActivity = Intent(applicationContext, SignInActivity::class.java)
            startActivity(signInActivity)
            return true
        } else if (id == R.id.action_checkupdates) {
            Toast.makeText(baseContext, "Checking for newer Anodium version...", Toast.LENGTH_LONG).show()
            AnodeClient.checkNewVersion(true)
            return true
        } else if (id == R.id.action_debug) {
            Log.i(LOGTAG, "Start debug activity")
            val debugActivity = Intent(applicationContext, DebugActivity::class.java)
            startActivity(debugActivity)
            return true
        } else if (id == R.id.action_logout) {
            Log.i(LOGTAG, "Log out")
            AnodeClient.eventLog("Menu: Log out selected")
            AnodeClient.LogoutUser().execute()
            //On Log out start sign in activity
            //val signinActivity = Intent(AnodeClient.mycontext, SignInActivity::class.java)
            //startActivity(signinActivity)
            return true
        } /*else if (id == R.id.action_deleteaccount) {
            Log.i(LOGTAG, "Delete account")
            AnodeClient.eventLog(baseContext, "Menu: Delete account selected")
            AnodeClient.DeleteAccount().execute()
            return true
        }*/ else if (id == R.id.action_changepassword) {
            Log.i(LOGTAG, "Change password")
            val changePassActivity = Intent(applicationContext, ChangePasswordActivity::class.java)
            changePassActivity.putExtra("ForgotPassword", false)
            startActivity(changePassActivity)
            return true
        } else if (id == R.id.action_wallet) {
            Log.i(LOGTAG, "Open wallet activity")
            val walletActivity = Intent(applicationContext, WalletActivity::class.java)
            startActivity(walletActivity)
            return true
        } else if (id == R.id.action_closeapp) {
            closeApp()
            return true
        } else {
            super.onOptionsItemSelected(item)
            return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == RESULT_OK) {
            Log.i(LOGTAG, "onActivityResult")
            if(data != null ) {
                //Connecting to VPN Server
                if (data.getStringExtra("action") == "connect") {
                    Log.i(LOGTAG, "Connecting to " + data.getStringExtra("publickey"))
                    AnodeClient.AuthorizeVPN().execute(data.getStringExtra("publickey"))
                    bigbuttonState(buttonStateConnecting)
                }
            } else {
                //Initialize CJDNS socket
                CjdnsSocket.init(AnodeUtil.CJDNS_PATH + "/" + AnodeUtil.CJDROUTE_SOCK)
            }
        }
        //On first run show nickname activity
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        //If there is no username stored
        if (prefs.getString("username", "").isNullOrEmpty()) {
            val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
            startActivity(accountNicknameActivity)
        } else if (!prefs.getBoolean("SignedIn", false)) {
            val signInActivity = Intent(applicationContext, SignInActivity::class.java)
            startActivity(signInActivity)
        }
    }

    private fun checkStoragePermission() {
        // Check if the storage permission has been granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            //Do nothing
        } else {
            // Permission is missing
            Log.i(co.anode.anodium.LOGTAG, "Missing permission WRITE_EXTERNAL_STORAGE")
            requestStoragePermission()
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), myPermissionsRequestWriteExternal)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), myPermissionsRequestWriteExternal)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == myPermissionsRequestWriteExternal) {
            if ((grantResults.isNotEmpty()) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                //Launch cjdroute & pltd
                //anodeUtil!!.launch()
            } else {
                Toast.makeText(baseContext, "Normal operation of Anodium can not proceed without needed permissions", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun internetConnection(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return if (activeNetwork?.isConnected == null)
            false
        else
            activeNetwork.isConnected
    }

    private fun disconnectVPN(showRatingBar: Boolean) {
        AnodeClient.AuthorizeVPN().cancel(true)
        AnodeClient.stopThreads()
        CjdnsSocket.IpTunnel_removeAllConnections()
        CjdnsSocket.Core_stopTun()
        CjdnsSocket.clearRoutes()
        startService(Intent(this, AnodeVpnService::class.java).setAction("co.anode.anodium.STOP"))
        bigbuttonState(buttonStateDisconnected)
        //Rating bar
        if (showRatingBar) {
            val ratingFragment: BottomSheetDialogFragment = RatingFragment()
            ratingFragment.show(supportFragmentManager, "")
        }
    }

    private fun getPublicIPv4(): String {
        val getURL = "http://v4.vpn.anode.co/api/0.3/vpn/clients/ipaddress/"
        val url = URL(getURL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 3000
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.requestMethod = "GET"
        try {
            conn.connect()
        } catch (e: java.lang.Exception) {
            return "None"
        }
        return try {
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            json.getString("ipAddress")
        } catch (e: Exception) {
            "None"
        }
    }

    private fun getPublicIPv6(): String {
        val getURL = "http://v6.vpn.anode.co/api/0.3/vpn/clients/ipaddress/"
        val url = URL(getURL)
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 3000
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.requestMethod = "GET"
        try {
            conn.connect()
        } catch (e: java.lang.Exception) {
            return "None"
        }
        return try {
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            json.getString("ipAddress")
        } catch (e: Exception) {
            "None"
        }
    }

    private fun closeApp() {
        Log.d(LOGTAG, "Closing anodium application")
        disconnectVPN(false)
        finish()
        exitProcess(0)
    }


    object VpnConnectionWaitingDialog: Runnable {
        private lateinit var h: Handler
        private var c: Context? = null

        fun init(handler: Handler, context: Context)  {
            h = handler
            c = context
        }

        override fun run() {
            h.removeCallbacks(VpnConnectionWaitingDialog)
            if (!AnodeClient.vpnConnected) {
                if (c!=null) {
                    val builder: AlertDialog.Builder = AlertDialog.Builder(c!!)
                    builder.setTitle("VPN Connecting")
                    builder.setMessage("Taking a long time to connect, VPN server may not be working.")
                    builder.setPositiveButton("Keep waiting") { dialog, _ ->
                        dialog.dismiss()
                    }
                    builder.setNegativeButton("Disconnect") { dialog, _ ->
                        AnodeClient.AuthorizeVPN().cancel(true)
                        AnodeClient.stopThreads()
                        CjdnsSocket.IpTunnel_removeAllConnections()
                        CjdnsSocket.Core_stopTun()
                        CjdnsSocket.clearRoutes()
                        c!!.startService(Intent(c!!, AnodeVpnService::class.java).setAction("co.anode.anodium.STOP"))
                        dialog.dismiss()
                    }
                    val alert: AlertDialog = builder.create()
                    alert.show()
                }
            }
        }
    }
}