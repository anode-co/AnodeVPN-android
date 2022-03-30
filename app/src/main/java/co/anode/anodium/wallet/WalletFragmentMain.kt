@file:Suppress("DEPRECATION")

package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import co.anode.anodium.*
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class WalletFragmentMain : Fragment() {
    lateinit var statusBar: TextView
    private lateinit var statusIcon: ImageView
    private lateinit var apiController: APIController
    private var walletUnlocked = false
    private var neutrinoSynced = false
    private var myPKTAddress = ""
    private val refreshPldInterval: Long = 10000
    lateinit var h: Handler
    private var balanceLastTimeUpdated: Long = 0
    private var transactionsLastTimeUpdated: Long = 0
    private var chainSyncLastShown: Long = 0
    private var passwordPromptActive = false
    private var updateConfirmations = arrayListOf<Boolean>()
    private var neutrinoTop = 0
    private val numberOfTxnsToShow = 25
    var noWalletUnlock = false
    //DEBUG
    private val logTxns = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        //Initialize handlers
        val service = ServiceVolley()
        apiController = APIController(service)
        h = Handler(Looper.getMainLooper())

        return inflater.inflate(R.layout.walletfragment_main, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (isAdded && this::h.isInitialized && !this.isHidden) {
            h.postDelayed(getPldInfo,500)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        //This is to cover the case where we show
        // the fragment after closing the setup wallet
        if (!hidden) {
            h.postDelayed(getPldInfo,0)
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::h.isInitialized) {
            h.removeCallbacks(refreshValues)
            h.removeCallbacks(getPldInfo)
        }
    }


    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        val context = requireContext()
        AnodeClient.eventLog("Activity: WalletFragmentMain created")
        refreshValues.init(v)
        getPldInfo.init(v)
        statusBar = v.findViewById(R.id.textview_status)
        statusIcon = v.findViewById(R.id.status_icon)
        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val walletFile = File(context.filesDir.toString() + "/pkt/wallet.db")
        if (!walletFile.exists()) {
            return
        }
        //set PKT address from shared preferences
        myPKTAddress = prefs.getString("lndwalletaddress", "").toString()
        val loading = v.findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.VISIBLE
        //Init UI elements
        val walletAddress = v.findViewById<TextView>(R.id.walletAddress)
        walletAddress.text = myPKTAddress
        walletAddress.setOnClickListener {
            AnodeClient.eventLog("Button: Copy wallet address clicked")
            Toast.makeText(context, "address has been copied", Toast.LENGTH_LONG).show()
        }
        val history =v.findViewById<TextView>(R.id.texthistory)
        history.setOnClickListener {
            AnodeClient.eventLog("Button: Older transactions clicked")
            val transactionsHistoryActivity = Intent(context, TransactionHistoryActivity::class.java)
            transactionsHistoryActivity.putExtra("skip", numberOfTxnsToShow)
            transactionsHistoryActivity.putExtra("neutrinotop", neutrinoTop)
            startActivityForResult(transactionsHistoryActivity, 0)
        }

        val sendPaymentButton = v.findViewById<Button>(R.id.button_sendPayment)
        //Disable it while trying to unlock wallet
        sendPaymentButton.isEnabled = false

        val shareButton = v.findViewById<Button>(R.id.walletAddressSharebutton)
        shareButton.setOnClickListener {
            AnodeClient.eventLog("Button: Share wallet address clicked")
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "This is my PKT wallet address: $myPKTAddress")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        sendPaymentButton.setOnClickListener {
            AnodeClient.eventLog("Button: Send PKT clicked")
            val sendPaymentActivity = Intent(context, SendPaymentActivity::class.java)
            startActivity(sendPaymentActivity)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun updateStatusBar(peers: Int, chainTop: Int, chainHeight: Int, bHash: String, bTimestamp: Long, walletHeight: Int) {
        if (peers == 0) {
            statusBar.text = getString(R.string.wallet_status_disconnected)
            statusIcon.setBackgroundResource(R.drawable.circle_red)
        } else if (chainHeight < chainTop) {
            statusIcon.setBackgroundResource(R.drawable.circle_orange)
            statusBar.text = "$peers Peers | "+getString(R.string.wallet_status_syncing_headers)+" $chainHeight/$chainTop"
        } else if (walletHeight < chainHeight){
            statusIcon.setBackgroundResource(R.drawable.circle_yellow)
            statusBar.text = "$peers Peers | "+getString(R.string.wallet_status_syncing_transactions)+" $walletHeight/$chainHeight"
        } else if (walletHeight == chainHeight) {
            statusIcon.setBackgroundResource(R.drawable.circle_green)
            val diffSeconds = (System.currentTimeMillis() - bTimestamp) / 1000
            val timeAgoText: String
            if (diffSeconds > 60) {
                val minutes = diffSeconds / 60
                if (minutes == (1).toLong()) {
                    timeAgoText = "$minutes "+getString(R.string.minute_ago)
                } else {
                    timeAgoText = "$minutes "+getString(R.string.minutes_ago)
                }
                if (diffSeconds > 1140) {//19min
                    statusIcon.setBackgroundResource(R.drawable.warning)
                }
            } else {
                timeAgoText = "$diffSeconds "+getString(R.string.seconds_ago)
            }
            statusBar.text = "$peers Peers | "+getString(R.string.wallet_status_synced)+"$chainHeight - $timeAgoText"
        }
    }

    /**
     * getting pld info
     * determines if wallet is unlocked
     * updates status bar with wallet and chain syncing info
     */
    @SuppressLint("SimpleDateFormat")
    private fun getInfo(v: View) {
        apiController.get(apiController.getInfoURL) { response ->
            if ((response != null) && (!response.has("error"))) {
                var chainHeight = 0
                var walletHeight = 0
                var bHash = ""
                var neutrinoPeers = 0
                var bTimestamp: Long = 0
                //Check if wallet is unlocked
                walletUnlocked = response.has("wallet") && !response.isNull("wallet") && response.getJSONObject("wallet").has("currentHeight")
                //Keep getting updates
                if (this::h.isInitialized) {
                    h.postDelayed(getPldInfo, refreshPldInterval)
                }
                if (!walletUnlocked) {
                    unlockWallet(v)
                    return@get
                } else {
                    h.postDelayed(refreshValues, 200)
                }
                val layout = v.findViewById<ConstraintLayout>(R.id.wallet_fragmentMain)
                activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
                v.findViewById<Button>(R.id.button_sendPayment).isEnabled = true

                if (((System.currentTimeMillis() - refreshPldInterval) > chainSyncLastShown) &&
                    response.has("neutrino") &&
                    !response.isNull("neutrino") &&
                    response.getJSONObject("neutrino").has("height")) {
                    val neutrino = response.getJSONObject("neutrino")
                    bHash = neutrino.getString("blockHash")
                    val timestamp = neutrino.getString("blockTimestamp")
                    if (!timestamp.isNullOrEmpty()) {
                        bTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").parse(timestamp).time
                    }
                    if (neutrino.has("peers")) {
                        neutrinoPeers = neutrino.length()
                        val peers = neutrino.getJSONArray("peers")
                        if (peers.length() > 0) {
                            if (peers.getJSONObject(0).getInt("lastBlock") > 0) {
                                val tempTop = peers.getJSONObject(0).getInt("lastBlock")
                                if (tempTop > neutrinoTop) {
                                    neutrinoTop = tempTop
                                }
                            }
                            chainHeight = neutrino.getInt("height")
                            chainSyncLastShown = System.currentTimeMillis()
                        }
                    }
                }
                if (response.has("wallet") &&
                        !response.isNull("wallet")) {
                    val wallet = response.getJSONObject("wallet")
                    walletHeight = wallet.getInt("currentHeight")
                }
                updateStatusBar(neutrinoPeers, neutrinoTop, chainHeight, bHash, bTimestamp, walletHeight)
            } else if ( response?.has("error") == true ) {
                statusBar.text = response.getString("error").toString()
            } else {
                walletUnlocked = false
                neutrinoSynced = false
            }
        }
    }
    /**
     * Will unlock PKT Wallet using the password saved in
     * encrypted shared preferences
     * if wrongPassword is returned the saved password will be reset and user
     * prompted to enter new password
     * then will call getNewAddress, getBalance and getTransactions
     */
    private fun unlockWallet(v: View) {
        Log.i(LOGTAG, "Trying to unlock wallet")
        statusBar.text = getString(R.string.wallet_status_unlocking)
        statusIcon.setBackgroundResource(0)
        //Get encrypted password
        val walletPassword = AnodeUtil.getKeyFromEncSharedPreferences("wallet_password")
        //If password is empty prompt user to enter new password
        if (walletPassword.isEmpty()) {
            //We should not be having a wallet without a password stored!!!
            //TODO: perhaps we should prompt the user to do a wallet recovery?
        } else {
            val jsonRequest = JSONObject()
            jsonRequest.put("wallet_passphrase", walletPassword)
            apiController.post(apiController.unlockWalletURL,jsonRequest) { response ->
                if (response == null) {
                    //unknown, throw error
                    Log.i(LOGTAG, "unknown status for wallet")
                    //Store and push error
                    //TODO: push error
                } else if ((response.has("message")) &&
                    response.getString("message").contains("ErrWrongPassphrase")) {
                    Log.d(LOGTAG, "Error unlocking wallet, wrong password")
                    //We can't do anything in this case...
                    //Wrong Password
//                    AnodeUtil.storePassword("")
//                    if (!passwordPromptActive) {
//                        passwordPromptActive = true
//                        promptUserPassword(v)
//                    }
                    walletUnlocked = false
                } else if (response.has("error")) {
                    Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                    walletUnlocked = false
                } else if (response.length() == 0) {
                    //empty response is success
                    Log.i(LOGTAG, "Wallet unlocked")
                    walletUnlocked = true
                    //Update screen
                    updateUiWalletUnlocket(v)
                }
            }
        }
    }

    private fun getNewPKTAddress(v: View) {
        //Check if we already have a stored address
        if (myPKTAddress == "") {
            apiController.post(apiController.getNewAddressURL, JSONObject("{}")) { response ->
                if ((response != null) && (response.has("address"))) {
                    myPKTAddress = response.getString("address")
                    val prefs = requireActivity().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                    prefs.edit().putString("lndwalletaddress", myPKTAddress).apply()
                    v.findViewById<TextView>(R.id.walletAddress).text = myPKTAddress
                }
            }
        }
    }

    /**
     * Update layout elements to indicate that wallet is unlocked
     *
     * @param v View
     */
    private fun updateUiWalletUnlocket(v: View) {
        if (isAdded) {
            statusBar.text = getString(R.string.wallet_status_unlock)
            statusIcon.setBackgroundResource(0)
            val layout = v.findViewById<ConstraintLayout>(R.id.wallet_fragmentMain)
            activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
            val loading = v.findViewById<ProgressBar>(R.id.loadingAnimation)
            loading.visibility = View.GONE
            v.findViewById<Button>(R.id.button_sendPayment).isEnabled = true
        }
    }

    /**
     * Get balance from wallet using REST call /v1/balance/blockchain
     * If it fails will keep trying...
     * On success it will update the screen
     *
     * @param v View
     */
    private fun getBalance(v: View) {
        apiController.get(apiController.getBalanceURL) { response ->
            if (response != null) {
                val json = JSONObject(response.toString())
                val walletBalance = v.findViewById<TextView>(R.id.walletBalanceNumber)
                if (json.has("totalBalance")) {
                    walletBalance.text = AnodeUtil.satoshisToPKT(json.getString("totalBalance").toLong())
                    balanceLastTimeUpdated = System.currentTimeMillis()
                } else if (response.has("message") &&
                        !response.isNull("message")) {
                    statusBar.text = handlePldError(response.getString("message"))
                    statusIcon.setBackgroundResource(0)
                }
            } else {
//                statusBar.text = ""
            }
        }
    }

    private fun handlePldError(error: String): String {
        if (error.contains("[LightningServer] is not yet ready")) {
            return "Waiting for wallet server..."
        }
        return error
    }

    /**
     * reset background color on tx line
     * when returning from txns details
     */
    fun clearLines(lineID: Int) {
        val l = requireView().findViewWithTag<ConstraintLayout>("TxLine$lineID")
        l?.setBackgroundColor(Color.WHITE)
    }

    /**
     * get Transactions
     */
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun getWalletTransactions(v: View) {
        val listsLayout = v.findViewById<LinearLayout>(R.id.paymentsList)
        val simpleDate = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val context = requireContext()
        val params = JSONObject()
        //Exclude mining transactions
        params.put("coinbase", 1)
        params.put("txnsLimit", numberOfTxnsToShow+1)
        if (neutrinoTop > 0) {
            params.put("startHeight", neutrinoTop)
            params.put("endHeight", 1)
        }
        val textSize = 15.0f

        apiController.post(apiController.getTransactionsURL, params) { response ->
            if ((response != null) &&
                !response.has("error") &&
                response.has("transactions") &&
                !response.isNull("transactions")) {
                transactionsLastTimeUpdated = System.currentTimeMillis()

                val transactions = response.getJSONArray("transactions")
                if (transactions.length() == 0) return@post
                if (logTxns) {
                    val txnsLogfile = File(requireActivity().filesDir.toString()+"/txnslog.txt")
                    val time = Calendar.getInstance().time
                    txnsLogfile.appendText("$time REQUEST: $params\n")
                    txnsLogfile.appendText("$time RESPONSE: $transactions\n")
                }
                listsLayout.removeAllViews()
                val lastAmount = transactions.getJSONObject(0).getString("amount").toLong()
                val timeDiff = (System.currentTimeMillis() / 1000) - transactions.getJSONObject(0).getString("timeStamp").toLong()
                //If last transaction is in the last 2min and is incoming push notification
                if ((lastAmount > 0) && timeDiff < 120) {
                    AnodeUtil.pushNotification("Got paid!", AnodeUtil.satoshisToPKT(lastAmount))
                }

                var txnsSize = transactions.length()
                if (txnsSize > 25) {
                    txnsSize = 25
                    v.findViewById<TextView>(R.id.texthistory).visibility = View.VISIBLE
                }
                for (i in 0 until txnsSize) {
                    val transaction = transactions.getJSONObject(i)
                    //Add new line
                    val line = ConstraintLayout(context)
                    val numConfirmations = transaction.getInt("numConfirmations")
                    val amount = transaction.getString("amount").toLong()
                    val amountStr = AnodeUtil.satoshisToPKT(amount)
                    val destAddress = transaction.getJSONArray("destAddresses").getString(0)
                    line.setOnClickListener {
                        val transactionDetailsFragment: BottomSheetDialogFragment = TransactionDetailsFragment()
                        val bundle = Bundle()
                        bundle.putString("txid", transaction.getString("txHash"))
                        bundle.putString("amount", amountStr)
                        bundle.putInt("blockheight", transaction.getInt("blockHeight"))
                        bundle.putString("blockhash", transaction.getString("blockHash"))
                        if (amount < 0) {
                            bundle.putString("address", destAddress)
                        }
                        bundle.putInt("confirmations", numConfirmations)
                        bundle.putInt("lineID", i)
                        bundle.putBoolean("history", false)
                        transactionDetailsFragment.arguments = bundle
                        transactionDetailsFragment.show(requireActivity().supportFragmentManager, "")
                        line.setBackgroundColor(Color.GRAY)
                    }
                    line.setBackgroundColor(Color.WHITE)
                    line.id = i
                    line.tag = "TxLine$i"
                    //line.orientation = LinearLayout.HORIZONTAL
                    val llParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                    line.layoutParams = llParams
                    line.setPadding(20, 20, 10, 20)
                    //ADDRESS
                    val textAddress = TextView(context)
                    textAddress.id = View.generateViewId()
                    textAddress.textSize = textSize
                    //AMOUNT
                    val textAmount = TextView(context)
                    textAmount.id = View.generateViewId()
                    textAmount.textSize = textSize
                    if (amount < 0) {
                        textAddress.text = "..." + destAddress.substring(destAddress.length - 4)
                        textAmount.text = amountStr
                        textAmount.setTextColor(Color.RED)
                    } else {
                        textAddress.text = "unknown"
                        textAmount.text = "+$amountStr"
                        textAmount.setTextColor(Color.BLACK)
                    }
                    //DATE
                    val textDate = TextView(context)
                    textDate.id = View.generateViewId()
                    textDate.text = simpleDate.format(Date(transaction.getString("timeStamp").toLong() * 1000))
                    textDate.textSize = textSize
                    //Add columns
                    //confirmations indicator
                    val icon = ImageView(context)
                    icon.id = View.generateViewId()
                    if (numConfirmations == 0) {
                        icon.setBackgroundResource(R.drawable.unconfirmed)
                        updateConfirmations.add(true)
                    }else if (numConfirmations == 1) {
                        icon.setBackgroundResource(R.drawable.clock1)
                        updateConfirmations.add(true)
                    } else if (numConfirmations == 2) {
                        icon.setBackgroundResource(R.drawable.clock2)
                        updateConfirmations.add(true)
                    } else if (numConfirmations == 3) {
                        icon.setBackgroundResource(R.drawable.clock3)
                        updateConfirmations.add(true)
                    } else if (numConfirmations == 4) {
                        icon.setBackgroundResource(R.drawable.clock4)
                        updateConfirmations.add(true)
                    } else if (numConfirmations == 5) {
                        icon.setBackgroundResource(R.drawable.clock5)
                        updateConfirmations.add(true)
                    }else if (numConfirmations > 5) {
                        icon.setBackgroundResource(R.drawable.confirmed)
                        updateConfirmations.add(false)
                    }
                    line.addView(icon)
                    //Date
                    line.addView(textDate)
                    //Amount
                    line.addView(textAmount)
                    //Last 4 digits of address
                    line.addView(textAddress)
                    val set = ConstraintSet()
                    set.clear(icon.id)
                    set.clear(textDate.id)
                    set.clear(textAmount.id)
                    set.clear(textAddress.id)
                    //icon on start
                    set.connect(icon.id, ConstraintSet.START, line.id, ConstraintSet.START, 20)
                    //date with icon
                    set.connect(textDate.id, ConstraintSet.START, icon.id, ConstraintSet.END, 20)
//                        set.connect(icon.id, ConstraintSet.END, textDate.id, ConstraintSet.START, 0)
                    //amount with date
                    set.connect(textAmount.id, ConstraintSet.START, textDate.id, ConstraintSet.END, 0)
//                        set.connect(textDate.id, ConstraintSet.END, textAmount.id, ConstraintSet.START, 0)
                    //address with amount
//                        set.connect(textAddress.id,ConstraintSet.START, textAmount.id,ConstraintSet.END, 0 )
                    set.connect(textAmount.id, ConstraintSet.END, textAddress.id, ConstraintSet.START, 0)
                    //address with end
                    set.connect(textAddress.id, ConstraintSet.END, line.id, ConstraintSet.END, 20)
                    val chainViews = intArrayOf( textDate.id, textAmount.id)
                    val chainWeights = floatArrayOf(0f, 0f)
                    set.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, chainViews, chainWeights, ConstraintSet.CHAIN_SPREAD)
                    set.constrainWidth(icon.id, ConstraintSet.WRAP_CONTENT)
                    set.constrainHeight(icon.id, ConstraintSet.WRAP_CONTENT)
                    set.constrainWidth(textDate.id, ConstraintSet.WRAP_CONTENT)
                    set.constrainHeight(textDate.id, ConstraintSet.WRAP_CONTENT)
                    set.constrainWidth(textAmount.id, ConstraintSet.WRAP_CONTENT)
                    set.constrainHeight(textAmount.id, ConstraintSet.WRAP_CONTENT)
                    set.constrainWidth(textAddress.id, ConstraintSet.WRAP_CONTENT)
                    set.constrainHeight(textAddress.id, ConstraintSet.WRAP_CONTENT)
                    set.applyTo(line)
                    //Add lines
                    listsLayout.addView(line)
                }
            } else if ((response != null) &&
                    response.has("message") &&
                    !response.isNull("message")) {
                statusBar.text = handlePldError(response.getString("message"))
                Log.d(LOGTAG, response.getString("message"))
            }
        }
    }
    private val getPldInfo = object : Runnable {
        lateinit var v: View

        fun init(view: View)  {
            v = view
        }

        override fun run() {
            getInfo(v)

        }
    }

    private val refreshValues = object : Runnable {
        lateinit var v: View

        fun init(view: View)  {
            v = view
        }
        override fun run() {
            if (walletUnlocked) {
                if (myPKTAddress == "") {
                    getNewPKTAddress(v)
                }
                if ((System.currentTimeMillis()-5000) > balanceLastTimeUpdated) {
                    getBalance(v)
                }
                if ((System.currentTimeMillis()-5000) > transactionsLastTimeUpdated) {
                    getWalletTransactions(v)
                }
            } else {
                unlockWallet(v)
            }
        }
    }
}

