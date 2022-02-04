package co.anode.anodium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView

class WalletInfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_info)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Wallet Info"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        //Call getinfo and display
        Thread( {
            Log.i(LOGTAG, "WalletInfoActivity.RefreshValues")
            val infotext = findViewById<TextView>(R.id.walletinfotext)
            while (true) {
                val response = LndRPCController.getInfo(this)//LndRPCController.getInfo()
                this.runOnUiThread(Runnable {
                    infotext.text = response.toString()
                })
                Thread.sleep(10000)
            }
        }, "WalletInfoActivity.RefreshValues").start()
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}