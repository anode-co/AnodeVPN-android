package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import lnrpc.Rpc
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class WalletFragmentMain : Fragment() {
    lateinit var statusBar: TextView
    lateinit var apiController: APIController
    private val baseRestAPIURL = "http://localhost:8080"
    private val getInfo2URL = "$baseRestAPIURL/pkt/v1/getinfo2"
    private val getBalanceURL = "$baseRestAPIURL/v1/balance/blockchain"
    private val getNewAddressURL = "$baseRestAPIURL/pkt/v1/getnewaddress/false"
    private val unlockWalletURL = "$baseRestAPIURL/v1/unlockwallet"
    private val getTransactionsURL = "$baseRestAPIURL/v1/transactions"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.walletfragment_main, container, false)
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        AnodeClient.eventLog(requireContext(),"Activity: WalletFragmentMain created")
        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val walletFile = File(requireContext().filesDir.toString() + "/pkt/wallet.db")
        if (!walletFile.exists()) {
            return
        }
        //Initialize Volley Service
        val service = ServiceVolley()
        apiController = APIController(service)
        val params = JSONObject()
        Log.i(LOGTAG, "WalletFragmentMain getting wallet details")
        val anodeUtil = AnodeUtil(null)
        var myaddress = prefs.getString("lndwalletaddress", "")
        val walletAddress = v.findViewById<TextView>(R.id.walletAddress)
        walletAddress.text = myaddress
        walletAddress.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Copy wallet address clicked")
            Toast.makeText(context, "address has been copied", Toast.LENGTH_LONG).show()
        }
        val history = v.findViewById<TextView>(R.id.texthistory)

        history.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Older transactions clicked")
            val transactionsActivity = Intent(context, TransactionHistoryActivity::class.java)
            startActivityForResult(transactionsActivity, 0)
        }
        statusBar = v.findViewById(R.id.textview_status)
        val walletBalance = v.findViewById<TextView>(R.id.walletBalanceNumber)
        val listsLayout = v.findViewById<LinearLayout>(R.id.paymentsList)
        val context = requireContext()
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
        val sendPaymentButton = v.findViewById<Button>(R.id.button_sendPayment)
        //Disable it while trying to unlock wallet
        sendPaymentButton.isEnabled = false

        //Updating main wallet screen every 10 secs
        Thread({
            var failedToUnlock = 0
            var prevTransactions = 0
            var walletResult: String
            var sleepInterval: Long = 1000
            //Try unlocking the wallet
            activity?.runOnUiThread {
                statusBar.text = "Trying to unlock wallet..."
            }
            //Check that fragment is added to the activity before trying to open the wallet
            while ((isAdded) && (!prefs.getBoolean("lndwalletopened", false))) {
                walletResult = openPKTWallet()
                if (walletResult.contains("ErrWrongPassphrase")) {
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
                        requireActivity().runOnUiThread {
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
                                            Toast.makeText(requireActivity(),"PKT wallet is open",Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(requireActivity(),"Wrong password.",Toast.LENGTH_LONG).show()
                                        }
                                    }
                                })

                            builder.setNegativeButton(
                                "Cancel",
                                DialogInterface.OnClickListener { dialog, _ ->
                                    dialog.dismiss()
                                })
                            val alert: AlertDialog = builder.create()
                            alert.show()
                        }
                    }
                    sleepInterval = 10000
                }
                failedToUnlock++
                if (failedToUnlock > 60) {
                   val pldStatus = LndRPCController.isPldRunning()
                   throw LndRPCException("Error unlocking wallet: $walletResult. PLD status: $pldStatus")
                }
                Thread.sleep(sleepInterval)
            }
            activity?.runOnUiThread {
                statusBar.text = "Wallet unlocked"
                val layout = v.findViewById<ConstraintLayout>(R.id.wallet_fragmentMain)
                activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
                sendPaymentButton.isEnabled = true
            }
            //Refresh UI with wallet values
            while(true) {
                if (prefs.getBoolean("lndwalletopened", false)) {
                    val tmpMyAddresses: MutableList<Rpc.GetAddressBalancesResponseAddr>? = LndRPCController.getAddresses()
                    val myAddresses: MutableList<String> = mutableListOf()
                    if (tmpMyAddresses != null) {
                        for (i in tmpMyAddresses.indices) {
                            myAddresses.add(tmpMyAddresses[i].address)
                        }
                    }
                    activity?.runOnUiThread {
                        statusBar.text = ""
                    }
                    if (myaddress == "") {
                        myaddress = if (myAddresses.size == 0) {
                            LndRPCController.generateAddress(context)
                        } else {
                            myAddresses[0]
                        }
                        prefs.edit().putString("lndwalletaddress", myaddress).apply()
                        activity?.runOnUiThread {
                            walletAddress.text = myaddress
                        }
                    }
                    var transactions = JSONArray()
                    params.put("coinbase", 1)
                    apiController.post(getTransactionsURL, params) { response ->
                        transactions = response!!.getJSONArray("transactions")
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
                                        bundle.putString("txid", transaction.getString("tx_hash"))
                                        for (a in 0 until transaction.getJSONArray("dest_addresses").length()) {
                                            //Remove any addresses that are from the same wallet
                                            if (!myAddresses.contains(transaction.getJSONArray("dest_addresses").getString(a))) {
                                                bundle.putString("address$a", transaction.getJSONArray("dest_addresses").getString(a))
                                            }
                                        }
                                        bundle.putLong("amount", transaction.getString("amount").toLong())
                                        bundle.putInt("blockheight", transaction.getInt("block_height"))
                                        bundle.putString("blockhash", transaction.getString("block_hash"))
                                        //Do not include confirmations
                                        //bundle.putInt("confirmations", transactions[i].numConfirmations)
                                        transactiondetailsFragment.arguments = bundle
                                        transactiondetailsFragment.show(requireActivity().supportFragmentManager,"")
                                    }
                                    line.id = i
                                    //line.orientation = LinearLayout.HORIZONTAL
                                    val llParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                                    line.layoutParams = llParams
                                    line.setPadding(10, 20, 10, 20)

                                    //ADDRESS
                                    val textAddress = TextView(context)
                                    textAddress.id = View.generateViewId()
                                    textAddress.width = 350

                                    //In/Out Icon
                                    val icon = ImageView(context)
                                    icon.id = View.generateViewId()
                                    var amount: Float = transaction.getString("amount").toFloat()
                                    val onePKT = 1073741824
                                    val mPKT = 1073741.824F
                                    val uPKT  = 1073.741824F
                                    val nPKT  = 1.073741824F
                                    if (amount < 0) {
                                        icon.setBackgroundResource(R.drawable.ic_baseline_arrow_upward_24)
                                        val destAddress = transaction.getJSONArray("dest_addresses").getString(0)
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
                                    if (amount > 1000000000) {
                                        amount /= onePKT
                                        textAmount.text = "PKT %.2f".format(amount)
                                    } else if (amount > 1000000) {
                                        amount /= mPKT
                                        textAmount.text = "mPKT %.2f".format(amount)
                                    } else if (amount > 1000) {
                                        amount /= uPKT
                                        textAmount.text = "μPKT %.2f".format(amount)
                                    } else if (amount < 1000000000) {
                                        amount /= onePKT
                                        textAmount.text = "PKT %.2f".format(amount)
                                    } else if (amount < 1000000) {
                                        amount /= mPKT
                                        textAmount.text = "mPKT %.2f".format(amount)
                                    } else if (amount < 1000) {
                                        amount /= uPKT
                                        textAmount.text = "μPKT %.2f".format(amount)
                                    } else {
                                        amount /= nPKT
                                        textAmount.text = "nPKT %.2f".format(amount)
                                    }
                                    line.addView(textAmount)
                                    //DATE
                                    val textDate = TextView(context)
                                    textDate.id = View.generateViewId()
                                    textDate.text =
                                        simpleDate.format(Date(transaction.getString("time_stamp").toLong() * 1000))
                                    line.addView(textDate)

                                    val set = ConstraintSet()
                                    set.clear(textAddress.id)
                                    set.clear(icon.id)
                                    set.clear(textAmount.id)
                                    set.clear(textDate.id)
                                    //address with start
                                    set.connect(textAddress.id,ConstraintSet.START,line.id,ConstraintSet.START,10)
                                    //date with end
                                    set.connect(textDate.id,ConstraintSet.END,line.id,ConstraintSet.END,10)
                                    //address with icon
                                    set.connect(icon.id, ConstraintSet.START, textAddress.id, ConstraintSet.END, 0)
                                    set.connect(textAddress.id, ConstraintSet.END, icon.id, ConstraintSet.START, 0)
                                    //icon with amount
                                    set.connect(textAmount.id, ConstraintSet.START, icon.id, ConstraintSet.END, 0)
                                    set.connect(icon.id, ConstraintSet.END, textAmount.id, ConstraintSet.START, 0)
                                    //amount with date
                                    set.connect(textDate.id, ConstraintSet.START, textAmount.id, ConstraintSet.END, 0)
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
                                    history.visibility = View.VISIBLE
                                }
                            }
                        }
                    }

                    //Get Balance
                    apiController.get(getBalanceURL) { response ->
                        val json = JSONObject(response.toString())
                        if (json.has("total_balance")) {
                            walletBalance.text = anodeUtil.satoshisToPKT(json.getString("total_balance").toLong())
                        }
                    }
                }
                //Refresh every 10secs
                Thread.sleep(10000)
            }
        }, "WalletFragmentMain.RefreshValues").start()

        val shareButton = v.findViewById<Button>(R.id.walletAddressSharebutton)
        shareButton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Share wallet address clicked")
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "This is my PKT wallet address: $myaddress")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        sendPaymentButton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Send PKT clicked")
            val sendPaymentActivity = Intent(context, SendPaymentActivity::class.java)
            startActivity(sendPaymentActivity)
        }
    }

    private fun openPKTWallet(): String {
        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
        Log.i(LOGTAG, "MainActivity trying to open wallet")
        var walletPassword = getPasswordFromEncSharedPreferences()
        var jsonRequest = JSONObject()

        val b64Password = android.util.Base64.encodeToString(walletPassword.toByteArray(), android.util.Base64.DEFAULT)
        jsonRequest.put("wallet_password", b64Password)
        apiController.post(unlockWalletURL,jsonRequest) { response ->
            //Handle unlockwallet REST call response
            //TODO: ...
        }

        if (result.contains("ErrWrongPassphrase")) {
            return result
        } else if (result != "OK") {
            //can not open wallet
            Log.w(LOGTAG, "Can not open PKT wallet")
            //wrong password prompt user to type password again
            val dataDir = File(requireActivity().filesDir.toString() + "/lnd/data/chain/pkt/mainnet")
            var checkWallet = result
            if (!dataDir.exists()) {
                Log.e(LOGTAG, "expected folder structure not available")
                checkWallet += " datadir does not exist "
            } else {
                checkWallet += " wallet.db exists "
            }
            if (prefs.getString("walletpassword", "").isNullOrEmpty()) {
                Log.e(LOGTAG, "walletpassword in shared preferences is empty")
                checkWallet += " walletpassword is empty"
            } else {
                checkWallet += " walletpassword is not empty"
            }
            val status = LndRPCController.isPldRunning()
            checkWallet += " PLD status: $status"
            return checkWallet
        } else if (result == "OK") {
            return result
        }
        return result
    }

    private fun getPasswordFromEncSharedPreferences(): String {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        val masterKey = MasterKey.Builder(requireContext())
            .setKeyGenParameterSpec(spec)
            .build()
        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                requireContext(),
                "co.anode.anodium-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        val password : String? = encSharedPreferences.getString("wallet_password", "")
        if (password.isNullOrEmpty()) {
            return ""
        } else {
            return password
        }
    }
}

