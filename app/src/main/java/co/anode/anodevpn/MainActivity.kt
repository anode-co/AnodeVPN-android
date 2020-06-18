package co.anode.anodevpn

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.content_main.*
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private var anodeUtil: AnodeUtil? = null
    companion object {
        private const val LOGTAG = "co.anode.anodevpn"
        private const val API_UPDATE_URL = "http://anode.co/assets/downloads/anode-vpn.apk"
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        anodeUtil = AnodeUtil(application)
        val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
        //Error Handling
        Thread.setDefaultUncaughtExceptionHandler { paramThread, paramThrowable -> //Catch your exception
            //Toast message before exiting app
            var type = "other"
            if (paramThrowable is CjdnsException) type = "cjdns_socket"
            else if (paramThrowable is AnodeUtilException) type = "cjdroute"
            else if (paramThrowable is AnodeVPNException) type = "VPNService"
            // we'll post the error on next startup
            AnodeClient.storeError(type, paramThrowable.message)

            object : Thread() {
                override fun run() {
                    Looper.prepare();
                    Toast.makeText(baseContext, "ERROR: "+paramThrowable.message, Toast.LENGTH_LONG).show()
                    AnodeClient.mycontext = baseContext
                    Log.e(LOGTAG,"Exception from "+paramThread.name, paramThrowable)
                    Looper.loop();
                }
            }.start()
            try {
                // Let the Toast display and give some time to post to server before app will get shutdown
                Thread.sleep(10000)
            } catch (e: InterruptedException) {}
            exitProcess(1)
        }

        //Start the log file
        anodeUtil!!.logFile()
        //Initialize App
        anodeUtil!!.initializeApp()
        //Launch cjdroute
        anodeUtil!!.launch()
        //Check for new version
        AnodeClient.mycontext = baseContext
        AnodeClient.status = findViewById(R.id.textview_status)

        //Get Public Key ID for API Authorization
        //AnodeClient.httpPostPubKeyRegistration("test","test")
        //Automatic update
        checkStoragePermission()
        AnodeClient.checkNewVersion()

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
            val status: TextView = findViewById(R.id.textview_status)
            if (buttonconnectvpns.text == "CONNECT") {
                status.text = "Connecting to VPN..."
                AnodeClient.AuthorizeVPN().execute("cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
                buttonconnectvpns.text = "DISCONNECT"
            } else {
                status.text = "Disconnect from VPN"
                disconnectVPN()
            }
        }

        val intent = VpnService.prepare(applicationContext)
        //Connect to CJDNS
        startService(Intent(this, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))

        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, Activity.RESULT_OK, null)
        }

        val pubkey: TextView = findViewById(R.id.textViewPubkey)
        pubkey.text = baseContext?.resources?.getString(R.string.public_key) +" "+ AnodeUtil(this).getPubKey()

        //Check for internet connectivity every 15 seconds
        val mHandler = Handler();
        val mHandlerTask: Runnable = object : Runnable {
            override fun run() {
                checkInternetConnection().execute(findViewById(R.id.textview_status))
                mHandler.postDelayed(this, 19000)
            }
        }
        mHandlerTask.run()

        val erHandler = Handler()
        val erHandlerTask: Runnable = object : Runnable {
            override fun run() {
                if (!AnodeClient.hasErrors()) {
                    // Wait for errors for 30 seconds
                    mHandler.postDelayed(this, 30000)
                    if (!AnodeClient.checkNetworkConnection()) {
                        // try again in a second, waiting for internet
                        mHandler.postDelayed(this, 1000)
                    } else if (AnodeClient.httpPostError()) {
                        // There was an error posting, lets wait 1 minut so as not to generate
                        // tons of crap
                        mHandler.postDelayed(this, 60000)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
// automatically handle clicks on the Home/Up button, so long
// as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        if (id == R.id.action_account_settings) {
            val accountNicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
            startActivity(accountNicknameActivity)
            return true
        } else if (id == R.id.action_signin) {
            val signinActivity = Intent(applicationContext, SignInActivity::class.java)
            startActivity(signinActivity)
            return true
        } else if (id == R.id.action_vpnlist) {
            val vpnListActivity = Intent(applicationContext, VpnListActivity::class.java)
            startActivity(vpnListActivity)
            return true
        } else {
            super.onOptionsItemSelected(item)
            return false
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == Activity.RESULT_OK) {
            //val intent = Intent(this, AnodeVpnService::class.java)
            //startService(intent)
            //Initialize CJDNS socket
            CjdnsSocket.init(anodeUtil!!.CJDNS_PATH + "/" + anodeUtil!!.CJDROUTE_SOCK)
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
            Log.i(co.anode.anodevpn.LOGTAG, "Missing permission WRITE_EXTERNAL_STORAGE")
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
                    textConnectivity?.text  = "Connected"
                    textConnectivity?.setBackgroundColor(0xFF00FF00.toInt())
                } )
            else
                textConnectivity?.post(Runnable {
                    textConnectivity?.text  = "Disconnected"
                    textConnectivity?.setBackgroundColor(0xFFFF0000.toInt())
                } )
        }

    }

    fun disconnectVPN() {
        buttonconnectvpns.text = "CONNECT"
        CjdnsSocket.clearRoutes()
        startService(Intent(this, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
    }
}

