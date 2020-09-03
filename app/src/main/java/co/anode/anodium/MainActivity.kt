package co.anode.anodium

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.content_main.*
import java.io.IOException
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import java.net.Socket
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
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        anodeUtil = AnodeUtil(application)
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        //Error Handling
        Thread.setDefaultUncaughtExceptionHandler { _, paramThrowable -> //Catch your exception
            exception(paramThrowable)
        }

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
        //Get Public Key ID for API Authorization
        //AnodeClient.httpPostPubKeyRegistration("test","test")
        /*
        button.setOnClickListener {
            Toast.makeText(baseContext, R.string.check_update, Toast.LENGTH_LONG).show()
            AnodeClient.checkNewVersion()
        }
        */
        /*
        buttonlistvpns.setOnClickListener {
            val vpnListActivity = Intent(applicationContext, VpnListActivity::class.java)
            startActivity(vpnListActivity)
        }*/

        buttonconnectvpns.setOnClickListener() {
            checked {
                val status: TextView = findViewById(R.id.textview_status)
                if (buttonconnectvpns.text == "CONNECT") {
                    //CjdnsSocket.init(anodeUtil!!.CJDNS_PATH + "/" + anodeUtil!!.CJDROUTE_SOCK)
                    status.text = "VPN Connecting..."
                    AnodeClient.AuthorizeVPN().execute("cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
                    buttonconnectvpns.text = "DISCONNECT"
                } else {
                    disconnectVPN()
                }
            }
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
                } else {
                    //check for new version every 5min
                    Thread.sleep(5 * 60000)
                }
            }
        }, "MainActivity.CheckUpdates").start()

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
                mainMenu!!.findItem(R.id.action_account_settings).setVisible(false)
                mainMenu!!.findItem(R.id.action_logout).setVisible(true)
            }
            //this.title = "Anodium - $username"
        } else {
            topUsername.text = ""
            //Add sign in and sing up to menu
            if (mainMenu != null) {
                mainMenu!!.findItem(R.id.action_signin).setVisible(true)
                mainMenu!!.findItem(R.id.action_account_settings).setVisible(true)
                mainMenu!!.findItem(R.id.action_logout).setVisible(false)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        setUsernameTopBar()
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
            Log.i(LOGTAG, "Start nickname activity")
            val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
            startActivity(accountNicknameActivity)
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
            AnodeClient.LogoutUser().execute()
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
        if (prefs.getBoolean("FirstRun", true)) {
            Log.i(LOGTAG, "First run: Start nickname activity")
            with(prefs.edit()) {
                putBoolean("FirstRun", false)
                commit()
            }
            val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
            startActivity(accountNicknameActivity)
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

    class checkInternetConnection(): AsyncTask<TextView, Any?, Boolean?>() {
        var textConnectivity: TextView? = null

        override fun onPreExecute() {
            super.onPreExecute()

        }
        override fun doInBackground(vararg params: TextView?): Boolean? {
            textConnectivity = params[0]
            try {
                val sock = Socket()
                sock.connect(InetSocketAddress("8.8.8.8", 53), 1500)
                sock.close()
                return true
            } catch (e: IOException) {
                return false
            }
        }

        override fun onPostExecute(result: Boolean?) {
            super.onPostExecute(result)
            if (result!!)
                textConnectivity?.post(Runnable {
                    textConnectivity?.text = "Connected"
                    textConnectivity?.setBackgroundColor(0xFF00FF00.toInt())
                })
            else
                textConnectivity?.post(Runnable {
                    textConnectivity?.text = "Disconnected"
                    textConnectivity?.setBackgroundColor(0xFFFF0000.toInt())
                })
        }

    }

    fun disconnectVPN() {
        AnodeClient.AuthorizeVPN().cancel(true)
        AnodeClient.stopThreads()
        val status = findViewById<TextView>(R.id.textview_status)
        status.setBackgroundColor(0xFFFF0000.toInt())
        status.text = "VPN disconnected"
        buttonconnectvpns.text = "CONNECT"
        CjdnsSocket.Core_stopTun()
        CjdnsSocket.clearRoutes()
        startService(Intent(this, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
    }
}

