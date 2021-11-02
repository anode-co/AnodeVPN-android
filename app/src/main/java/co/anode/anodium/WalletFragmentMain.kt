package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
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
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import lnrpc.Rpc
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class WalletFragmentMain : Fragment() {
    lateinit var statusBar: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.walletfragment_main, container, false)
    }

    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        AnodeClient.eventLog(requireContext(),"Activity: WalletFragmentMain created")
        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val walletFile = File(requireContext().filesDir.toString() + "/lnd/data/chain/pkt/mainnet/wallet.db")
        if (!walletFile.exists()) {
            return
        }
        Log.i(LOGTAG, "WalletFragmentMain getting wallet details")

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
            while (!prefs.getBoolean("lndwalletopened", false)) {
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
                   val pltdStatus = LndRPCController.isPltdRunning()
                   throw LndRPCException("Error unlocking wallet: $walletResult. PLTD status: $pltdStatus")
                }
                Thread.sleep(sleepInterval)
            }
            activity?.runOnUiThread {
                statusBar.text = "Wallet unlocked"
                val layout = v.findViewById<ConstraintLayout>(R.id.wallet_fragmentMain)
                activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
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
                            LndRPCController.generateAddress()
                        } else {
                            myAddresses[0]
                        }
                        prefs.edit().putString("lndwalletaddress", myaddress).apply()
                        activity?.runOnUiThread {
                            walletAddress.text = myaddress
                        }
                    }
                    val transactions = LndRPCController.getTransactions()
                    if (transactions.count() > prevTransactions) {
                        prevTransactions = transactions.count()
                        var tcount = transactions.count()
                        if (tcount > 25) {
                            tcount = 25
                        }
                        activity?.runOnUiThread {
                            listsLayout.removeAllViews()
                            //Set balance
                            val balance = LndRPCController.getTotalBalance()
                            if ( balance < 0) {
                                //Error in getting balance
                                statusBar.text = "Error in retrieving balance"
                                walletBalance.text = "0"
                            } else {
                                walletBalance.text = "%.2f".format(balance)
                            }
                            for (i in 0 until tcount) {
                                //Add new line
                                val line = ConstraintLayout(context)
                                line.setOnClickListener {
                                    val transactiondetailsFragment: BottomSheetDialogFragment = TransactionDetailsFragment()
                                    val bundle = Bundle()
                                    bundle.putString("txid", transactions[i].txHash)
                                    for (a in 0 until transactions[i].destAddressesCount) {
                                        //Remove any addresses that are from the same wallet
                                        if (!myAddresses.contains(transactions[i].getDestAddresses(a))) {
                                            bundle.putString("address$a", transactions[i].getDestAddresses(a))
                                        }
                                    }
                                    bundle.putLong("amount", transactions[i].amount)
                                    bundle.putInt("blockheight", transactions[i].blockHeight)
                                    bundle.putString("blockhash", transactions[i].blockHash)
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
                                var amount: Float = transactions[i].amount.toFloat()
                                val onePKT = 1073741824
                                val mPKT = 1073741.824F
                                val uPKT  = 1073.741824F
                                val nPKT  = 1.073741824F
                                if (amount < 0) {
                                    icon.setBackgroundResource(R.drawable.ic_baseline_arrow_upward_24)
                                    textAddress.text = transactions[i].destAddressesList[0].substring(0, 6) + "..." + transactions[i].destAddressesList[0].substring(transactions[i].destAddressesList[0].length - 8)
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
                                    simpleDate.format(Date(transactions[i].timeStamp * 1000))
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
                            if (transactions.count() > 25) {
                                history.visibility = View.VISIBLE
                            }
                        }
                    } else {
                        var balance = LndRPCController.getTotalBalance()
                        if ( balance < 0) {
                            //Error in getting balance
                            statusBar.text = "Error in retrieving balance"
                            balance = 0.0F
                        }
                        activity?.runOnUiThread {
                            walletBalance.text = "%.2f".format(balance)
                        }
                    }
                    //If there were no transactions it may have been due to error so try again sooner (2sec)
                    sleepInterval = 10000
                    if (transactions.count() == 0) {
                        sleepInterval = 1000
                    }
                    Thread.sleep(sleepInterval)
                } else {
                    Thread.sleep(500)
                }
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

        val sendPaymentButton = v.findViewById<Button>(R.id.button_sendPayment)
        sendPaymentButton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Send PKT clicked")
            val sendPaymentActivity = Intent(context, SendPaymentActivity::class.java)
            startActivity(sendPaymentActivity)
        }
    }


    private fun openPKTWallet(): String {
        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
        Log.i(LOGTAG, "MainActivity trying to open wallet")
        val result = LndRPCController.openWallet(prefs)
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
            val status = LndRPCController.isPltdRunning()
            checkWallet += " PLTD status: $status"
            return checkWallet
        } else if (result == "OK") {
            return result
        }
        return result
    }
}

