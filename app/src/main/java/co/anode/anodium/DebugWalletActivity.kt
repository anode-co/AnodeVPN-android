package co.anode.anodium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.children
import java.io.File

class DebugWalletActivity : AppCompatActivity() {
    private var scrollposition = 0
    private var toBottom = false
    private var logfile = ""
    private var autoscroll = false

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
        logfile = File(anodeUtil.CJDNS_PATH+"/"+anodeUtil.PLTD_LOG).readText()
        logtext.text = logfile
        val scroll = findViewById<ScrollView>(R.id.wallet_debug_scroll)
        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }

        scroll.setOnScrollChangeListener { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            scrollposition = scrollY
            val diff = scroll.getChildAt(0).bottom - (scrollY + scroll.height)
            toBottom = diff < 50
        }

        Thread(Runnable {
            Log.i(LOGTAG, "DebugActivity.RefreshValues startup")
            var sleep: Long = 500
            var oldlog = logfile
            while (true) {
                this.runOnUiThread(Runnable {
                    if (toBottom) {
                        var newlog =
                            File(anodeUtil.CJDNS_PATH + "/" + anodeUtil.PLTD_LOG).readText()
                        if (newlog.length > oldlog.length) {
                            oldlog = newlog
                            logtext.text = newlog
                            //position is in characters?
                            //scroll.fullScroll(View.FOCUS_DOWN)
                            scroll.post {
                                scroll.fullScroll(View.FOCUS_DOWN)
                            }
                            sleep = 1000
                        } else {
                            sleep = 300
                        }
                    } else {
                        sleep = 1000
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