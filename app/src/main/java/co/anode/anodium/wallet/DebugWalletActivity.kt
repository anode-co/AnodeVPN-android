package co.anode.anodium.wallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.R
import java.io.File

class DebugWalletActivity : AppCompatActivity() {
    private var toBottom = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug_wallet)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "PLD Log"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        //Open pld log file and display it
        val logtext = findViewById<TextView>(R.id.debugwalletlogtext)

        val logFile = File(AnodeUtil.CJDNS_PATH +"/"+ AnodeUtil.PLD_LOG)
        val maxLength = 500*1024
        if (logFile.length() > maxLength) {
            logtext.text = logFile.readText().drop(logFile.length().toInt()-maxLength)
        } else {
            logtext.text = logFile.readText()
        }

        val scroll = findViewById<ScrollView>(R.id.wallet_debug_scroll)
        scroll.post {
            scroll.fullScroll(View.FOCUS_DOWN)
        }
        scroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val diff = scroll.getChildAt(0).bottom - (scrollY + scroll.height)
            toBottom = diff < 50
        }
        Thread({
            Log.i(LOGTAG, "DebugActivity.RefreshValues startup")
            var sleep: Long = 500
            var oldLog = logtext.text
            while (true) {
                this.runOnUiThread {
                    if (toBottom) {
                        val newlog = File(AnodeUtil.CJDNS_PATH + "/" + AnodeUtil.PLD_LOG).readText()
                        if (newlog.length > oldLog.length) {
                            oldLog = newlog
                            if (newlog.length > maxLength) {
                                logtext.text = newlog.drop(logFile.length().toInt()-maxLength)
                            } else {
                                logtext.text = newlog
                            }
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
                }
                Thread.sleep(sleep)
            }
        }, "DebugWalletActivity.RefreshValues").start()
        val shareButton = findViewById<Button>(R.id.buttonSharePldLog)
        shareButton.setOnClickListener {
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, File(AnodeUtil.CJDNS_PATH + "/" + AnodeUtil.PLD_LOG).readText())
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}