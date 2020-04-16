package com.anode.anode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.text.HtmlCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import org.w3c.dom.Text
import java.io.*


class MainActivity : AppCompatActivity() {

    private var util: AnodeUtil? = null

    private fun initializeApp() {
        //Create files folder
        application.filesDir.mkdir()
        val cjdrouteFile = File(AnodeUtil().CJDNS_PATH+"/"+AnodeUtil().cjdrouteConfFile)
        if (!cjdrouteFile.exists()) {
            //Copy cjdroute
            var `in`: InputStream?
            var out: OutputStream?
            try {
                val am = baseContext.assets
                //Read architecture
                val arch = System.getProperty("os.arch")
                Log.i(LOGTAG, "OS Architecture: $arch")
                `in` = if (arch!!.contains("x86") || arch.contains("i686")) {
                    am.open("i686/16/cjdroute")
                } else if (arch.contains("arm64-v8a") || arch.contains("aarch64")) {
                    am.open("aarch64/21/cjdroute")
                } else if (arch.contains("armeabi") || arch.contains("armv7a")) {
                    am.open("armv7a/16/cjdroute")
                } else if (arch.contains("x86_64")) {
                    am.open("X86_64/21/cjdroute")
                } else { //Unknown architecture
                    Log.i(LOGTAG, "Incompatible CPU architecture")
                    return
                }
                out = FileOutputStream(application.filesDir.toString() + "/cjdroute")
                val buffer = ByteArray(1024)
                var read: Int
                while (`in`.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                `in`.close()
                out.close()
                //Set permissions
                val file = File(application.filesDir.toString() + "/cjdroute")
                file.setExecutable(true)
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(LOGTAG, "Failed to copy cjdroute file", e)
            }
        }
        //Create and initialize conf file
        if (!File(application.filesDir.toString()+"/"+AnodeUtil().cjdrouteConfFile).exists()) {
            util!!.initializeCjdrouteConfFile()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        util = AnodeUtil()
        val prefs = getSharedPreferences("com.anode.anode", Context.MODE_PRIVATE)

        //Initialize the app by copying cjdroute and generating the conf file
        initializeApp()

        /* We may need the first run check in the future... */
        /*
        if (prefs.getBoolean("firstrun", true)) {
            prefs.edit().putBoolean("firstrun", false).commit()
        } */
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
            startService(intent)
        }
    }

    companion object {
        private const val LOGTAG = "MainActivity"
    }
}