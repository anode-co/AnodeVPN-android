package co.anode.anodium.ui.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.anode.anodium.R
import co.anode.anodium.databinding.FragmentWalletBinding
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import co.anode.anodium.wallet.SendPaymentActivity
import co.anode.anodium.wallet.TransactionDetailsFragment
import co.anode.anodium.wallet.TransactionHistoryActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class WalletFragment : Fragment() {
    private var _binding: FragmentWalletBinding? = null
    lateinit var statusBar: TextView
    private lateinit var statusIcon: ImageView
    private lateinit var apiController: APIController
    @Volatile
    private var walletUnlocked = false
    private var neutrinoSynced = false
    private var myPKTAddress = ""
    private val refreshPldInterval: Long = 10000
    lateinit var h: Handler
    private var balanceLastTimeUpdated: Long = 0
    private var transactionsLastTimeUpdated: Long = 0
    private var chainSyncLastShown: Long = 0
    private var updateConfirmations = arrayListOf<Boolean>()
    private var neutrinoTop = 0
    private val numberOfTxnsToShow = 25
    private var walletPasswordQuickRetry: String? = null
    private lateinit var pinPasswordAlert: AlertDialog
    private var wrongPinAttempts= 0
    private lateinit var root: View
    private lateinit var mycontext: Context
    private var txnDetailsNum = -1

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnodeClient.eventLog("Activity: WalletFragment created")

        val service = ServiceVolley()
        apiController = APIController(service)
        h = Handler(Looper.getMainLooper())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel = ViewModelProvider(this).get(WalletViewModel::class.java)
        mycontext = requireContext()
        _binding = FragmentWalletBinding.inflate(inflater, container, false)
        root = binding.root

        statusBar = root.findViewById(R.id.textview_status)
        AnodeClient.statustv = statusBar
        statusIcon = root.findViewById(R.id.status_icon)
//        val walletFile = File("$filesDir/pkt/wallet.db")
//        if (!walletFile.exists()) {
//            return
//        }

        //Init UI elements
        val walletAddress = root.findViewById<TextView>(R.id.walletAddress)
        walletAddress.setOnClickListener {
            AnodeClient.eventLog("Button: Copy wallet address clicked")
            Toast.makeText(context, "address has been copied", Toast.LENGTH_LONG).show()
        }
        val history = root.findViewById<TextView>(R.id.texthistory)
        history.setOnClickListener {
            AnodeClient.eventLog("Button: Older transactions clicked")
            val transactionsHistoryActivity = Intent(context, TransactionHistoryActivity::class.java)
            transactionsHistoryActivity.putExtra("skip", numberOfTxnsToShow)
            transactionsHistoryActivity.putExtra("neutrinotop", neutrinoTop)
            startActivityForResult(transactionsHistoryActivity, 0)
        }

        val sendPaymentButton = root.findViewById<Button>(R.id.button_sendPayment)
        //Disable it while trying to unlock wallet
        sendPaymentButton.isEnabled = false

        val shareButton = root.findViewById<Button>(R.id.walletAddressSharebutton)
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
            if (myPKTAddress.isEmpty()) {
                Log.d(LOGTAG, "Can not start sendPaymentActivity because myPKTAddress is empty.")
                Toast.makeText(context, "Can not send money from empty address.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val sendPaymentActivity = Intent(context, SendPaymentActivity::class.java)
            sendPaymentActivity.putExtra("walletAddress", myPKTAddress)
            startActivity(sendPaymentActivity)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        if(isAdded) {
            h.postDelayed(getPldInfo, 500)
        }
    }
    private val getPldInfo = Runnable { getInfo() }

    /**
     * getting pld info
     * determines if wallet is unlocked
     * updates status bar with wallet and chain syncing info
     */
    @SuppressLint("SimpleDateFormat")
    private fun getInfo() {
        apiController.get(apiController.getInfoURL) { response ->
            if ((response != null) && (!response.has("error"))) {
                var chainHeight = 0
                var walletHeight = 0
                var bHash = ""
                var neutrinoPeers = 0
                var bTimestamp: Long = 0
                //Check if wallet is unlocked
                walletUnlocked = response.has("wallet") && !response.isNull("wallet") && response.getJSONObject("wallet").has("currentHeight")
                if (!walletUnlocked) {
                    if (walletPasswordQuickRetry != null){
                        unlockWallet(walletPasswordQuickRetry!!)
                    } else {
                        pinOrPasswordPrompt(wrongPass = false, forcePassword = false)
                    }
                    return@get
                } else {
                    h.postDelayed(getPldInfo, refreshPldInterval)
                    h.postDelayed(refreshValues, 200)
                }
                val layout = root.findViewById<ConstraintLayout>(R.id.fragment_wallet)
                activity?.let { layout.setBackgroundColor(it.getColor(android.R.color.white)) }
                root.findViewById<Button>(R.id.button_sendPayment).isEnabled = true

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
    private fun unlockWallet(password:String) {
        Log.i(LOGTAG, "Trying to unlock wallet")
        h.removeCallbacks(getPldInfo)
        statusBar.text = getString(R.string.wallet_status_unlocking)
        statusIcon.setBackgroundResource(0)
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        showLoading()
        apiController.post(apiController.unlockWalletURL,jsonRequest) { response ->
            hideLoading()
            if (response == null) {
                Log.i(LOGTAG, "unknown status for wallet")
            } else if ((response.has("error")) &&
                response.getString("error").contains("ErrWrongPassphrase")) {
                Log.d(LOGTAG, "Error unlocking wallet, wrong password")
                walletPasswordQuickRetry = null //to stop retrying to unlock wallet
                pinOrPasswordPrompt(wrongPass = true, forcePassword = false)
                walletUnlocked = false
            } else if (response.has("error")) {
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                walletUnlocked = false
            } else if (response.length() == 0) {
                //empty response is success
                Log.i(LOGTAG, "Wallet unlocked")
                walletUnlocked = true
                //Update screen
                //TODO:???
//                updateUiWalletUnlocket()
            }
            h.postDelayed(getPldInfo,100)
        }
    }

    private fun getNewPKTAddress(numberOfAddresses: Int) {
        for (i in 0 until numberOfAddresses) {
            apiController.post(apiController.getNewAddressURL, JSONObject("{}")) { response ->
                if ((response != null) && (response.has("address"))) {
                    i.inc()
                }
            }
        }
    }

    private fun getCurrentPKTAddress() {
        val jsonData = JSONObject()
        jsonData.put("showzerobalance", true)
        apiController.post(apiController.getAddressBalancesURL, jsonData) {
                response ->
            if (response == null) {
                Log.e(LOGTAG, "unexpected null response from wallet/address/balances")
                return@post
            }
            if (response.has("addrs")) {
                //Parse response
                val addresses = response.getJSONArray("addrs")
                if (addresses.length() == 0) {
                    getNewPKTAddress(5)
                    return@post
                } else if (addresses.length() == 1) {
                    myPKTAddress = addresses.getJSONObject(0).getString("address")
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
                val walletAddress = root.findViewById<TextView>(R.id.walletAddress)
                walletAddress.text = myPKTAddress
            } else if (response.has("error")) {
                //Parse error
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
            }
        }
    }

    private fun pinOrPasswordPrompt(wrongPass: Boolean, forcePassword:Boolean) {
        if (this::pinPasswordAlert.isInitialized && pinPasswordAlert.isShowing) { return }
        val storedPin = AnodeUtil.getWalletPin()
        var isPin = false
        val builder = AlertDialog.Builder(mycontext)
        if (wrongPass) {
            builder.setTitle("Wrong password")
        } else {
            builder.setTitle("")
        }
        val input = EditText(mycontext)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        if (wrongPinAttempts > 3) {
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setTitle("Too many failed PIN attempts.")
            builder.setMessage("Please enter your password")
        } else if (storedPin.isNotEmpty() && !forcePassword) {
            input.inputType = InputType.TYPE_CLASS_NUMBER
            builder.setMessage("Please enter your PIN")
            isPin = true
        } else {
            input.inputType = InputType.TYPE_CLASS_TEXT
            builder.setMessage("Please enter your password")
        }
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        builder.setPositiveButton("OK"
        ) { dialog, _ ->
            val inputPassword = input.text.toString()
            dialog.dismiss()
            if (isPin) {
                if (inputPassword != storedPin) {
                    wrongPinAttempts++
                } else if (inputPassword == storedPin){
                    wrongPinAttempts = 0
                    val encryptedPassword = AnodeUtil.getWalletPassword()
                    walletPasswordQuickRetry = AnodeUtil.decrypt(encryptedPassword, inputPassword)
                }
            } else {
                wrongPinAttempts = 0
                walletPasswordQuickRetry = inputPassword
            }
            //try unlocking the wallet with new password
            if (walletPasswordQuickRetry != null ) {
                unlockWallet(walletPasswordQuickRetry!!)
            } else {
                pinOrPasswordPrompt(wrongPass = false, forcePassword = true)
            }
        }

        builder.setNegativeButton("Cancel"
        ) { dialog, _ ->
            h.postDelayed(getPldInfo, 0)
            dialog.dismiss()
        }
        if (isPin) {
            builder.setNeutralButton("Password") { dialog, _ ->
                dialog.dismiss()
                pinOrPasswordPrompt(wrongPass = false, forcePassword = true)
            }
        } else if (storedPin.isNotEmpty() && wrongPinAttempts<=3){
            builder.setNeutralButton("PIN") { dialog, _ ->
                dialog.dismiss()
                pinOrPasswordPrompt(wrongPass = false, forcePassword = false)
            }
        }

        pinPasswordAlert = builder.create()
        pinPasswordAlert.show()
    }

    private val refreshValues = Runnable {
        if (walletUnlocked) {
            if (myPKTAddress == "") {
                getCurrentPKTAddress()
            }
            if ((System.currentTimeMillis()-5000) > balanceLastTimeUpdated) {
                getBalance()
            }
            if ((System.currentTimeMillis()-5000) > transactionsLastTimeUpdated) {
                getWalletTransactions()
            }
        }
    }

    /**
     * get Transactions
     */
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun getWalletTransactions() {
        val listsLayout = root.findViewById<LinearLayout>(R.id.paymentsList)
        val simpleDate = SimpleDateFormat("yyyy-MM-dd HH:mm")
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
                    root.findViewById<TextView>(R.id.texthistory).visibility = View.VISIBLE
                }
                for (i in 0 until txnsSize) {
                    val transaction = transactions.getJSONObject(i)
                    //Add new line
                    val line = ConstraintLayout(mycontext)
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
                        txnDetailsNum = i
                        bundle.putInt("lineID", txnDetailsNum)
                        bundle.putBoolean("history", false)
                        transactionDetailsFragment.arguments = bundle
                        transactionDetailsFragment.show(parentFragmentManager, "")
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
                    val textAddress = TextView(mycontext)
                    textAddress.id = View.generateViewId()
                    textAddress.textSize = textSize
                    //AMOUNT
                    val textAmount = TextView(mycontext)
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
                    val textDate = TextView(mycontext)
                    textDate.id = View.generateViewId()
                    textDate.text = simpleDate.format(Date(transaction.getString("timeStamp").toLong() * 1000))
                    textDate.textSize = textSize
                    //Add columns
                    //confirmations indicator
                    val icon = ImageView(mycontext)
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
    /**
     * Get balance from wallet using REST call /v1/balance/blockchain
     * If it fails will keep trying...
     * On success it will update the screen
     *
     * @param v View
     */
    private fun getBalance() {
        apiController.get(apiController.getBalanceURL) { response ->
            if (response != null) {
                val json = JSONObject(response.toString())
                val walletBalance = root.findViewById<TextView>(R.id.walletBalanceNumber)
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

    @SuppressLint("SetTextI18n")
    private fun updateStatusBar(peers: Int, chainTop: Int, chainHeight: Int, bHash: String, bTimestamp: Long, walletHeight: Int) {
        if (context == null) return
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
     * reset background color on tx line
     * when returning from txns details
     */
    fun clearLines(lineID: Int) {
        txnDetailsNum = -1
        val l = root.findViewWithTag<ConstraintLayout>("TxLine$lineID")
        l?.setBackgroundColor(Color.WHITE)
    }

    private fun makeBackgroundWhite(){
        val layout = root.findViewById<ConstraintLayout>(R.id.fragment_wallet)
        layout.setBackgroundColor(mycontext.getColor(android.R.color.white))
    }

    private fun makeBackgroundGrey() {
        val layout = root.findViewById<ConstraintLayout>(R.id.fragment_wallet)
        layout.setBackgroundColor(mycontext.getColor(android.R.color.darker_gray))
    }

    private fun showLoading() {
        makeBackgroundGrey()
        val loading = root.findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        makeBackgroundWhite()
        val loading = root.findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.GONE
    }

    private fun handlePldError(error: String): String {
        if (error.contains("[LightningServer] is not yet ready")) {
            return "Waiting for wallet server..."
        }
        return error
    }
}