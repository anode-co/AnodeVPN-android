package co.anode.anodevpn

import android.Manifest
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
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private var anodeUtil: AnodeUtil? = null
    companion object {
        private const val LOGTAG = "co.anode.anodevpn"
        private const val API_UPDATE_URL = "http://anode.co/assets/downloads/anode-vpn.apk"
    }

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
            object : Thread() {
                override fun run() {
                    Looper.prepare();
                    Toast.makeText(baseContext, "ERROR: "+paramThrowable.message, Toast.LENGTH_LONG).show()
                    AnodeClient.mycontext = baseContext
                    var type = "other"
                    //CJDNS socket error
                    if (paramThrowable is CjdnsException) type = "cjdns_socket"
                    else if (paramThrowable is AnodeUtilException) type = "cjdroute"
                    else if (paramThrowable is AnodeVPNException) type = "VPNService"
                    if (AnodeClient.checkNetworkConnection()){
                        //Trying to post error to server
                        AnodeClient.httpPostError(type, paramThrowable.message)
                    }
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

        //Get Public Key ID for API Authorization
        //AnodeClient.httpPostPubKeyRegistration("test","test")


        checkStoragePermission()
        button.setOnClickListener {
            Toast.makeText(baseContext, R.string.check_update, Toast.LENGTH_LONG).show()
            AnodeClient.checkNewVersion()
        }

        buttonlistvpns.setOnClickListener {
            val vpnListActivity = Intent(applicationContext, VpnListActivity::class.java)
            startActivity(vpnListActivity)
        }

        val intent = VpnService.prepare(applicationContext)

        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, Activity.RESULT_OK, null)
        }

        //Check for internet connectivity every 15 seconds
        val mHandler = Handler();
        val mHandlerTask: Runnable = object : Runnable {
            override fun run() {
                checkInternetConnection().execute(findViewById(R.id.textview_internetconnectivity))
                mHandler.postDelayed(this, 15000)
            }
        }
        mHandlerTask.run()
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
// automatically handle clicks on the Home/Up button, so long
// as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_login) {
            //val loginActivity = Intent(applicationContext, LoginActivity::class.java)
            //startActivity(loginActivity)
            true
        } else super.onOptionsItemSelected(item)
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
}