package co.anode.anodium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import java.io.File

class DebugWalletActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_wallet)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "PLTD Log"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        //Open pltd log file and display it
        val logtext = findViewById<TextView>(R.id.debugwalletlogtext)
        val anodeUtil = AnodeUtil(application)

        Thread(Runnable {
            Log.i(LOGTAG, "DebugActivity.RefreshValues startup")
            var sleep: Long = 500
            val anodeUtil = AnodeUtil(application)
            var logfile = File(anodeUtil.CJDNS_PATH+"/"+anodeUtil.PLTD_LOG).readText()
            this.runOnUiThread(Runnable {
                logtext.text = logfile
            })
            var oldlog = logfile
            while (true) {
                this.runOnUiThread(Runnable {
                    var newlog = File(anodeUtil.CJDNS_PATH+"/"+anodeUtil.PLTD_LOG).readText()
                    if (newlog.length > oldlog.length) {
                        oldlog = newlog
                        logtext.text = newlog
                        sleep = 2000
                    } else {
                        sleep = 500
                    }
                })
                Thread.sleep(sleep)
            }
        }, "DebugWalletActivity.RefreshValues").start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}