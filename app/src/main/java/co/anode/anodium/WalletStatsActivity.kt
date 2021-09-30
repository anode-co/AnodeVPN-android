package co.anode.anodium

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContentProviderCompat.requireContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WalletStatsActivity : AppCompatActivity() {
    var updating = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_stats)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Wallet Stats"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        val buttonWalletLog = findViewById<Button>(R.id.buttonViewWalletLog)
        buttonWalletLog.setOnClickListener {
            val debugWalletActivity = Intent(this, DebugWalletActivity::class.java)
            startActivity(debugWalletActivity)
        }

        val buttonWalletInfo = findViewById<Button>(R.id.buttonViewWalletInfo)
        buttonWalletInfo.setOnClickListener {
            val infoWalletActivity = Intent(this, WalletInfoActivity::class.java)
            startActivity(infoWalletActivity)
        }

        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        val walletfile = File("$filesDir/lnd/data/chain/pkt/mainnet/wallet.db")
        if (!walletfile.exists()) {
            Toast.makeText(baseContext, "PKT wallet does not exist.", Toast.LENGTH_LONG).show()
            return
        }

        val myaddress = findViewById<TextView>(R.id.wstats_address)
        val mybalance = findViewById<TextView>(R.id.wstats_balance)

        myaddress.text = prefs.getString("lndwalletaddress", "")
        Thread(Runnable {
            while (!prefs.getBoolean("lndwalletopened", false)) {
                openPKTWallet()
                Thread.sleep(500)
            }
            while(updating) {
                runOnUiThread {
                    mybalance.text = "PKT %.2f".format(LndRPCController.getTotalBalance())
                    val response = LndRPCController.getInfo()
                    if (response != null) {
                        val blockHeight = findViewById<TextView>(R.id.wstats_bheight)
                        blockHeight.text = response.lightning.blockHeight.toString()
                        val blockhash = findViewById<TextView>(R.id.wstats_bhash)
                        blockhash.text = response.lightning.blockHash
                        val headertimestamp = findViewById<TextView>(R.id.wstats_bheader)
                        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
                        headertimestamp.text = simpleDate.format(Date(response.lightning.bestHeaderTimestamp * 1000))
                        val synced = findViewById<TextView>(R.id.wstats_synced)
                        synced.text = response.lightning.syncedToChain.toString()
                    }
                }
                Thread.sleep(1000)
            }
        }, "WalletStats.RefreshValues").start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onStop() {
        super.onStop()
        updating = false
    }

    override fun onPause() {
        super.onPause()
        updating = false
    }

    override fun onResume() {
        super.onResume()
        updating = true
    }

    fun openPKTWallet(): Boolean {
        val prefs = getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
        Log.i(LOGTAG, "MainActivity trying to open wallet")
        var result = LndRPCController.openWallet(prefs)
        if (result.contains("ErrWrongPassphrase")) {
            var password = ""
            val builder: AlertDialog.Builder? = this.let { AlertDialog.Builder(it) }
            if (builder != null) {
                builder.setTitle("PKT Wallet")
                builder.setMessage("Please type your PKT Wallet password")
            }
            val input = EditText(this)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            input.layoutParams = lp
            if (builder != null) {
                runOnUiThread {
                    builder.setView(input)
                    builder.setPositiveButton(

                        "Submit",
                        DialogInterface.OnClickListener { dialog, _ ->
                            password = input.text.toString()
                            dialog.dismiss()

                            if ((prefs != null) && (password.isNotEmpty())) {
                                with(prefs.edit()) {
                                    putString("walletpassword", password)
                                    commit()
                                }
                                val result = LndRPCController.openWallet(prefs)
                                if (result == "OK") {
                                    Toast.makeText(this,"PKT wallet is open",Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this,"Wrong password.",Toast.LENGTH_LONG).show()
                                }
                            }
                        })

                    builder.setNegativeButton(
                        "Cancel",
                        DialogInterface.OnClickListener { dialog, _ ->
                            dialog.dismiss()
                        })
                    val alert: androidx.appcompat.app.AlertDialog = builder.create()
                    alert.show()
                }
            }
        } else if (result != "OK") {
            //can not open wallet
            Log.w(LOGTAG, "Can not open PKT wallet")
            //wrong password prompt user to type password again
            val datadir =
                File("$filesDir/lnd/data/chain/pkt/mainnet")
            var checkwallet = result
            if (!datadir.exists()) {
                Log.e(LOGTAG, "expected folder structure not available")
                checkwallet += " datadir does not exist "
            } else {
                checkwallet += " wallet.db exists "
            }
            if (prefs.getString("walletpassword", "").isNullOrEmpty()) {
                Log.e(LOGTAG, "walletpassword in shared preferences is empty")
                checkwallet += " walletpassword is empty"
            } else {
                checkwallet += " walletpassword is not empty"
            }
            val status = LndRPCController.isPltdRunning()
            checkwallet += " PLTD status: $status"
            return false
        } else if (result == "OK") {
            return true
        }
        return false
    }
}