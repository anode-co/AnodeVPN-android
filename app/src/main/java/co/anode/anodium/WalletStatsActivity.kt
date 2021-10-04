package co.anode.anodium

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.util.*
import android.widget.ArrayAdapter

class WalletStatsActivity : AppCompatActivity() {
    var updating = true

    @SuppressLint("SetTextI18n")
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

        val peersExpandbutton = findViewById<TextView>(R.id.peers_expand)
        val bannedExpandbutton = findViewById<TextView>(R.id.banned_expand)
        val queriesExpandbutton = findViewById<TextView>(R.id.queries_expand)

        val peersListView = findViewById<ListView>(R.id.peers_list)
        val bannedListView = findViewById<ListView>(R.id.banned_list)
        val queriesListView = findViewById<ListView>(R.id.queries_list)
        peersListView.visibility = View.GONE
        bannedListView.visibility = View.GONE
        queriesListView.visibility = View.GONE

        peersExpandbutton.setOnClickListener {
            if (peersListView.visibility == View.VISIBLE) {
                peersListView.visibility = View.GONE
            } else {
                peersListView.visibility = View.VISIBLE
            }
        }
        bannedExpandbutton.setOnClickListener {
            if (bannedListView.visibility == View.VISIBLE) {
                bannedListView.visibility = View.GONE
            } else {
                bannedListView.visibility = View.VISIBLE
            }
        }
        queriesExpandbutton.setOnClickListener {
            if (queriesListView.visibility == View.VISIBLE) {
                queriesListView.visibility = View.GONE
            } else {
                queriesListView.visibility = View.VISIBLE
            }
        }
        myaddress.text = prefs.getString("lndwalletaddress", "")
        Thread(Runnable {
            while (!prefs.getBoolean("lndwalletopened", false)) {
                openPKTWallet()
                Thread.sleep(500)
            }
            val walletsync = findViewById<TextView>(R.id.wstats_walletsync)
            val neutrinosync = findViewById<TextView>(R.id.wstats_neutrinosync)
            val connectedServers = findViewById<TextView>(R.id.wstats_connectedservers)
            val bannedServers = findViewById<TextView>(R.id.wstats_bannedservers)
            val numqueries = findViewById<TextView>(R.id.wstats_queries)
            while(updating) {
                runOnUiThread {
                    mybalance.text = "Requesting data"
                    walletsync.text = "Requesting data\n "
                    neutrinosync.text = "Requesting data\n "
                    connectedServers.text = "-"
                    bannedServers.text = "-"
                    numqueries.text = "-"
                }
                val getinforesponse = LndRPCController.getInfo()
                val balance = LndRPCController.getTotalBalance()
                val peersList = mutableListOf<String>()
                val bansList = mutableListOf<String>()
                val queriesList = mutableListOf<String>()
                if (getinforesponse != null)  {
                    if (getinforesponse.neutrino != null) {
                        for (i in 0 until getinforesponse?.neutrino?.peersList?.size!!) {
                            val peerDesc = getinforesponse.neutrino?.peersList?.get(i)
                            peersList.add("Address: " + peerDesc?.addr)
                            peersList.add("Version: " + peerDesc?.userAgent)
                            peersList.add("Sync: " + peerDesc?.lastBlock.toString())
                        }
                        for (i in 0 until getinforesponse.neutrino?.bansList?.size!!) {
                            val ban = getinforesponse.neutrino?.bansList?.get(i)
                            bansList.add("Address: "+ban?.addr)
                            bansList.add("Reason: "+ban?.reason)
                            bansList.add("Endtime: "+ban?.endTime)
                        }
                        for (i in 0 until getinforesponse.neutrino?.queriesList?.size!!) {
                            val query = getinforesponse.neutrino?.queriesList?.get(i)
                            queriesList.add("Server: "+query?.peer)
                            queriesList.add("Request: "+query?.command)
                            queriesList.add("Created: "+query?.createTime)
                            if (query?.lastResponseTime!! > 0) {
                                val datetime = query.lastResponseTime.toLong().let { Date(it) }
                                queriesList.add("Waiting since: $datetime")
                            } else {
                                queriesList.add("Waiting since: ")
                            }
                        }
                    }
                }
                val peersadapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, android.R.id.text1, peersList)
                val bannedadapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, android.R.id.text1, bansList)
                val queriesadapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, android.R.id.text1, queriesList)

                runOnUiThread {
                    peersListView.adapter = peersadapter
                    bannedListView.adapter = bannedadapter
                    queriesListView.adapter = queriesadapter
                    if (balance < 0) {
                        mybalance.text = "Error retrieving balance"
                    } else {
                        mybalance.text = "PKT %.2f".format(balance)
                    }
                    if (getinforesponse != null) {
                        walletsync.text = getinforesponse.wallet.currentHeight.toString()+"\n"+getinforesponse.wallet.currentBlockTimestamp
                        neutrinosync.text = getinforesponse.neutrino.height.toString()+"\n"+getinforesponse.neutrino.blockTimestamp
                        connectedServers.text = getinforesponse.neutrino?.peersList?.size.toString()
                        bannedServers.text = getinforesponse.neutrino?.bansList?.size.toString()
                        numqueries.text = getinforesponse.neutrino?.queriesList?.size.toString()
                    }
                }
                Thread.sleep(5000)
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