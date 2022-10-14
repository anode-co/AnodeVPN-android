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
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.anode.anodium.BuildConfig
import co.anode.anodium.R
import co.anode.anodium.databinding.FragmentWalletBinding
import co.anode.anodium.integration.presentation.EnterWalletActivity
import co.anode.anodium.integration.presentation.WalletActivity
import co.anode.anodium.wallet.SendPaymentActivity
import co.anode.anodium.wallet.TransactionHistoryActivity
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.wallet.NewWalletActivity
import co.anode.anodium.wallet.TransactionDetailsFragment
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.wallet.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors


class WalletFragment : Fragment() {
    private val LOGTAG = BuildConfig.APPLICATION_ID
    private var _binding: FragmentWalletBinding? = null
    lateinit var statusBar: TextView
    private lateinit var statusIcon: ImageView

    @Volatile
    private var walletUnlocked = false
    private var neutrinoSynced = false
    private var myPKTAddress = ""
    private val refreshPldInterval: Long = 2000
    lateinit var h: Handler
    private var balanceLastTimeUpdated: Long = 0
    private var transactionsLastTimeUpdated: Long = 0
    private var chainSyncLastShown: Long = 0
    private var updateConfirmations = arrayListOf<Boolean>()
    private var neutrinoTop = 0
    private var resumedNeutrinoTop = 0
    private val numberOfTxnsToShow = 10
    private var txnsLimit = 10
    private var txnsLimitReached = false
    private var walletPasswordQuickRetry: String? = null
    private lateinit var pinPasswordAlert: AlertDialog
    private var wrongPinAttempts= 0
    private lateinit var root: View
    private lateinit var mycontext: Context
    private var txnDetailsNum = -1
    private var activeWallet = "wallet"

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnodeClient.eventLog("Activity: WalletFragment created")

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
        val fileDir = mycontext.filesDir

