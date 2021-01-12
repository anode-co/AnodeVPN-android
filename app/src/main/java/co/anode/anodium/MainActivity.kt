package co.anode.anodium

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.VpnService
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.json.JSONObject
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.SocketTimeoutException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLHandshakeException
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private var anodeUtil: AnodeUtil? = null
    private var mainMenu: Menu? = null

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
                Looper.prepare();
                Toast.makeText(baseContext, "ERROR: " + paramThrowable.message, Toast.LENGTH_LONG).show()
                AnodeClient.mycontext = baseContext
                Looper.loop();
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
        setSupportActionBar(toolbar);
        //Disable night mode (dark mode)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        anodeUtil = AnodeUtil(application)
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
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
        //Launch cjdroute
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
                    AnodeClient.AuthorizeVPN().execute(prefs.getString("LastServerPubkey", "cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k"))
                }
            }
        }


        buttonVPNList.setOnClickListener() {
            val vpnlistactivity = Intent(applicationContext, VpnListActivity::class.java)
            startActivity(vpnlistactivity)
        }

        val intent = VpnService.prepare(applicationContext)
        //Connect to CJDNS
        //startService(Intent(this, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))

        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, Activity.RESULT_OK, null)
            //Add peers
            CjdnsSocket.UDPInterface_beginConnection()
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
                if (internetConnection() == false) {
                    runOnUiThread {
                        val toast = Toast.makeText(applicationContext, getString(R.string.toast_no_internet), Toast.LENGTH_LONG)
                        toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 200)
                        toast.show()
                    }
                }
                Thread.sleep(3000)
            }
        }, "MainActivity.CheckInternetConnectivity").start()

        //Get public IP
        Thread(Runnable {
            while (true) {
                if (internetConnection() == true) {
                    val textPublicIP = findViewById<TextView>(R.id.publicip)
                    val publicip = GetPublicIP()
                    runOnUiThread {
                        textPublicIP.text = publicip
                    }
                }
                Thread.sleep(10000)
            }
        }, "MainActivity.GetPublicIP").start()

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
    }

    fun setUsernameTopBar() {
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
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
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
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
        status.setBackgroundColor(0x00000000)
        status.text = ""
        buttonconnectvpns.isChecked = AnodeClient.isVpnActive()
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
            val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
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
    if (resultCode == Activity.RESULT_OK) {
            //Initialize CJDNS socket
            CjdnsSocket.init(anodeUtil!!.CJDNS_PATH + "/" + anodeUtil!!.CJDROUTE_SOCK)
        }
        //On first run show nickname activity
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
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
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        return if (activeNetwork?.isConnected == null)
            false
        else
            activeNetwork.isConnected
    }

    fun disconnectVPN(showRatingBar: Boolean) {
        AnodeClient.AuthorizeVPN().cancel(true)
        AnodeClient.stopThreads()
        val status = findViewById<TextView>(R.id.textview_status)
        status.setBackgroundColor(0xFFFF0000.toInt())
        status.text = "VPN disconnected"
        CjdnsSocket.IpTunnel_removeAllConnections()
        CjdnsSocket.Core_stopTun()
        CjdnsSocket.clearRoutes()
        startService(Intent(this, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))

        //Rating bar
        if (showRatingBar) {
            val ratingFragment: BottomSheetDialogFragment = RatingFragment()
            ratingFragment.show(supportFragmentManager,"")
        }
    }

    fun GetPublicIP(): String {
        var result = ""
        var v4ip = ""
        var v6ip = ""
        val getv4URL = "https://v4.vpn.anode.co/api/0.3/vpn/clients/ipaddress/"
        val getv6URL = "https://v6.vpn.anode.co/api/0.3/vpn/clients/ipaddress/"
        val getAddressURL = "https://h.vpn.anode.co/api/0.3/vpn/clients/ipaddress/"

        val urlv4 = URL(getv4URL)
        val urlv6 = URL(getv6URL)
        val connv4 = urlv4.openConnection() as HttpsURLConnection

        connv4.connectTimeout = 2000
        connv4.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connv4.requestMethod = "GET"
        try {
            connv4.connect()
        } catch (e: SocketTimeoutException) {
            v4ip = "Error getting public IP"
        } catch (e: SSLHandshakeException) {
            v4ip = "Error getting public IP"
        }
        try {
            val json = JSONObject(connv4.inputStream.bufferedReader().readText())
            //json.getInt("version")
            v4ip = json.getString("ipAddress")
        } catch (e: Exception) {
            v4ip = "Error getting public IP"
        }

        val connv6 = urlv6.openConnection() as HttpsURLConnection
        connv6.connectTimeout = 2000
        connv6.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connv6.requestMethod = "GET"
        try {
            connv6.connect()
        } catch (e: SocketTimeoutException) {
            v6ip = "Error getting public IP"
        } catch (e: SSLHandshakeException) {
            v6ip = "Error getting public IP"
        }
        try {
            val json = JSONObject(connv6.inputStream.bufferedReader().readText())
            //json.getInt("version")
            v6ip = json.getString("ipAddress")
        } catch (e: Exception) {
            v6ip = "Error getting public IP"
        }
        result = baseContext.resources.getString(R.string.text_publicip)+ " v4: "+v4ip+"\n"+baseContext.resources.getString(R.string.text_publicip)+" v6: "+v6ip
        return result
    }

    fun closeApp() {
        Log.d(LOGTAG, "Closing anodium application")
        disconnectVPN(false)
        finish()
        System.exit(0)
    }
}

