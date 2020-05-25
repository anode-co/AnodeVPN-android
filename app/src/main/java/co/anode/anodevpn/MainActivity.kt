package co.anode.anodevpn

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.File
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
        //val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
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
                        AnodeClient.httpPost(type, paramThrowable.message)
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

        checkStoragePermission()
        button.setOnClickListener {
            Toast.makeText(baseContext, R.string.check_update, Toast.LENGTH_LONG).show()
            AnodeClient.checkNewVersion()
        }

        buttonlistvpns.setOnClickListener {
            val vpnListActivity = Intent(applicationContext, VpnListActivity::class.java)
            startActivity(vpnListActivity)
        }
        /* We may need the first run check in the future... */
        /*
        if (prefs.getBoolean("firstrun", true)) {
            prefs.edit().putBoolean("firstrun", false).commit()
        } */
        val intent = VpnService.prepare(applicationContext)

        if (intent != null) {
            startActivityForResult(intent, 0)
        } else {
            onActivityResult(0, Activity.RESULT_OK, null)
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        return super.onCreateView(name, context, attrs)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
/*
    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
// automatically handle clicks on the Home/Up button, so long
// as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }
*/
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
    /*
    fun AppCompatActivity.checkSelfPermissionCompat(permission: String) =
            ActivityCompat.checkSelfPermission(this, permission)
    private fun checkStoragePermission() {
        // Check if the storage permission has been granted
        if (checkSelfPermissionCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        ) {
            // start downloading
            downloadFile( Uri.parse(API_UPDATE_URL))
        } else {
            // Permission is missing and must be requested.
            requestStoragePermission()
        }
    }

    fun AppCompatActivity.shouldShowRequestPermissionRationaleCompat(permission: String) =
            ActivityCompat.shouldShowRequestPermissionRationale(this, permission)

    private fun requestStoragePermission() {
        if (shouldShowRequestPermissionRationaleCompat(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 0)
        }
    }

    private fun downloadFile(uri: Uri): Long {
        var downloadReference: Long = 0
        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var destination = this.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += FILE_NAME
        val destinationuri = Uri.parse("$FILE_BASE_PATH$destination")
        try {
            val file = File(destination)
            if (file.exists()) file.delete()
            val request = DownloadManager.Request(uri)
            //Setting title of request
            request.setTitle(FILE_NAME)
            request.setMimeType("application/vnd.android.package-archive")
            //Setting description of request
            request.setDescription("Your file is downloading")
            //set notification when download completed
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            //Set the local destination for the downloaded file to a path within the application's external files directory
            //request.setDestinationInExternalPublicDir(destination, FILE_NAME) //WORKS
            request.setDestinationUri(destinationuri)
            //Enqueue download and save the referenceId
            downloadReference = downloadManager.enqueue(request)

            showInstallOption(destination, uri)
        } catch (e: IllegalArgumentException) {
            //BaseUtils.showToast(mContext, "Download link is broken or not availale for download")
            //Log.e(TAG, "Line no: 455,Method: downloadFile: Download link is broken")
        }
        return downloadReference
    }

    fun showInstallOption(
            destination: String,
            uri: Uri
    ) {
        // set BroadcastReceiver to install app when .apk is downloaded
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                    context: Context,
                    intent: Intent
            ) {
                val contentUri = FileProvider.getUriForFile(
                        context,
                        //BuildConfig.APPLICATION_ID + PROVIDER_PATH,
                        context.applicationContext.packageName +PROVIDER_PATH,
                        File(destination)
                )
                //val contentUri = Uri.parse("$FILE_BASE_PATH$destination")
                val install = Intent(Intent.ACTION_VIEW)
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                //install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                install.data = contentUri
                //install.type = "application/android.com.app"
                context.startActivity(install)
                context.unregisterReceiver(this)
                // finish()
            }
        }
        this.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

     */
}