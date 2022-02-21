package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class WalletFragmentMain : Fragment() {
    lateinit var statusBar: TextView
    lateinit var apiController: APIController
    private var walletUnlocked = false
    private var neutrinoSynced = false
    private var myPKTAddress = ""
    private val refreshValuesInterval: Long = 10000
    lateinit var h: Handler
    private var balanceLastTimeUpdated: Long = 0
    private var transactionsLastTimeUpdated: Long = 0
    private var passwordPromptActive = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.walletfragment_main, container, false)
    }

    override fun onResume() {
        super.onResume()
        if (this::h.isInitialized) {
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
        h = Handler(Looper.getMainLooper())
        val context = requireContext()
        AnodeClient.eventLog(context,"Activity: WalletFragmentMain created")
        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val walletFile = File(context.filesDir.toString() + "/pkt/wallet.db")
        if (!walletFile.exists()) {
            return
        }

        val anodeUtil = AnodeUtil(context)
        //Initialize handlers
        val service = ServiceVolley()
        apiController = APIController(service)
        refreshValues.init(v, anodeUtil)
        getPldInfo.init(v, anodeUtil)
        //set PKT address from shared preferences
        myPKTAddress = prefs.getString("lndwalletaddress", "").toString()

        //Init UI elements
        val walletAddress = v.findViewById<TextView>(R.id.walletAddress)
        walletAddress.text = myPKTAddress
        walletAddress.setOnClickListener {
            AnodeClient.eventLog(context, "Button: Copy wallet address clicked")
            Toast.makeText(context, "address has been copied", Toast.LENGTH_LONG).show()
        }
        val history =v.findViewById<TextView>(R.id.texthistory)
        history.setOnClickListener {
            AnodeClient.eventLog(context, "Button: Older transactions clicked")
            val transactionsActivity = Intent(context, TransactionHistoryActivity::class.java)
            startActivityForResult(transactionsActivity, 0)
        }
        statusBar = v.findViewById(R.id.textview_status)
        val sendPaymentButton = v.findViewById<Button>(R.id.button_sendPayment)
        //Disable it while trying to unlock wallet
        sendPaymentButton.isEnabled = false

        val shareButton = v.findViewById<Button>(R.id.walletAddressSharebutton)
        shareButton.setOnClickListener {
            AnodeClient.eventLog(context, "Button: Share wallet address clicked")
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "This is my PKT wallet address: $myPKTAddress")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        sendPaymentButton.setOnClickListener {
            AnodeClient.eventLog(context, "Button: Send PKT clicked")
            val sendPaymentActivity = Intent(context, SendPaymentActivity::class.java)
            startActivity(sendPaymentActivity)
        }
    }

    /**
     * getting pld info
     */
    private fun getInfo() {
        apiController.get(apiController.getInfoURL) { response ->
            if (response != null) {
                //Check if wallet is unlocked
                if (response.has("wallet") &&
                    !response.isNull("wallet") &&
                    response.getJSONObject("wallet").has("current_height")) {
                    val walletHeight = response.getJSONObject("wallet").getInt("current_height")
                    walletUnlocked = true
                }
                if (response.has("neutrino") && !response.isNull("neutrino") && response.getJSONObject("neutrino").has("height")) {
                    val neutrino = response.getJSONObject("neutrino")
                    if (neutrino.has("peers")) {
                        val peers = neutrino.getJSONArray("peers")
                        if (peers.length() > 0) {
                            val neutrinoTop = peers.getJSONObject(0).getInt("lastBlock")
                            val neutrinoHeight = neutrino.getInt("height")
                            //If neutrino current height is close to last block then we can try to unlock
                            //otherwise we will wait
                            if (neutrinoTop < (neutrinoHeight + 20)) {
                                neutrinoSynced = true
                                statusBar.text = "Chain synced"
                            } else {
                                neutrinoSynced = false
                                statusBar.text = "Syncing: $neutrinoHeight out of $neutrinoTop. Please wait"
                            }
                        }
                    }
                }
                //Keep getting updates
                if (this::h.isInitialized) {
                    h.postDelayed(getPldInfo, 5000)
                }
            } else {
                //TODO: post error
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
    private fun unlockWallet(v: View, a:AnodeUtil) {
        Log.i(LOGTAG, "Trying to unlock wallet")
        statusBar.text = "Trying to unlock wallet..."
        //Get encrypted password
        val walletPassword = a.getPasswordFromEncSharedPreferences()
        //If password is empty prompt user to enter new password
        if (walletPassword.isEmpty()) {
            if (!passwordPromptActive) {
                passwordPromptActive = true
                promptUserPassword(v, a)
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
                    a.storePassword("")
                    if (!passwordPromptActive) {
                        passwordPromptActive = true
                        promptUserPassword(v, a)
                    }
                    walletUnlocked = false
                } else if (response.length() == 0) {
                    //empty response is success
                    Log.i(LOGTAG, "Wallet unlocked")
                    walletUnlocked = true
                    //Update screen
                    updateUiWalletUnlocket(v)
                    //Wait a bit before making next call
                    Thread.sleep(300)
                    //Get new address
                    getNewPKTAddress(v)
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
     * Create alert dialog that prompts user to enter wallet password
     * and save it in encrypted shared preferences
     *
     * @param AnodeUtil
     */
    private fun promptUserPassword(v: View, a: AnodeUtil) {
        var password: String
        val builder: AlertDialog.Builder? = activity?.let { AlertDialog.Builder(it) }
        if (builder != null) {
            builder.setTitle("PKT Wallet")
            builder.setMessage("Please type your PKT Wallet password")
        }
        val input = EditText(activity)
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        input.layoutParams = lp
        if (builder != null) {
            builder.setView(input)
            input.transformationMethod = PasswordTransformationMethod.getInstance();
            builder.setPositiveButton("Submit",
                DialogInterface.OnClickListener { dialog, _ ->
                    password = input.text.toString()
                    dialog.dismiss()
                    if (password.isNotEmpty()) {
                        passwordPromptActive = false
                        //write password to encrypted shared preferences
                        a.storePassword(password)
                        //reset pkt wallet address
                        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                        prefs.edit().putString("lndwalletaddress", "").apply()
                        //try unlocking the wallet with new password
                        unlockWallet(v, a)
                    }
                })

            builder.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, _ ->
                    passwordPromptActive = false
                    dialog.dismiss()
                })
            val alert: AlertDialog = builder.create()
            alert.show()
        }
    }

    /**
     * Update layout elements to indicate that wallet is unlocked
     *
     * @param View
     */
    private fun updateUiWalletUnlocket(v: View) {
        statusBar.text = "Wallet unlocked"
        val layout = v.findViewById<ConstraintLayout>(R.id.wallet_fragmentMain)
        activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
        v.findViewById<Button>(R.id.button_sendPayment).isEnabled = true
    }

    /**
     * Get balance from wallet using REST call /v1/balance/blockchain
     * If it fails will keep trying...
     * On success it will update the screen
     *
     * @param View
     * @param AnodeUtil
     */
    private fun getBalance(v: View, a: AnodeUtil) {
        statusBar.text = "Retrieving wallet balance..."
        //Get Balance
        apiController.get(apiController.getBalanceURL) { response ->
            if (response != null) {
                val json = JSONObject(response.toString())
                val walletBalance = v.findViewById<TextView>(R.id.walletBalanceNumber)
                if (json.has("totalBalance")) {
                    walletBalance.text = a.satoshisToPKT(json.getString("totalBalance").toLong())
                    balanceLastTimeUpdated = System.currentTimeMillis()
                } else {
                    //getBalance(v, a)
                }
            } else {
                //getBalance(v, a)
            }
            statusBar.text = ""
        }
    }

    /**
     *
     */
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun getWalletTransactions(v: View, a: AnodeUtil) {
        val listsLayout = v.findViewById<LinearLayout>(R.id.paymentsList)
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
        val context = requireContext()
        var prevTransactions = 0
        var transactions = JSONArray()
        val params = JSONObject()
        //Exclude mining transactions
        params.put("coinbase", 1)
        statusBar.text = "Retrieving transactions..."
        apiController.post(apiController.getTransactionsURL, params) { response ->
            if ((response != null) && (!response.has("error"))) {
                transactionsLastTimeUpdated = System.currentTimeMillis()
                transactions = response.getJSONArray("transactions")
                if (transactions.length() == 0) return@post
                if (transactions.length() > prevTransactions) {
                    prevTransactions = transactions.length()
                    var tcount = transactions.length()
                    if (tcount > 25) {
                        tcount = 25
                    }

                    activity?.runOnUiThread {
                        listsLayout.removeAllViews()
                        for (i in 0 until tcount) {
                            val transaction = transactions.getJSONObject(i)

                            //Add new line
                            val line = ConstraintLayout(context)
                            line.setOnClickListener {
                                val transactiondetailsFragment: BottomSheetDialogFragment = TransactionDetailsFragment()
                                val bundle = Bundle()
                                bundle.putString("txid", transaction.getString("txHash"))
                                /*
                                for (a in 0 until transaction.getJSONArray("dest_addresses")
                                    .length()) {
                                    //Remove any addresses that are from the same wallet
                                    if (!myAddresses.contains(
                                            transaction.getJSONArray("dest_addresses")
                                                .getString(a)
                                        )
                                    ) {
                                        bundle.putString(
                                            "address$a",
                                            transaction.getJSONArray("dest_addresses")
                                                .getString(a)
                                        )
                                    }
                                }*/
                                bundle.putLong("amount", transaction.getString("amount").toLong())
                                bundle.putInt("blockheight", transaction.getInt("blockHeight"))
                                bundle.putString("blockhash", transaction.getString("blockHash"))
                                //Do not include confirmations
                                //bundle.putInt("confirmations", transactions[i].numConfirmations)
                                transactiondetailsFragment.arguments = bundle
                                transactiondetailsFragment.show(requireActivity().supportFragmentManager, "")
                            }
                            line.id = i
                            //line.orientation = LinearLayout.HORIZONTAL
                            val llParams = ConstraintLayout.LayoutParams(
                                ConstraintLayout.LayoutParams.MATCH_PARENT,
                                ConstraintLayout.LayoutParams.WRAP_CONTENT
                            )
                            line.layoutParams = llParams
                            line.setPadding(10, 20, 10, 20)

                            //ADDRESS
                            val textAddress = TextView(context)
                            textAddress.id = View.generateViewId()
                            textAddress.width = 350

                            //In/Out Icon
                            val icon = ImageView(context)
                            icon.id = View.generateViewId()
                            val amount = transaction.getString("amount").toLong()

                            if (amount < 0) {
                                icon.setBackgroundResource(R.drawable.ic_baseline_arrow_upward_24)
                                val destAddress = transaction.getJSONArray("destAddresses").getString(0)
                                textAddress.text = destAddress.substring(0, 6) + "..." + destAddress.substring(destAddress.length - 8)
                            } else {
                                icon.setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
                                textAddress.text = ""
                                //If address is same as our address get next one
                                /*if (transactions[i].destAddressesList[0] == myaddress) {
                                textaddress.text =
                                    transactions[i].destAddressesList[1].substring(0,6) + "..." + transactions[i].destAddressesList[1].substring(
                                        transactions[i].destAddressesList[0].length - 8)
                            } else {
                                //Show first address
                                textaddress.text =
                                    transactions[i].destAddressesList[0].substring(0,6) + "..." + transactions[i].destAddressesList[1].substring(
                                        transactions[i].destAddressesList[0].length - 8)
                            }*/
                            }
                            line.addView(textAddress)
                            line.addView(icon)
                            //AMOUNT
                            val textAmount = TextView(context)
                            textAmount.id = View.generateViewId()
                            textAmount.width = 350
                            textAmount.text = a.satoshisToPKT(amount)
                            line.addView(textAmount)
                            //DATE
                            val textDate = TextView(context)
                            textDate.id = View.generateViewId()
                            textDate.text = simpleDate.format(Date(transaction.getString("timeStamp").toLong() * 1000))
                            line.addView(textDate)
                            val set = ConstraintSet()
                            set.clear(textAddress.id)
                            set.clear(icon.id)
                            set.clear(textAmount.id)
                            set.clear(textDate.id)
                            //address with start
                            set.connect(textAddress.id, ConstraintSet.START, line.id, ConstraintSet.START, 10)
                            //date with end
                            set.connect(textDate.id, ConstraintSet.END, line.id, ConstraintSet.END, 10)
                            //address with icon
                            set.connect(icon.id, ConstraintSet.START, textAddress.id, ConstraintSet.END, 0)
                            set.connect(textAddress.id, ConstraintSet.END, icon.id, ConstraintSet.START, 0)
                            //icon with amount
                            set.connect(textAmount.id, ConstraintSet.START, icon.id, ConstraintSet.END, 0)
                            set.connect(icon.id, ConstraintSet.END, textAmount.id, ConstraintSet.START, 0)
                            //amount with date
                            set.connect(textDate.id,ConstraintSet.START,textAmount.id,ConstraintSet.END, 0 )
                            set.connect(textAmount.id, ConstraintSet.END, textDate.id, ConstraintSet.START, 0)
                            val chainViews = intArrayOf(textAddress.id, textDate.id)
                            val chainWeights = floatArrayOf(0f, 0f)
                            set.createHorizontalChain(ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, chainViews, chainWeights, ConstraintSet.CHAIN_SPREAD)
                            set.constrainWidth(textAddress.id, ConstraintSet.WRAP_CONTENT)
                            set.constrainWidth(icon.id, ConstraintSet.WRAP_CONTENT)
                            set.constrainWidth(textAmount.id, ConstraintSet.WRAP_CONTENT)
                            set.constrainWidth(textDate.id, ConstraintSet.WRAP_CONTENT)
                            set.constrainHeight(textAddress.id, ConstraintSet.WRAP_CONTENT)
                            set.constrainHeight(icon.id, ConstraintSet.WRAP_CONTENT)
                            set.constrainHeight(textAmount.id, ConstraintSet.WRAP_CONTENT)
                            set.constrainHeight(textDate.id, ConstraintSet.WRAP_CONTENT)
                            set.applyTo(line)
                            listsLayout.addView(line)
                        }
                        //If more than 25 transactions show link to older transactions activity
                        if (transactions.length() > 25) {
                            v.findViewById<TextView>(R.id.texthistory).visibility = View.VISIBLE
                        }
                    }
                }
            }
            statusBar.text = ""
        }
    }

    private val getPldInfo = object : Runnable {
        lateinit var v: View
        lateinit var a: AnodeUtil

        fun init(view: View, anodeutil: AnodeUtil)  {
            v = view
            a = anodeutil
        }

        override fun run() {
            getInfo()
            if (walletUnlocked) {
                h.postDelayed(refreshValues, refreshValuesInterval)
            } else {
                unlockWallet(v, a)
            }
        }
    }

    private val refreshValues = object : Runnable {
        lateinit var v: View
        lateinit var a: AnodeUtil

        fun init(view: View, anodeutil: AnodeUtil)  {
            v = view
            a = anodeutil
        }
        override fun run() {
            if (walletUnlocked) {
                if (myPKTAddress == "") {
                    getNewPKTAddress(v)
                }
                if ((System.currentTimeMillis()-5000) > balanceLastTimeUpdated) {
                    getBalance(v, a)
                }
                if ((System.currentTimeMillis()-5000) > transactionsLastTimeUpdated) {
                    getWalletTransactions(v, a)
                }
            }
        }
    }
}

