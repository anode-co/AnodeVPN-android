package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import co.anode.anodium.R
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class WalletStatsActivity : AppCompatActivity() {
    private var updating = true
    private lateinit var apiController: APIController
    private var walletUnlocked = false
    private var balanceLastTimeUpdated: Long = 0
    private var passwordPromptActive = false
    private lateinit var myBalance: TextView
    val peersListDetails = mutableListOf<String>()

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

        //Initialize handlers
        val service = ServiceVolley()
        apiController = APIController(service)

        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        val walletfile = File("$filesDir/pkt/wallet.db")
        if (!walletfile.exists()) {
            Toast.makeText(baseContext, "PKT wallet does not exist.", Toast.LENGTH_LONG).show()
            return
        }

        //Initializing UI components
        val myaddress = findViewById<TextView>(R.id.wstats_address)
        myBalance = findViewById(R.id.wstats_balance)

        val peersListView = findViewById<ListView>(R.id.peers_list)

        myaddress.text = prefs.getString("lndwalletaddress", "")
        //Handle peer list click
        peersListView.setOnItemClickListener { _, _, position, _ ->
            showPeerDetails(peersListDetails[position])
        }

        val refreshButton = findViewById<Button>(R.id.buttonRefresh)
        refreshButton.setOnClickListener {
            getInfo()
            if (!walletUnlocked) {
                unlockWallet()
            } else {
                getBalance()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_wallet_stats, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
        val id = item.itemId
        if (id == R.id.action_wallet_info) {
            Log.i(LOGTAG, "Open wallet info activity")
            val walletInfo = Intent(applicationContext, WalletInfoActivity::class.java)
            startActivity(walletInfo)
            return true
        } else if (id == R.id.action_wallet_debug) {
            Log.i(LOGTAG, "Open wallet debug logs activity")
            val debugWalletActivity = Intent(this, DebugWalletActivity::class.java)
            startActivity(debugWalletActivity)
            return true
        } else if (id == R.id.action_wallet_delete_db) {
            AnodeUtil.deleteNeutrino()
            AnodeUtil.stopPld()//will restart
            return true
        }else {
            super.onOptionsItemSelected(item)
            return false
        }
    }

    private fun showPeerDetails(details: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Peer Details")
        builder.setMessage(details)
        builder.setNegativeButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
    private fun getInfo() {
        val walletSync = findViewById<TextView>(R.id.wstats_walletsync)
        val neutrinoSync = findViewById<TextView>(R.id.wstats_neutrinosync)
        val connectedServers = findViewById<TextView>(R.id.wstats_connectedservers)
        val bannedServers = findViewById<TextView>(R.id.wstats_bannedservers)
        val numQueries = findViewById<TextView>(R.id.wstats_queries)
        myBalance.text = "Requesting data"
        walletSync.text = "Requesting data\n "
        neutrinoSync.text = "Requesting data\n "
        connectedServers.text = "-"
        bannedServers.text = "-"
        numQueries.text = "-"

        val peersListView = findViewById<ListView>(R.id.peers_list)
        val bannedListView = findViewById<ListView>(R.id.banned_list)
        val queriesListView = findViewById<ListView>(R.id.queries_list)

        apiController.get(apiController.getInfoURL) { response ->
            if (response != null) {
                if (response.has("wallet") && !response.isNull("wallet")) {
                    val walletInfo = response.getJSONObject("wallet")
                    val localDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").parse(walletInfo.getString("currentBlockTimestamp"))
                    walletSync.text = walletInfo.getInt("currentHeight").toString() + " | " +SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(localDateTime)
                }
                if (response.has("neutrino") && !response.isNull("neutrino")) {
                    peersListDetails.clear()
                    val peersList = mutableListOf<String>()
                    val bansList = mutableListOf<String>()
                    val queriesList = mutableListOf<String>()
                    val peersAdapter = ArrayAdapter(this,R.layout.simple_list_item, R.id.simple_list_textview, peersList)
                    val bannedAdapter = ArrayAdapter(this,R.layout.simple_list_item, R.id.simple_list_textview, bansList)
                    val queriesAdapter = ArrayAdapter(this,R.layout.simple_list_item, R.id.simple_list_textview, queriesList)
                    val neutrino = response.getJSONObject("neutrino")
                    val localDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").parse(neutrino.getString("blockTimestamp"))
                    neutrinoSync.text = neutrino.getLong("height").toString() + " | " +SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(localDateTime)
                    connectedServers.text = neutrino.getJSONArray("peers").length().toString()
                    bannedServers.text = neutrino.getJSONArray("bans").length().toString()
                    numQueries.text = neutrino.getJSONArray("queries").length().toString()
                    val neutrinoPeers = neutrino.getJSONArray("peers")
                    for (i in 0 until neutrinoPeers.length()) {
                        val peerDesc = neutrinoPeers.getJSONObject(i)
                        peersList.add("Address: " + peerDesc.getString("addr").split(":")[0]+" Sync: " + peerDesc.getString("lastBlock").toString())
                        peersListDetails.add(peerDesc.toString(i))
//                        peersList.add("Version: " + peerDesc.getString("userAgent"))
//                        peersList.add("Sync: " + peerDesc.getString("lastBlock").toString())
                    }
//                    peersList.sort()
//                    peersListDetails.sort()
                    for (i in 0 until neutrino.getJSONArray("bans").length()) {
                        val ban = neutrino.getJSONArray("bans").getJSONObject(i)
                        bansList.add("Address: " + ban.getString("addr"))
                        bansList.add("Reason: " + ban.getString("reason"))
                        bansList.add("Endtime: " + ban.getString("endTime"))
                    }
                    for (i in 0 until neutrino.getJSONArray("queries").length()) {
                        val query = neutrino.getJSONArray("queries").getJSONObject(i)
                        queriesList.add("Server: " + query.getString("peer").split(":")[0]+" | "+ query.getString("command"))
                        if (query.getLong("lastResponseTime") > 0) {
                            val datetime = Date(query.getLong("lastResponseTime"))
                            queriesList.add("Waiting since: $datetime")
                        } else {
                            queriesList.add("no waiting ")
                        }
                    }
                    peersListView.adapter = peersAdapter

                    var totalHeight = 0
                    for (i in 0 until peersAdapter.count) {
                        val listItem: View = peersAdapter.getView(i, null, peersListView)
                        listItem.measure(0, 0)
                        totalHeight += listItem.measuredHeight
                    }
                    val params = peersListView.layoutParams
                    params.height = totalHeight + (peersListView.dividerHeight * (peersAdapter.count-1))
                    peersListView.layoutParams = params
                    peersListView.requestLayout()
                    bannedListView.adapter = bannedAdapter
                    queriesListView.adapter = queriesAdapter
                }
            }
        }
    }

    private fun getBalance() {
        //Get Balance
        apiController.get(apiController.getBalanceURL) { response ->
            if (response != null) {
                val json = JSONObject(response.toString())
                if (json.has("totalBalance")) {
                    myBalance.text = AnodeUtil.satoshisToPKT(json.getString("totalBalance").toLong())
                    balanceLastTimeUpdated = System.currentTimeMillis()
                } else {
                    //getBalance(v, a)
                }
            } else {
                //getBalance(v, a)
            }
        }
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
        getInfo()
        getBalance()
    }

    override fun onResume() {
        super.onResume()
        updating = true
        getInfo()
        getBalance()
    }


    private fun unlockWallet() {
        Log.i(LOGTAG, "Trying to unlock wallet")
        //Get encrypted password
        val walletPassword = AnodeUtil.getKeyFromEncSharedPreferences("wallet_password")
        //If password is empty prompt user to enter new password
        if (walletPassword.isEmpty()) {
            //We should not be having a wallet without a password stored!!!
        } else {
            val jsonRequest = JSONObject()
            jsonRequest.put("wallet_passphrase", walletPassword)
            apiController.post(apiController.unlockWalletURL,jsonRequest) { response ->
                if (response == null) {
                    //unknown, throw error
                    Log.i(LOGTAG, "unknown status for wallet")
                } else if ((response.has("error")) &&
                    response.getString("error").contains("ErrWrongPassphrase")) {
                    Log.d(LOGTAG, "Error unlocking wallet, wrong password")
                    walletUnlocked = false
                } else if (response.has("error")) {
                    Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                    walletUnlocked = false
                } else if (response.length() == 0) {
                    //empty response is success
                    Log.i(LOGTAG, "Wallet unlocked")
                    walletUnlocked = true
                }
            }
        }
    }
}