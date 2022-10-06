package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import co.anode.anodium.BuildConfig
import co.anode.anodium.R
import co.anode.anodium.support.AnodeUtil
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class WalletStatsActivity : AppCompatActivity() {
    private var updating = true
    private lateinit var pinPasswordAlert: AlertDialog
    @Volatile
    private var walletUnlocked = false
    private var balanceLastTimeUpdated: Long = 0
    private lateinit var myBalance: TextView
    val peersListDetails = mutableListOf<String>()
    private var wrongPinAttempts= 0
    private var walletPasswordQuickRetry: String? = null
    private var activeWallet = "wallet"

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

        val param = intent.extras
        if (param?.get("walletName").toString() != "null") {
            activeWallet = param?.get("walletName").toString()
        }
        val walletFile = File("$filesDir/pkt/$activeWallet.db")
        if (!walletFile.exists()) {
            Toast.makeText(baseContext, "PKT wallet does not exist.", Toast.LENGTH_LONG).show()
            return
        }

        //Getting password from password prompt
        walletPasswordQuickRetry = param?.getString("password")

        //Initializing UI components
        myBalance = findViewById(R.id.wstats_balance)
        val peersListView = findViewById<ListView>(R.id.peers_list)
        val name = findViewById<TextView>(R.id.wstats_name)
        name.text = "$activeWallet.db"
        //Handle peer list click
        peersListView.setOnItemClickListener { _, _, position, _ ->
            showPeerDetails(peersListDetails[position])
        }

        val refreshButton = findViewById<Button>(R.id.buttonRefresh)
        refreshButton.setOnClickListener {
            loadWalletStats()
        }
        val generateButton = findViewById<Button>(R.id.buttonGeneratePassword)
        generateButton.setOnClickListener {
            pinPrompt()
        }
    }

    private fun loadWalletStats() {
        getInfo()
        if (walletUnlocked) {
            if (findViewById<TextView>(R.id.wstats_address).text.toString().isEmpty()) {
                getCurrentPKTAddress()
            }
            getBalance()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean { // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_wallet_stats, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean { // Handle action bar item clicks here. The action bar will
        val id = item.itemId
        if (id == R.id.action_wallet_info) {
            Log.i(BuildConfig.APPLICATION_ID, "Open wallet info activity")
            val walletInfo = Intent(applicationContext, WalletInfoActivity::class.java)
            startActivity(walletInfo)
            return true
        } else if (id == R.id.action_wallet_debug) {
            Log.i(BuildConfig.APPLICATION_ID, "Open wallet debug logs activity")
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

        AnodeUtil.apiController.get(AnodeUtil.apiController.getInfoURL) { response ->
            if (response != null) {
                walletUnlocked = response.has("wallet") && !response.isNull("wallet") && response.getJSONObject("wallet").has("currentHeight")
                if (!walletUnlocked) {
                    pinOrPasswordPrompt(wrongPass = false, forcePassword = false)
                    return@get
                }
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
                    val addressRegex = "([0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3}[.][0-9]{1,3})|(\\s*((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])(\\.(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])){3}))|:)))(%.+)?)".toRegex()
                    for (i in 0 until neutrinoPeers.length()) {
                        val peerDesc = neutrinoPeers.getJSONObject(i)
                        val addr = peerDesc.getString("addr")
                        peersList.add(addressRegex.find(addr,0)?.value+" Sync: " + peerDesc.getString("lastBlock").toString())
                        peersListDetails.add(peerDesc.toString(i))
                    }
                    for (i in 0 until neutrino.getJSONArray("bans").length()) {
                        val ban = neutrino.getJSONArray("bans").getJSONObject(i)
                        val banAddr = ban.getString("addr")
                        bansList.add(addressRegex.find(banAddr,0)?.value.toString())
                        bansList.add("Reason: " + ban.getString("reason"))
                        bansList.add("Endtime: " + ban.getString("endTime"))
                    }
                    for (i in 0 until neutrino.getJSONArray("queries").length()) {
                        val query = neutrino.getJSONArray("queries").getJSONObject(i)
                        val peerAddr = query.getString("peer")
                        queriesList.add(addressRegex.find(peerAddr,0)?.value+" | "+ query.getString("command"))
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
        AnodeUtil.apiController.get(AnodeUtil.apiController.getBalanceURL) { response ->
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
    }

    override fun onResume() {
        super.onResume()
        updating = true
        loadWalletStats()
    }

    private fun pinOrPasswordPrompt(wrongPass: Boolean, forcePassword:Boolean) {
        if (this::pinPasswordAlert.isInitialized && pinPasswordAlert.isShowing) { return }
        val storedPin = AnodeUtil.getWalletPin(activeWallet)
        var isPin = false
        val builder = AlertDialog.Builder(this)
        if (wrongPass) {
            builder.setTitle("Wrong password")
        } else {
            builder.setTitle("")
        }
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        if (wrongPinAttempts > 3) {
            input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            builder.setTitle("Too many failed PIN attempts.")
            builder.setMessage("Please enter your password")
        } else if (storedPin.isNotEmpty() && !forcePassword) {
            input.inputType = InputType.TYPE_CLASS_NUMBER
            builder.setMessage("Please enter your PIN")
            isPin = true
        } else {
            input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            builder.setMessage("Please enter your password")
        }
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        builder.setPositiveButton("OK"
        ) { dialog, _ ->
            val inputPassword = input.text.toString()
            var password = ""
            dialog.dismiss()
            if (isPin) {
                if (inputPassword != storedPin) {
                    wrongPinAttempts++
                } else if (inputPassword == storedPin){
                    wrongPinAttempts = 0
                    val encryptedPassword = AnodeUtil.getWalletPassword(activeWallet)
                    password = AnodeUtil.decrypt(encryptedPassword, inputPassword).toString()
                }
            } else {
                wrongPinAttempts = 0
                password = inputPassword
            }
            //try unlocking the wallet with new password
            unlockWallet(password)
        }

        builder.setNegativeButton("Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        if (storedPin.isNotEmpty() && wrongPinAttempts<=3) {
            builder.setNeutralButton("Password") { dialog, _ ->
                dialog.dismiss()
                pinOrPasswordPrompt(wrongPass = false, forcePassword = true)
            }
        }
        pinPasswordAlert = builder.create()
        pinPasswordAlert.setCanceledOnTouchOutside(false)
        pinPasswordAlert.show()
    }
    /**
     * Will unlock PKT Wallet using the password saved in
     * encrypted shared preferences
     * if wrongPassword is returned the saved password will be reset and user
     * prompted to enter new password
     * then will call getNewAddress, getBalance and getTransactions
     */
    private fun unlockWallet(password: String) {
        Log.i(BuildConfig.APPLICATION_ID, "Trying to unlock wallet")
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        jsonRequest.put("wallet_name", "$activeWallet.db")
        AnodeUtil.apiController.post(AnodeUtil.apiController.unlockWalletURL,jsonRequest) { response ->
            if (response == null) {
                Log.i(BuildConfig.APPLICATION_ID, "unknown status for wallet")
            } else if ((response.has("error")) &&
                response.getString("error").contains("ErrWrongPassphrase")) {
                Log.d(BuildConfig.APPLICATION_ID, "Error unlocking wallet, wrong password")
                pinOrPasswordPrompt(wrongPass = true, forcePassword = false)
                walletUnlocked = false
            } else if (response.has("error")) {
                Log.d(BuildConfig.APPLICATION_ID, "Error: "+response.getString("error").toString())
                walletUnlocked = false
            } else if (response.length() == 0) {
                //empty response is success
                Log.i(BuildConfig.APPLICATION_ID, "Wallet unlocked")
                walletUnlocked = true
            }
        }
    }

    private fun pinPrompt() {
        if (this::pinPasswordAlert.isInitialized && pinPasswordAlert.isShowing) { return }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Enter your PIN")
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        builder.setMessage("For generating wallet password, for recovering wallet from app versions 0.2.102-0.2.104")
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        builder.setPositiveButton("OK"
        ) { dialog, _ ->
            val generatedPassword = AnodeUtil.getTrustedPassword(input.text.toString())
            findViewById<TextView>(R.id.wstats_generatedpassword_label).visibility = View.VISIBLE
            val genPassField = findViewById<TextView>(R.id.wstats_generatedpassword)
            genPassField.text = generatedPassword
            genPassField.visibility = View.VISIBLE
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }
        pinPasswordAlert = builder.create()
        pinPasswordAlert.show()
    }

    private fun getCurrentPKTAddress() {
        val jsonData = JSONObject()
        jsonData.put("showzerobalance", true)
        AnodeUtil.apiController.post(AnodeUtil.apiController.getAddressBalancesURL, jsonData) {
                response ->
            if (response == null) {
                Log.e(BuildConfig.APPLICATION_ID, "unexpected null response from wallet/address/balances")
                return@post
            }
            if (response.has("addrs")) {
                var myPKTAddress: String
                //Parse response
                val addresses = response.getJSONArray("addrs")
                if (addresses.length() == 1) {
                    //myPKTAddress = addresses.getJSONObject(0).getString("address")
                    return@post
                } else if (addresses.length() == 0) {
                    getNewPKTAddress(1)
                    return@post
                } else {
                    var biggestBalance = 0.0f
                    //Default with the 1st address
                    myPKTAddress = addresses.getJSONObject(0).getString("address")
                    //Find address with the biggest balance
                    for (i in 0 until addresses.length()) {
                        val balance = addresses.getJSONObject(i).getString("total").toFloat()
                        if (balance > biggestBalance) {
                            biggestBalance = balance
                            myPKTAddress = addresses.getJSONObject(i).getString("address")
                        }
                    }
                }
                findViewById<TextView>(R.id.wstats_address).text = myPKTAddress
            } else if (response.has("error")) {
                //Parse error
                Log.d(BuildConfig.APPLICATION_ID, "Error: "+response.getString("error").toString())
            }
        }
    }

    private fun getNewPKTAddress(numberOfAddresses: Int) {
        for (i in 0 until numberOfAddresses) {
            AnodeUtil.apiController.post(AnodeUtil.apiController.getNewAddressURL, JSONObject("{}")) { response ->
                if ((response != null) && (response.has("address"))) {
                    i.inc()
                }
            }
        }
    }
}