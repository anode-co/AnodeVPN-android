package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import java.io.File
import java.util.*
import android.widget.ArrayAdapter
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.R
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import org.json.JSONObject

class WalletStatsActivity : AppCompatActivity() {
    private var updating = true
    private lateinit var apiController: APIController
    private lateinit var h: Handler
    private val refreshValuesInterval: Long = 10000
    private var walletUnlocked = false
    private var balanceLastTimeUpdated: Long = 0
    private var passwordPromptActive = false
    private lateinit var myBalance: TextView

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
        h = Handler(Looper.getMainLooper())

        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        val walletfile = File("$filesDir/pkt/wallet.db")
        if (!walletfile.exists()) {
            Toast.makeText(baseContext, "PKT wallet does not exist.", Toast.LENGTH_LONG).show()
            return
        }

        //Initializing UI components
        val myaddress = findViewById<TextView>(R.id.wstats_address)
        myBalance = findViewById(R.id.wstats_balance)
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

        val deleteButton = findViewById<Button>(R.id.buttonDeleteNeutrino)
        deleteButton.setOnClickListener {
            AnodeUtil.deleteNeutrino()
            AnodeUtil.stopPld()//will restart
        }
    }

    private fun getInfo() {
        val walletsync = findViewById<TextView>(R.id.wstats_walletsync)
        val neutrinosync = findViewById<TextView>(R.id.wstats_neutrinosync)
        val connectedServers = findViewById<TextView>(R.id.wstats_connectedservers)
        val bannedServers = findViewById<TextView>(R.id.wstats_bannedservers)
        val numqueries = findViewById<TextView>(R.id.wstats_queries)
        myBalance.text = "Requesting data"
        walletsync.text = "Requesting data\n "
        neutrinosync.text = "Requesting data\n "
        connectedServers.text = "-"
        bannedServers.text = "-"
        numqueries.text = "-"

        val peersListView = findViewById<ListView>(R.id.peers_list)
        val bannedListView = findViewById<ListView>(R.id.banned_list)
        val queriesListView = findViewById<ListView>(R.id.queries_list)

        apiController.get(apiController.getInfoURL) { response ->
            if (response != null) {
                if (response.has("wallet") && !response.isNull("wallet")) {
                    val walletInfo = response.getJSONObject("wallet")
                    walletsync.text = walletInfo.getInt("currentHeight").toString() + "\n" + walletInfo.getString("currentBlockTimestamp")
                }
                if (response.has("neutrino") && !response.isNull("neutrino")) {
                    val peersList = mutableListOf<String>()
                    val bansList = mutableListOf<String>()
                    val queriesList = mutableListOf<String>()
                    val peersAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, android.R.id.text1, peersList)
                    val bannedAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, android.R.id.text1, bansList)
                    val queriesAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, android.R.id.text1, queriesList)
                    val neutrino = response.getJSONObject("neutrino")
                    neutrinosync.text = neutrino.getLong("height").toString() + "\n" + neutrino.getString("blockTimestamp")
                    connectedServers.text = neutrino.getJSONArray("peers").length().toString()
                    bannedServers.text = neutrino.getJSONArray("bans").length().toString()
                    numqueries.text = neutrino.getJSONArray("queries").length().toString()
                    val neutrinoPeers = neutrino.getJSONArray("peers")
                    for (i in 0 until neutrinoPeers.length()) {
                        val peerDesc = neutrinoPeers.getJSONObject(i)
                        peersList.add("Address: " + peerDesc.getString("addr"))
                        peersList.add("Version: " + peerDesc.getString("userAgent"))
                        peersList.add("Sync: " + peerDesc.getString("lastBlock").toString())
                    }
                    for (i in 0 until neutrino.getJSONArray("bans").length()) {
                        val ban = neutrino.getJSONArray("bans").getJSONObject(i)
                        bansList.add("Address: " + ban.getString("addr"))
                        bansList.add("Reason: " + ban.getString("reason"))
                        bansList.add("Endtime: " + ban.getString("endTime"))
                    }
                    for (i in 0 until neutrino.getJSONArray("queries").length()) {
                        val query = neutrino.getJSONArray("queries").getJSONObject(i)
                        queriesList.add("Server: " + query.getString("peer"))
                        queriesList.add("Request: " + query.getString("command"))
                        queriesList.add("Created: " + query.getString("createTime"))
                        if (query.getLong("lastResponseTime") > 0) {
                            val datetime = Date(query.getLong("lastResponseTime"))
                            queriesList.add("Waiting since: $datetime")
                        } else {
                            queriesList.add("Waiting since: ")
                        }
                    }
                    peersListView.adapter = peersAdapter
                    bannedListView.adapter = bannedAdapter
                    queriesListView.adapter = queriesAdapter
                }

            }
            h.postDelayed(getPldInfo, refreshValuesInterval)
        }
    }

    private val getPldInfo = Runnable {
        getInfo()
        if (!walletUnlocked) {
            unlockWallet()
        } else {
            getBalance()
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
        h.removeCallbacks(getPldInfo)
    }

    override fun onResume() {
        super.onResume()
        h.postDelayed(getPldInfo, 500)
        updating = true
    }

    private fun promptUserPassword() {
        var password: String
        val builder: AlertDialog.Builder = this.let { AlertDialog.Builder(it) }
        builder.setTitle("PKT Wallet")
        builder.setMessage("Please type your PKT Wallet password")
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp
        builder.setView(input)
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        builder.setPositiveButton("Submit"
        ) { dialog, _ ->
            password = input.text.toString()
            dialog.dismiss()
            if (password.isNotEmpty()) {
                passwordPromptActive = false
                //write password to encrypted shared preferences
                AnodeUtil.storePassword(password)
                //reset pkt wallet address
                val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                prefs.edit().putString("lndwalletaddress", "").apply()
                //try unlocking the wallet with new password
                unlockWallet()
            }
        }

        builder.setNegativeButton("Cancel"
        ) { dialog, _ ->
            passwordPromptActive = false
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun unlockWallet() {
        Log.i(LOGTAG, "Trying to unlock wallet")
        //Get encrypted password
        val walletPassword = AnodeUtil.getPasswordFromEncSharedPreferences()
        //If password is empty prompt user to enter new password
        if (walletPassword.isEmpty()) {
            if (!passwordPromptActive) {
                passwordPromptActive = true
                promptUserPassword()
            }
        } else {
            val jsonRequest = JSONObject()
            var b64Password = android.util.Base64.encodeToString(walletPassword.toByteArray(), android.util.Base64.DEFAULT)
            b64Password = b64Password.replace("\n","")
            jsonRequest.put("wallet_password", b64Password)
            apiController.post(apiController.unlockWalletURL,jsonRequest) { response ->
                if (response == null) {
                    //unknown, throw error
                    Log.i(LOGTAG, "unknown status for wallet")
                    //Store and push error
                    //TODO: push error
                } else if ((response.has("message")) &&
                    response.getString("message").contains("ErrWrongPassphrase")) {
                    Log.d(LOGTAG, "Error unlocking wallet, wrong password")
                    //Wrong Password
                    AnodeUtil.storePassword("")
                    if (!passwordPromptActive) {
                        passwordPromptActive = true
                        promptUserPassword()
                    }
                    walletUnlocked = false
                } else if (response.length() == 0) {
                    //empty response is success
                    Log.i(LOGTAG, "Wallet unlocked")
                    walletUnlocked = true
                    //Wait a bit before making next call
                    Thread.sleep(300)
                }
            }
        }
    }
}