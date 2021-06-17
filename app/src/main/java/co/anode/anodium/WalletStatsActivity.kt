package co.anode.anodium

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WalletStatsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_stats)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Wallet Stats"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        val button = findViewById<Button>(R.id.buttonViewWalletLog)
        button.setOnClickListener {
            val debugWalletActivity = Intent(this, DebugWalletActivity::class.java)
            startActivity(debugWalletActivity)
        }
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        val walletfile = File("$filesDir/lnd/data/chain/pkt/mainnet/wallet.db")
        if (walletfile.exists() && !prefs.getBoolean("lndwalletopened", false)) {
            Toast.makeText(baseContext, "PKT wallet is not unlocked please try again.", Toast.LENGTH_LONG).show()
        } else {
            val myaddress = findViewById<TextView>(R.id.wstats_address)
            val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            myaddress.text = prefs.getString("lndwalletaddress", "")
            val mybalance = findViewById<TextView>(R.id.wstats_balance)
            mybalance.text = "PKT %.2f".format(LndRPCController.getTotalBalance())
            val response = LndRPCController.getInfo()
            if (response != null) {
                val blockHeight = findViewById<TextView>(R.id.wstats_bheight)
                blockHeight.text = response.blockHeight.toString()
                val blockhash = findViewById<TextView>(R.id.wstats_bhash)
                blockhash.text = response.blockHash
                val headertimestamp = findViewById<TextView>(R.id.wstats_bheader)
                val simpleDate = SimpleDateFormat("dd/MM/yyyy")
                headertimestamp.text = simpleDate.format(Date(response.bestHeaderTimestamp * 1000))
                val synced = findViewById<TextView>(R.id.wstats_synced)
                synced.text = response.syncedToChain.toString()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}