        val prefs = requireActivity().getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)

        activeWallet = prefs.getString("activeWallet","wallet").toString()

        //Init UI elements
        val walletAddress = root.findViewById<TextView>(R.id.walletAddress)
        walletAddress.setOnClickListener {
            AnodeClient.eventLog("Button: Copy wallet address clicked")
            Toast.makeText(context, "address has been copied", Toast.LENGTH_LONG).show()
        }
        walletAddress.text = getString(R.string.wallet_address_loading)
        val history = root.findViewById<TextView>(R.id.texthistory)
        history.setOnClickListener {
            AnodeClient.eventLog("Button: Older transactions clicked")
            val transactionsHistoryActivity = Intent(context, TransactionHistoryActivity::class.java)
            transactionsHistoryActivity.putExtra("skip", numberOfTxnsToShow)
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
                putExtra(Intent.EXTRA_TEXT, "$myPKTAddress")
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
            sendPaymentActivity.putExtra("walletName", activeWallet)
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
        val fileDir = mycontext.filesDir
        val activeWalletFile = File("$fileDir/pkt/$activeWallet.db")
        val walletFiles = AnodeUtil.getWalletFiles()
        if (walletFiles.size < 1) {
            Log.i(LOGTAG, "Open password prompt activity, no wallet file found")
            val prefs = requireActivity().getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
            if (prefs.getBoolean("useNewUI", false)) {
               //TODO: launch new screens
            } else {
                val newWalletActivity = Intent(context, NewWalletActivity::class.java)
                startActivity(newWalletActivity)
            }
        } else if (!activeWalletFile.exists()){
            activeWallet = walletFiles[0]
            Log.i(LOGTAG, "Active wallet file not found, will try to open $activeWallet.")
        } else {
            Log.i(LOGTAG, "Active wallet file $activeWallet found.")
            val label = root.findViewById<TextView>(R.id.walletAddressLabel)
            var labelStr = getString(R.string.wallet_address)
            if (activeWallet != "wallet") {
                  labelStr += " ($activeWallet)"
            }
            label.text = labelStr
        }
        resumedNeutrinoTop = neutrinoTop
        if(isAdded) {
            //Show cached info
            showCachedData()
            //Delay getinfo when we want to use the new ui to allow user to enter other screens
            val prefs = requireActivity().getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
            if (prefs.getBoolean("useNewUI",false)) {
                h.postDelayed(getPldInfo, 5000)
            } else {
                h.postDelayed(getPldInfo, 0)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        if (this::h.isInitialized) {
            h.removeCallbacks(refreshValues)
            h.removeCallbacks(getPldInfo)
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
        AnodeUtil.apiController.get(AnodeUtil.apiController.getInfoURL) { response ->
            if ((response != null) && (!response.has("error"))) {
                var chainHeight = 0
                var walletHeight = 0
                var bHash = ""
                var neutrinoPeers = 0
                var bTimestamp: Long = 0
                val prefs = requireActivity().getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
                //Check if wallet is unlocked
                walletUnlocked = response.has("wallet") && !response.isNull("wallet") && response.getJSONObject("wallet").has("currentHeight")
                if (!walletUnlocked) {
                    if (walletPasswordQuickRetry != null){
                        unlockWallet(walletPasswordQuickRetry!!)
                    } else {
                        if (prefs.getBoolean("useNewUI",false)) {
                            showNewWalletUnlockFragment()
                        } else {
                            pinOrPasswordPrompt(wrongPass = false, forcePassword = false)
                        }
//                        pinOrPasswordPrompt(wrongPass = false, forcePassword = false)
                    }
                    return@get
                } else if (prefs.getBoolean("useNewUI",false)){
                    showNewWalletFragment()
                    return@get
                } else {
                    h.postDelayed(getPldInfo, refreshPldInterval)
                    h.postDelayed(refreshValues, 50)
                }
                val layout = root.findViewById<ConstraintLayout>(R.id.fragment_wallet)
                activity?.let { layout.setBackgroundColor(it.getColor(android.R.color.transparent)) }
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
                        val peers = neutrino.getJSONArray("peers")
                        neutrinoPeers = peers.length()
                        for (i in 0 until peers.length()) {
                            if (peers.getJSONObject(0).getInt("lastBlock") > 0) {
                                val tempTop = peers.getJSONObject(0).getInt("lastBlock")
                                if (tempTop > neutrinoTop) {
                                    neutrinoTop = tempTop
                                }
                            }
                            if (neutrino.getInt("height") > chainHeight) {
                                chainHeight = neutrino.getInt("height")
                            }
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
        if (isAdded) {
            statusBar.text = getString(R.string.wallet_status_unlocking)
            statusIcon.setBackgroundResource(0)
        }
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        jsonRequest.put("wallet_name", "$activeWallet.db")
        showLoading()
        AnodeUtil.apiController.post(AnodeUtil.apiController.unlockWalletURL,jsonRequest) { response ->
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

    private fun showNewWalletFragment() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val result = AnodeClient.APIHttpReq("https://pkt.cash/api/cmc-price","", "GET", needsAuth = false, isRetry = false)
            handler.post {
                var price = "0"
                if (result.isNotBlank()) {
                    val json = JSONObject(result)
                    price = try {
                        json.getJSONObject("data").getJSONObject("PKT").getJSONObject("quote").getJSONObject("USD").getDouble("price").toString()
                    } catch (e: Exception) {
                        Log.e(LOGTAG, "Error parsing price: $e")
                        "0.00"
                    }
                }
                val walletActivity = Intent(mycontext, WalletActivity::class.java)
                walletActivity.putExtra("WALLET_NAME", activeWallet)
                walletActivity.putExtra("PKT_TO_USD", price)
                startActivity(walletActivity)
            }
        }
    }

    private fun showNewWalletUnlockFragment() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            handler.post {
                val enterWalletActivity = Intent(mycontext, EnterWalletActivity::class.java)
                enterWalletActivity.putExtra("WALLET_NAME", activeWallet)
                startActivity(enterWalletActivity)
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

    private fun getCurrentPKTAddress() {
        val jsonData = JSONObject()
        jsonData.put("showzerobalance", true)
        AnodeUtil.apiController.post(AnodeUtil.apiController.getAddressBalancesURL, jsonData) {
                response ->
            if (response == null) {
                Log.e(LOGTAG, "unexpected null response from wallet/address/balances")
                return@post
            }
            if (response.has("addrs")) {
                //Parse response
                val addresses = response.getJSONArray("addrs")
                if (addresses.length() == 0) {
                    getNewPKTAddress(1)
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
                AnodeUtil.setCacheWalletAddress(myPKTAddress)
            } else if (response.has("error")) {
                //Parse error
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
            }
        }
    }

    private fun pinOrPasswordPrompt(wrongPass: Boolean, forcePassword:Boolean) {
        if (this::pinPasswordAlert.isInitialized && pinPasswordAlert.isShowing) { return }
        val storedPin = AnodeUtil.getWalletPin(activeWallet)
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
            input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            builder.setTitle("Too many failed PIN attempts.")
            builder.setMessage("Please enter password for $activeWallet")
        } else if (storedPin.isNotEmpty() && !forcePassword) {
            input.inputType = InputType.TYPE_CLASS_NUMBER
            builder.setMessage("Please enter PIN for $activeWallet")
            isPin = true
        } else {
            input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            builder.setMessage("Please enter password for $activeWallet")
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
                    val encryptedPassword = AnodeUtil.getWalletPassword(activeWallet)
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
        pinPasswordAlert.setCanceledOnTouchOutside(false)
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
                getWalletTransactions(false)
            }
        }
    }

    private fun makeListofTxns(transactions: JSONArray, length: Int) {
        val listsLayout = root.findViewById<LinearLayout>(R.id.paymentsList)
        val simpleDate = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val textSize = 15.0f
        for (i in 0 until length) {
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
            line.setBackgroundColor(mycontext.getColor(android.R.color.transparent))
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
    }


    private fun showCachedData() {
        //Balance
        root.findViewById<TextView>(R.id.walletBalanceNumber).text = AnodeUtil.getCachedWalletBalance()
        //Address
        myPKTAddress = AnodeUtil.getCachedWalletAddress()
        if (myPKTAddress.isEmpty()) {
            root.findViewById<TextView>(R.id.walletAddress).text = getString(R.string.wallet_address_loading)
        } else {
            root.findViewById<TextView>(R.id.walletAddress).text = myPKTAddress
        }
        //Transactions
        val transactions = AnodeUtil.getCachedWalletTxns()
        makeListofTxns(transactions, transactions.length())
    }

    /**
     * get Transactions
     */
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun getWalletTransactions(nextPage: Boolean) {
        val listsLayout = root.findViewById<LinearLayout>(R.id.paymentsList)
        val params = JSONObject()
        if (!txnsLimitReached && nextPage) {
            txnsLimit *= 2
        }
        //Exclude mining transactions
        params.put("coinbase", 1)
        params.put("reversed", false)
        params.put("txnsSkip", 0)
        params.put("txnsLimit", txnsLimit)
        AnodeUtil.apiController.post(AnodeUtil.apiController.getTransactionsURL, params) { response ->
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

                val txnsSize = transactions.length()
                if (txnsLimit >= txnsSize) {
                    txnsLimitReached = true
                }
                /*if (txnsSize > 25) {
                    txnsSize = 25
                    root.findViewById<TextView>(R.id.texthistory).visibility = View.VISIBLE
                }*/
                AnodeUtil.setCacheWalletTxns(transactions)
                makeListofTxns(transactions, txnsSize)
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
        AnodeUtil.apiController.get(AnodeUtil.apiController.getBalanceURL) { response ->
            if (response != null) {
                val json = JSONObject(response.toString())
                val walletBalance = root.findViewById<TextView>(R.id.walletBalanceNumber)
                if (json.has("totalBalance")) {
                    val balance = AnodeUtil.satoshisToPKT(json.getString("totalBalance").toLong())
                    walletBalance.text = balance
                    AnodeUtil.setCacheWalletBalance(balance)
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
            val timeAgoText: String
            val diffSeconds = (System.currentTimeMillis() - bTimestamp) / 1000
            if (diffSeconds > 60) {
                val minutes = diffSeconds / 60
                if (minutes == (1).toLong()) {
                    timeAgoText = "$minutes "+getString(R.string.minute_ago)
                } else {
                    timeAgoText = "$minutes "+getString(R.string.minutes_ago)
                }
                if (diffSeconds > 1140) {//19min
                    if (resumedNeutrinoTop == neutrinoTop) {
                        statusBar.text = "waiting for data..."
                        return
                    } else {
                        statusIcon.setBackgroundResource(R.drawable.warning)
                    }
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
        l?.setBackgroundColor(mycontext.getColor(android.R.color.transparent))
    }

    private fun makeBackgroundWhite(){
        val layout = root.findViewById<ConstraintLayout>(R.id.fragment_wallet)
        layout.setBackgroundColor(mycontext.getColor(android.R.color.transparent))
    }

    private fun makeBackgroundGrey() {
        val layout = root.findViewById<ConstraintLayout>(R.id.fragment_wallet)
        layout.setBackgroundColor(mycontext.getColor(android.R.color.darker_gray))
    }

    private fun showLoading() {
        if (isAdded) {
            makeBackgroundGrey()
            val loading = root.findViewById<ProgressBar>(R.id.loadingAnimation)
            loading.visibility = View.VISIBLE
        }
    }

    private fun hideLoading() {
        if (isAdded) {
            makeBackgroundWhite()
            val loading = root.findViewById<ProgressBar>(R.id.loadingAnimation)
            loading.visibility = View.GONE
        }
    }

    private fun handlePldError(error: String): String {
        if (error.contains("[LightningServer] is not yet ready")) {
            return "Waiting for wallet server..."
        }
        return error
    }
}