package co.anode.anodevpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import java.io.*
import kotlin.system.exitProcess


class MainActivity : AppCompatActivity() {
    private var anodeUtil: AnodeUtil? = null

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
                    Toast.makeText(baseContext, paramThrowable.message, Toast.LENGTH_LONG).show()
                    AnodeClient.mycontext = baseContext
                    if (AnodeClient.checkNetworkConnection()){
                        //Trying to post error to server
                        val result = AnodeClient.httpPost("https://anode.co/api/error", "Error", paramThrowable.message)
                    }
                    Log.e(LOGTAG,"Exception from "+paramThread.name, paramThrowable)
                    Looper.loop();
                }
            }.start()
            try {
                // Let the Toast display and give some time to post to server before app will get shutdown
                Thread.sleep(10000)
            } catch (e: InterruptedException) {
            }
            exitProcess(1)
        }

        //Initialize the app by copying cjdroute and generating the conf file
        anodeUtil!!.initializeApp()

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

    companion object {
        private const val LOGTAG = "MainActivity"
    }
}