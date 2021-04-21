package co.anode.anodium

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.VpnService
import android.os.*
import android.text.Html
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private val BUTTON_STATE_DISCONNECTED = 0
    private val BUTTON_STATE_CONNECTING = 1
    private val BUTTON_STATE_CONNECTED = 2
    private var anodeUtil: AnodeUtil? = null
    private var mainMenu: Menu? = null
    private var publicIpThreadSleep: Long = 10
    private var uiInForeground = true
    private var previousPublicIPv4 = ""
    val h = Handler()

    companion object {
        private const val LOGTAG = "co.anode.anodium"
    }

    fun exception(paramThrowable: Throwable) {
        //Toast message before exiting app
        var type = "other"
        if ((paramThrowable.cause as InvocationTargetException).targetException is CjdnsException) type = "cjdnsSocket"
        else if ((paramThrowable.cause as InvocationTargetException).targetException is AnodeUtilException) type = "cjdroute"
        else if ((paramThrowable.cause as InvocationTargetException).targetException is AnodeVPNException) type = "vpnService"
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

    private fun checked(l: () -> Unit) {
        try {
            l()
        } catch (t: Throwable) {
            exception(t)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        //Disable night mode (dark mode)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        anodeUtil = AnodeUtil(application)
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        //Error Handling
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable -> //Catch your exception
            exception(paramThrowable)
        }

        /*
        with (prefs.edit()) {
            putString("username","")
            putBoolean("Registered",false)
            putBoolean("SignedIn",false)
            commit()
        }
       */

        //Start the log file
        anodeUtil!!.logFile()
        //Initialize App
        anodeUtil!!.initializeApp()
        //Launch cjdroute & pltd
        anodeUtil!!.launch()
        //Initialize AnodeClient
        AnodeClient.mycontext = baseContext
        AnodeClient.statustv = findViewById(R.id.textview_status)
        AnodeClient.connectButton = findViewById(R.id.buttonconnectvpns)
        AnodeClient.mainActivity = this
        val MIN_CLICK_INTERVAL: Long = 1000
        var mLastClickTime: Long = 0

        buttonconnectvpns.setOnClickListener() {
            //avoid accidental double clicks
            if (SystemClock.uptimeMillis() - mLastClickTime > MIN_CLICK_INTERVAL) {
                mLastClickTime = SystemClock.uptimeMillis()
                if (!buttonconnectvpns.isChecked) {
                    disconnectVPN(true)
                } else {
                    AnodeClient.AuthorizeVPN().execute(prefs.getString("LastServerPubkey", "hsrk7rrwssgpzv7jqxv95wmnx9c435s8jtf0k0w7v4rupymdj9k0.k"))
                    bigbuttonState(BUTTON_STATE_CONNECTING)
                }
            }
        }

        buttonVPNList.setOnClickListener() {
            val vpnlistactivity = Intent(applicationContext, VpnListActivity::class.java)
            startActivityForResult(vpnlistactivity, 0)
        }

        val intent = VpnService.prepare(applicationContext)
        //Connect to CJDNS
        //startService(Intent(this, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))

        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, RESULT_OK, null)
            //Get list of peering lines and add them as peers
            AnodeClient.GetPeeringLines().execute()
        }

        //Create notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val CHANNEL_ID = "anodium_channel_01"
            val name = "anodium_channel"
            val descriptionText = "Anodium channel"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }


        // Removed public key from main
        // val pubkey: TextView = findViewById(R.id.textViewPubkey)
        //pubkey.text = baseContext?.resources?.getString(R.string.public_key) +" "+ AnodeUtil(this).getPubKey()

        //Check for internet connectivity every 15 seconds
        /* Disable internet connectivity check
        val mHandler = Handler();
        val mHandlerTask: Runnable = object : Runnable {
            override fun run() {
                checkInternetConnection().execute(findViewById(R.id.textview_status))
                mHandler.postDelayed(this, 19000)
            }
        }
        mHandlerTask.run()
        */
        Thread(Runnable {
            Log.i(LOGTAG, "MainActivity.UploadErrorsThread startup")
            while (true) {
                AnodeClient.ignoreErr {
                    val erCount = AnodeClient.errorCount(application)
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

        //Check internet connectivity & public IP
        Thread(Runnable {
            while (true) {
                if ((internetConnection() == false) && (uiInForeground)) {
                    runOnUiThread {
                        val toast = Toast.makeText(applicationContext, getString(R.string.toast_no_internet), Toast.LENGTH_LONG)
                        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 200)
                        toast.show()
                        //bigbuttonState(BUTTON_STATE_DISCONNECTED)
                    }
                }
                Thread.sleep(3000)
            }
        }, "MainActivity.CheckInternetConnectivity").start()

        //Get v4 public IP
        Thread(Runnable {
            while (true) {
                if ((internetConnection() == true) && (uiInForeground)) {
                    val textPublicIP = findViewById<TextView>(R.id.v4publicip)
                    val publicip = GetPublicIPv4()
                    if (!publicip.contains("Error")) {
                        publicIpThreadSleep = 10000
                    }
                    runOnUiThread {
                        textPublicIP.text = Html.fromHtml("<b>" + baseContext.resources.getString(R.string.text_publicipv4) + "</b>&nbsp;" + publicip)
                        if ((AnodeClient.vpnConnected) &&
                                (previousPublicIPv4 != publicip) &&
                                ((publicip != "None") || (!publicip.contains("Error")))) {
                            bigbuttonState(BUTTON_STATE_CONNECTED)
                        }
                    }
                    previousPublicIPv4 = publicip
                }
                Thread.sleep(publicIpThreadSleep)
            }
        }, "MainActivity.GetPublicIPv4").start()

        //Get v6 public IP
        Thread(Runnable {
            while (true) {
                if ((internetConnection() == true) && (uiInForeground)) {
                    val textPublicIP = findViewById<TextView>(R.id.v6publicip)
                    val publicip = GetPublicIPv6()
                    if (!publicip.contains("Error")) {
                        publicIpThreadSleep = 10000
                    }
                    runOnUiThread {
                        textPublicIP.text = Html.fromHtml("<b>" + baseContext.resources.getString(R.string.text_publicipv6) + "</b>&nbsp;" + publicip)
                        /*if (AnodeClient.vpnConnected) {
                            bigbuttonState(BUTTON_STATE_CONNECTED)
                        }*/
                    }
                }
                Thread.sleep(publicIpThreadSleep)
            }
        }, "MainActivity.GetPublicIPv4").start()

        //Check for event log files daily
        Thread(Runnable {
            Log.i(LOGTAG, "MainActivity.UploadEventsThread startup")
            while (true) {
                AnodeClient.ignoreErr {
                    //Check if 24 hours have passed since last log file submitted
                    if ((System.currentTimeMillis() - prefs.getLong("LastEventLogFileSubmitted", 0) > 86400000) or
                            (System.currentTimeMillis() - prefs.getLong("LastRatingSubmitted", 0) > 86400000)) {
                        val bEvents = File(applicationContext.filesDir.toString() + "/anodium-events.log").exists()
                        val bRatings = File(applicationContext.filesDir.toString() + "/anodium-rating.json").exists()
                        var timetosleep: Long = 60000
                        if ((!bEvents) or (!bRatings)) {
                            Log.d(LOGTAG, "No events or ratings to report, sleeping")
                            Thread.sleep(60 * 60000)
                        } else if (!AnodeClient.checkNetworkConnection()) {
                            // try again in 10 seconds, waiting for internet
                            Log.i(LOGTAG, "Waiting for internet connection to report events")
                            Thread.sleep(10000)
                        } else {
                            if (bEvents) {
                                Log.i(LOGTAG, "Reporting an events log file")
                                if (AnodeClient.httpPostEvent(application.filesDir).contains("Error")) {
                                    timetosleep = 60000
                                } else {
                                    //Log posted, sleep for a day
                                    timetosleep = 60000 * 60 * 24
                                }
                            }
                            if (bRatings) {
                                Log.i(LOGTAG, "Reporting ratings")
                                if (AnodeClient.httpPostRating().contains("Error")) {
                                    timetosleep = 60000
                                } else {
                                    with(prefs.edit()) {
                                        putLong("LastRatingSubmitted", System.currentTimeMillis())
                                        commit()
                                    }
                                    timetosleep = 60000 * 60 * 24
                                }
                            }
                            Thread.sleep(timetosleep)
                        }
                    }
                    Thread.sleep(60 * 60000)
                }
            }
        }, "MainActivity.UploadEventsThread").start()
        //Delete old APK files
        AnodeClient.deleteFiles(application?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString(), ".apk")
        AnodeClient.downloadFails = 0
        //Automatic update
        //Get storage permission for downloading APK
        checkStoragePermission()
        //Check for updates every 5min
        Thread(Runnable {
            Log.i(LOGTAG, "MainActivity.CheckUpdates")
            while (true) {
                AnodeClient.checkNewVersion(false)
                if (AnodeClient.downloadFails > 1) {
                    //In case of >1 failure delete old apk files and retry after 20min
                    AnodeClient.deleteFiles(application?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString(), ".apk")
                    Thread.sleep(20 * 60000)
                } else if (AnodeClient.downloadingUpdate) {
                    Thread.sleep(20 * 60000)
                } else {
                    //check for new version every 5min
                    Thread.sleep(5 * 60000)
                }
            }
        }, "MainActivity.CheckUpdates").start()

        AnodeClient.eventLog(baseContext, "Application launched")
        /*Settings for testing wallet
        prefs.edit().putBoolean("lndwallet", true).apply()
        prefs.edit().putString("walletpassword","password").apply()*/
        /*Settings for testing wallet creation*/
        //prefs.edit().putBoolean("lndwallet", false).apply()
        //prefs.edit().putString("walletpassword","").apply()
        if (prefs.getBoolean("lndwallet",false) ) {
            Log.i(LOGTAG, "MainActivity trying to open wallet")
            LndRPCController.openWallet(prefs)
        }
    }

    fun bigbuttonState(state: Int) {
        val status = findViewById<TextView>(R.id.textview_status)
        when(state) {
            BUTTON_STATE_DISCONNECTED -> {
                AnodeClient.eventLog(baseContext, "Main button status DISCONNECTING")
                //Status bar
                status.text = ""
                //Show disconnected on toast so it times out
                val toast = Toast.makeText(applicationContext, getString(R.string.status_disconnected), Toast.LENGTH_LONG)
                toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 200)
                toast.show()
                //Button
                buttonconnectvpns.alpha = 1.0f
                h.removeCallbacks(connectingDialog)
            }
            BUTTON_STATE_CONNECTING -> {
                AnodeClient.eventLog(baseContext, "Main button status for CONNECTING")
                //Start 30sec timer
                connectingDialog.init(h, this@MainActivity)
                h.postDelayed(connectingDialog, 30000)
                status.text = resources.getString(R.string.status_connecting)
                buttonconnectvpns.text = "Cancel"
                buttonconnectvpns.alpha = 0.5f
            }
            BUTTON_STATE_CONNECTED -> {
                AnodeClient.eventLog(baseContext, "Main button status for CONNECTED")
                h.removeCallbacks(connectingDialog)
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
        AnodeClient.eventLog(baseContext, "Resume MainActivity")
        AnodeClient.statustv = findViewById(R.id.textview_status)
        uiInForeground = true
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        val signedin = prefs.getBoolean("SignedIn", false)
        val nickname_backpressed = prefs.getBoolean("NicknameActivity_BackPressed", false)
        val signin_backpressed = prefs.getBoolean("SignInActivity_BackPressed", false)
        //Exit app if user is not signed in
        if (!signedin and (nickname_backpressed or signin_backpressed)) {
            //TODO: handle back when comes through other use cases
            with(prefs.edit()) {
                putBoolean("NicknameActivity_BackPressed", false)
                putBoolean("SignInActivity_BackPressed", false)
                commit()
            }
            //Close app
            finishAffinity()
            System.exit(0)
        }
        setUsernameTopBar()
        //Show/Hide Registration on menu
        if (mainMenu != null) {
            mainMenu!!.findItem(R.id.action_account_settings).setVisible(!prefs.getBoolean("Registered", false))
        }
        //Set button to correct status
        val status = findViewById<TextView>(R.id.textview_status)
        status.text = ""
        if (AnodeClient.isVpnActive()) {
            bigbuttonState(BUTTON_STATE_CONNECTED)
        }
    }

    override fun onStart() {
        super.onStart()
        uiInForeground = true
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
            val signinActivity = Intent(applicationContext, SignInActivity::class.java)
            startActivity(signinActivity)
            return true
        /*} else if (id == R.id.action_vpnlist) {
            Log.i(LOGTAG,"Start VPN list activity")
            val vpnListActivity = Intent(applicationContext, VpnListActivity::class.java)
            startActivity(vpnListActivity)
            return true
        } else if (id == R.id.action_submitlogs) {
            AnodeClient.mycontext = baseContext
            AnodeClient.PostLogs().execute()
            return true*/
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
            AnodeClient.eventLog(baseContext, "Menu: Log out selected")
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
            val changepassactivity = Intent(applicationContext, ChangePasswordActivity::class.java)
            changepassactivity.putExtra("ForgotPassword", false)
            startActivity(changepassactivity)
            return true
        } else if (id == R.id.action_wallet) {
            Log.i(LOGTAG, "Open wallet activity")
            val walletactivity = Intent(applicationContext, WalletActivity::class.java)
            startActivity(walletactivity)
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
                //Conecting to VPN Server
                if (data.getStringExtra("action") == "connect") {
                    Log.i(LOGTAG, "Connecting to " + data.getStringExtra("publickey"))
                    AnodeClient.AuthorizeVPN().execute(data.getStringExtra("publickey"))
                    bigbuttonState(BUTTON_STATE_CONNECTING)
                }
            } else {
                //Initialize CJDNS socket
                CjdnsSocket.init(anodeUtil!!.CJDNS_PATH + "/" + anodeUtil!!.CJDROUTE_SOCK)
            }
        }
        //On first run show nickname activity
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        /*
        if (prefs.getBoolean("FirstRun", true)) {
            Log.i(LOGTAG, "First run: Start nickname activity")
            with(prefs.edit()) {
                putBoolean("FirstRun", false)
                commit()
            }
            val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
            startActivity(accountNicknameActivity)
        }*/
        //If there is no username stored
        if (prefs.getString("username", "").isNullOrEmpty()) {
            val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
            startActivity(accountNicknameActivity)
        } else if (!prefs.getBoolean("SignedIn", false)) {
            val signinActivity = Intent(applicationContext, SignInActivity::class.java)
            startActivity(signinActivity)
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

    fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }
    }

    fun internetConnection(): Boolean? {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return if (activeNetwork?.isConnected == null)
            false
        else
            activeNetwork.isConnected
    }

    fun disconnectVPN(showRatingBar: Boolean) {
        AnodeClient.AuthorizeVPN().cancel(true)
        AnodeClient.stopThreads()
        CjdnsSocket.IpTunnel_removeAllConnections()
        CjdnsSocket.Core_stopTun()
        CjdnsSocket.clearRoutes()
        startService(Intent(this, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
        bigbuttonState(BUTTON_STATE_DISCONNECTED)
        //Rating bar
        if (showRatingBar) {
            val ratingFragment: BottomSheetDialogFragment = RatingFragment()
            ratingFragment.show(supportFragmentManager, "")
        }
    }

    fun GetPublicIPv4(): String {
        //return URL("http://ipv4bot.whatismyipaddress.com/").readText(Charsets.UTF_8)
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

    fun GetPublicIPv6(): String {
        //return URL("http://ipv6bot.whatismyipaddress.com/").readText(Charsets.UTF_8)
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
        try {
            val json = JSONObject(conn.inputStream.bufferedReader().readText())
            return json.getString("ipAddress")
        } catch (e: Exception) {
            return "None"
        }
    }

    fun closeApp() {
        Log.d(LOGTAG, "Closing anodium application")
        disconnectVPN(false)
        finish()
        System.exit(0)
    }

    @SuppressLint("StaticFieldLeak")
    object connectingDialog: Runnable {
        private var h: Handler? = null
        private var c: Context? = null

        fun init(handler: Handler, context: Context)  {
            h = handler
            c = context
        }
        override fun run() {
            h?.removeCallbacks(connectingDialog)
            //if (!AnodeClient.vpnConnected) {
            if (c!=null) {
                val builder: AlertDialog.Builder = AlertDialog.Builder(c!!)
                builder.setTitle("VPN Connecting")
                builder.setMessage("Taking a long time to connect, VPN server may not be working.")

                builder.setPositiveButton("Keep waiting", DialogInterface.OnClickListener { dialog, which ->
                    dialog.dismiss()
                })

                builder.setNegativeButton("Disconnect", DialogInterface.OnClickListener { dialog, which ->
                    //MainActivity().disconnectVPN(false)
                    AnodeClient.AuthorizeVPN().cancel(true)
                    AnodeClient.stopThreads()
                    CjdnsSocket.IpTunnel_removeAllConnections()
                    CjdnsSocket.Core_stopTun()
                    CjdnsSocket.clearRoutes()
                    c!!.startService(Intent(c!!, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                    dialog.dismiss()
                })
                val alert: AlertDialog = builder.create()
                alert.show()
            }
            //}
        }
    }

}
