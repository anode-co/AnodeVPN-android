package co.anode.anodevpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.*


class MainActivity : AppCompatActivity() {

    private var anodeUtil: AnodeUtil? = null

    private fun initializeApp() {
        //Create files folder
        application.filesDir.mkdir()
        val cjdrouteFile = File(AnodeUtil().CJDNS_PATH+"/"+ AnodeUtil().CJDROUTE_BINFILE)

        //Read architecture
        val arch = System.getProperty("os.arch")
        var `in`: InputStream? = null
        var out: OutputStream?
        try {
            val am = baseContext.assets
            Log.i(LOGTAG, "OS Architecture: $arch")
            if (arch == "x86" || arch!!.contains("i686")) {
                `in` = am.open("i686/16/cjdroute")
            } else if (arch.contains("arm64-v8a") || arch.contains("aarch64")) {
                `in` = am.open("aarch64/21/cjdroute")
            } else if (arch.contains("armeabi") || arch.contains("armv7a")) {
                `in` = am.open("armv7a/16/cjdroute")
            } else if (arch.contains("x86_64")) {
                `in` = am.open("X86_64/21/cjdroute")
            } else { //Unknown architecture
                Log.i(LOGTAG, "Incompatible CPU architecture")
                return
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(LOGTAG, "Failed to open cjdroute file", e)
        }

        if (!cjdrouteFile.exists() ||
            arch!!.contains("i686") ||
            arch!!.contains("x86") ||
            arch!!.contains("X86_64")){
            //Copy cjdroute
            try {
                if (!cjdrouteFile.exists()) {
                    Log.i(LOGTAG,"cjdroute does not exists")
                }
                Log.i(LOGTAG,"Copying cjdroute")
                out = FileOutputStream(application.filesDir.toString() + "/cjdroute")
                val buffer = ByteArray(1024)
                var read: Int
                while (`in`!!.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                `in`!!.close()
                out.close()
                //Set permissions
                val file = File(application.filesDir.toString() + "/cjdroute")
                file.setExecutable(true)
            }catch (e: IOException) {
                e.printStackTrace()
                Log.e(LOGTAG, "Failed to copy cjdroute file", e)
            }
        }
        //Create and initialize conf file
        if (!File(application.filesDir.toString()+"/"+ AnodeUtil().cjdrouteConfFile).exists()) {
            anodeUtil!!.initializeCjdrouteConfFile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        anodeUtil = AnodeUtil()
        //val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)

        //Initialize the app by copying cjdroute and generating the conf file
        initializeApp()

        anodeUtil!!.launch()
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
// automatically handle clicks on the Home/Up button, so long
// as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val intent = Intent(this, AnodeVpnService::class.java)
            //startService(intent)
            //Initialize CJDNS socket
            CjdnsSocket.init(AnodeUtil().CJDNS_PATH + "/" + AnodeUtil().CJDROUTE_SOCK)
        }
    }

    companion object {
        private const val LOGTAG = "MainActivity"
    }
}