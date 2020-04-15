package com.anode.anode

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import java.io.*

class MainActivity : AppCompatActivity() {

    private var util: AnodeUtil? = null

    private fun InitializeApp() {
        //Create files folder
        application.filesDir.mkdir()
        //Copy cjdroute
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            val am = baseContext.assets
            //Read architecture
            val arch = System.getProperty("os.arch")
            Log.i(LOGTAG, "OS Architecture: $arch")
            `in` = if (arch!!.contains("x86") || arch.contains("i686")) {
                am.open("x86/cjdroute")
            } else if (arch.contains("arm64-v8a")) {
                am.open("arm64-v8a/cjdroute")
            } else if (arch.contains("armeabi")) {
                am.open("armeabi-v7a/cjdroute")
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
        //Create and initialize conf file
        util!!.initializeCjdrouteConfFile()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        var buttonStartTest: FloatingActionButton? = null

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        util = AnodeUtil()
        val prefs = getSharedPreferences("com.anode.anode", Context.MODE_PRIVATE)

        if (prefs.getBoolean("firstrun", true)) {
            InitializeApp()
            prefs.edit().putBoolean("firstrun", false).commit()
        }
        buttonStartTest = findViewById(R.id.button_start_test)

        buttonStartTest.setOnClickListener(View.OnClickListener { view ->
            Snackbar.make(view, "Launching cjdroute...", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            val intent = VpnService.prepare(applicationContext)
            if (intent != null) {
                startActivityForResult(intent, 0)
            } else {
                onActivityResult(0, Activity.RESULT_OK, null)
            }
        })
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

    fun showMessageSnackbar(msg: String?) {
        var buttonStartTest: FloatingActionButton? = null
        val view = findViewById<View>(buttonStartTest!!.id)
        Snackbar.make(view, msg!!, Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
    }

    companion object {
        private const val LOGTAG = "MainActivity"
    }
}