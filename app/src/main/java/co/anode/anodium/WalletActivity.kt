package co.anode.anodium

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.wallet_activity_title)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        AnodeClient.eventLog(baseContext,"Activity: WalletActivity created")
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        //Show setup or main fragment according to wallet existing or not
        val ft = supportFragmentManager.beginTransaction()
        ft.setCustomAnimations(android.R.anim.fade_in,android.R.anim.fade_out)
        //Check if wallet exists
        val walletfile = File(baseContext.filesDir.toString() + "/lnd/data/chain/pkt/mainnet/wallet.db")
        if (walletfile.exists()) {
            val createfragment = supportFragmentManager.findFragmentById(R.id.wallet_fragmentCreate)
            if (createfragment != null) {
                Log.i(LOGTAG, "WalletActivity hide create fragment")
                ft.hide(createfragment)
            }
        } else {
            val mainfragment = supportFragmentManager.findFragmentById(R.id.wallet_fragmentMain)
            if (mainfragment != null) {
                Log.i(LOGTAG, "WalletActivity hide main fragment")
                ft.hide(mainfragment)
            }
        }
        ft.commit()
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}