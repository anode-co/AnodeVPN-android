package co.anode.anodium

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
import com.github.lightningnetwork.lnd.lnrpc.Transaction
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class WalletFragmentMain : Fragment() {
    private var walletlocked = true

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.walletfragment_main, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)
        AnodeClient.eventLog(requireContext(),"Activity: WalletFragmentMain created")
        val prefs = requireContext().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val walletfile = File(requireContext().filesDir.toString() + "/lnd/data/chain/pkt/mainnet/wallet.db")
        if (!walletfile.exists()) {
            return
        }
        Log.i(LOGTAG, "WalletFragmentMain getting wallet details")

        var myaddress = prefs.getString("lndwalletaddress", "")

        val walletAddress = v.findViewById<TextView>(R.id.walletAddress)
        walletAddress.text = myaddress
        walletAddress.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Copy wallet address clicked")
            Toast.makeText(context, "address has been copied", Toast.LENGTH_LONG)
        }
        val history = v.findViewById<TextView>(R.id.texthistory)

        history.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Older transactions clicked")
            val transactionsActivity = Intent(context, TransactionHistoryActivity::class.java)
            startActivityForResult(transactionsActivity, 0)
        }

        val walletBalance = v.findViewById<TextView>(R.id.walletBalanceNumber)
        walletBalance.text = "0.0"
        val listsLayout = v.findViewById<LinearLayout>(R.id.paymentsList)
        val context = requireContext()
        var prevtransactions : MutableList<Transaction> = ArrayList()
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")

        //Updating main wallet screen every 10 secs
        Thread(Runnable {
            var prevtransactions = 0
            //Try unlocking the wallet
            while (walletlocked) {
                openPKTWallet()
                Thread.sleep(1000)
            }
            //Refresh UI with wallet values
            while(true) {
                if (prefs.getBoolean("lndwalletopened", false)) {
                    if (myaddress == "") {
                        myaddress = LndRPCController.generateAddress()
                        with(prefs.edit()) {
                            putString("lndwalletaddress", myaddress)
                            commit()
                        }
                        walletAddress.text = myaddress
                    }
                    var transactions = LndRPCController.getTransactions()
                    if (transactions.count() > prevtransactions) {
                        prevtransactions = transactions.count()
                        var tcount = transactions.count()
                        if (tcount > 25) {
                            tcount = 25
                        }
                        activity?.runOnUiThread {
                            listsLayout.removeAllViews()
                            //Set balance
                            walletBalance.text = "%.2f".format(LndRPCController.getTotalBalance())
                            for (i in 0 until tcount) {
                                //Add new line
                                var line = ConstraintLayout(context)
                                line.setOnClickListener {
                                    val transactiondetailsFragment: BottomSheetDialogFragment =
                                        TransactionDetailsFragment()
                                    val bundle = Bundle()
                                    bundle.putString("txid", transactions[i].txHash)
                                    for (a in 0 until transactions[i].destAddressesCount) {
                                        bundle.putString(
                                            "address$a",
                                            transactions[i].getDestAddresses(a)
                                        )
                                    }
                                    bundle.putLong("amount", transactions[i].amount)
                                    bundle.putInt("blockheight", transactions[i].blockHeight)
                                    bundle.putString("blockhash", transactions[i].blockHash)
                                    bundle.putInt("confirmations", transactions[i].numConfirmations)
                                    transactiondetailsFragment.arguments = bundle
                                    transactiondetailsFragment.show(
                                        requireActivity().supportFragmentManager,
                                        ""
                                    )
                                }
                                line.id = i
                                //line.orientation = LinearLayout.HORIZONTAL
                                val llparams =
                                    ConstraintLayout.LayoutParams(
                                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                                    )
                                line.layoutParams = llparams
                                line.setPadding(10, 20, 10, 20)

                                //ADDRESS
                                val textaddress = TextView(context)
                                textaddress.id = View.generateViewId()
                                textaddress.width = 450

                                //In/Out Icon
                                val icon = ImageView(context)
                                icon.id = View.generateViewId()
                                val amount: Float = transactions[i].amount.toFloat() / 1073741824
                                if (amount < 0) {
                                    icon.setBackgroundResource(R.drawable.ic_baseline_arrow_upward_24)
                                    textaddress.text =
                                        transactions[i].destAddressesList[0].substring(0, 6) + "..." + transactions[i].destAddressesList[0].substring(
                                            transactions[i].destAddressesList[0].length - 8)
                                } else {
                                    icon.setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
                                    //If address is same as our address get next one
                                    if (transactions[i].destAddressesList[0] == myaddress) {
                                        textaddress.text =
                                            transactions[i].destAddressesList[1].substring(0,6) + "..." + transactions[i].destAddressesList[1].substring(
                                                transactions[i].destAddressesList[0].length - 8)
                                    } else {
                                        //Show first address
                                        textaddress.text =
                                            transactions[i].destAddressesList[0].substring(0,6) + "..." + transactions[i].destAddressesList[1].substring(
                                                transactions[i].destAddressesList[0].length - 8)
                                    }
                                }
                                line.addView(textaddress)
                                line.addView(icon)
                                //AMOUNT
                                val textamount = TextView(context)
                                textamount.id = View.generateViewId()
                                textamount.width = 250
                                textamount.text = "PKT %.2f".format(amount)
                                line.addView(textamount)
                                //DATE
                                val textDate = TextView(context)
                                textDate.id = View.generateViewId()
                                textDate.text =
                                    simpleDate.format(Date(transactions[i].timeStamp * 1000))
                                line.addView(textDate)

                                val set = ConstraintSet()
                                set.clear(textaddress.id)
                                set.clear(icon.id)
                                set.clear(textamount.id)
                                set.clear(textDate.id)
                                //address with start
                                set.connect(
                                    textaddress.id,
                                    ConstraintSet.START,
                                    line.id,
                                    ConstraintSet.START,
                                    10
                                )
                                //date with end
                                set.connect(
                                    textDate.id,
                                    ConstraintSet.END,
                                    line.id,
                                    ConstraintSet.END,
                                    10
                                )
                                //address with icon
                                set.connect(
                                    icon.id,
                                    ConstraintSet.START,
                                    textaddress.id,
                                    ConstraintSet.END,
                                    0
                                )
                                set.connect(
                                    textaddress.id,
                                    ConstraintSet.END,
                                    icon.id,
                                    ConstraintSet.START,
                                    0
                                )
                                //icon with amount
                                set.connect(
                                    textamount.id,
                                    ConstraintSet.START,
                                    icon.id,
                                    ConstraintSet.END,
                                    0
                                )
                                set.connect(
                                    icon.id,
                                    ConstraintSet.END,
                                    textamount.id,
                                    ConstraintSet.START,
                                    0
                                )
                                //amount with date
                                set.connect(
                                    textDate.id,
                                    ConstraintSet.START,
                                    textamount.id,
                                    ConstraintSet.END,
                                    0
                                )
                                set.connect(
                                    textamount.id,
                                    ConstraintSet.END,
                                    textDate.id,
                                    ConstraintSet.START,
                                    0
                                )
                                val chainViews = intArrayOf(textaddress.id, textDate.id)
                                val chainWeights = floatArrayOf(0f, 0f)
                                set.createHorizontalChain(
                                    ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
                                    ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
                                    chainViews, chainWeights,
                                    ConstraintSet.CHAIN_SPREAD
                                )
                                set.constrainWidth(textaddress.id, ConstraintSet.WRAP_CONTENT)
                                set.constrainWidth(icon.id, ConstraintSet.WRAP_CONTENT)
                                set.constrainWidth(textamount.id, ConstraintSet.WRAP_CONTENT)
                                set.constrainWidth(textDate.id, ConstraintSet.WRAP_CONTENT)
                                set.constrainHeight(textaddress.id, ConstraintSet.WRAP_CONTENT)
                                set.constrainHeight(icon.id, ConstraintSet.WRAP_CONTENT)
                                set.constrainHeight(textamount.id, ConstraintSet.WRAP_CONTENT)
                                set.constrainHeight(textDate.id, ConstraintSet.WRAP_CONTENT)
                                set.applyTo(line)
                                listsLayout.addView(line)
                            }
                            //If more than 25 transactions show link to older transactions activity
                            if (transactions.count() > 25) {
                                history.visibility = View.VISIBLE
                            }
                        }
                    }
                    //If there were no transactions it may have been due to error so try again sooner (1sec)
                    var sleepInterval:Long = 10000
                    if (transactions.count() == 0) {
                        sleepInterval = 1000
                    }
                    Thread.sleep(sleepInterval)
                } else {
                    Thread.sleep(500)
                }
            }
        }, "WalletFragmentMain.RefreshValues").start()

        val sharebutton = v.findViewById<Button>(R.id.walletAddressSharebutton)
        sharebutton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Share wallet address clicked")
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "This is my PKT wallet address: $myaddress")
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        val sendpaymentButton = v.findViewById<Button>(R.id.button_sendPayment)
        sendpaymentButton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Send PKT clicked")
            val sendPaymentActivity = Intent(context, SendPaymentActivity::class.java)
            startActivity(sendPaymentActivity)
        }
    }


    fun openPKTWallet(): Boolean {
        val prefs = requireContext().getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
        Log.i(LOGTAG, "MainActivity trying to open wallet")
        var result = LndRPCController.openWallet(prefs)
        if (result.contains("ErrWrongPassphrase")) {
            var password = ""
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
                                    with(prefs.edit()) {
                                        putBoolean("lndwalletopened", true)
                                        commit()
                                    }
                                    Toast.makeText(requireActivity(),"PKT wallet is open",Toast.LENGTH_LONG).show()
                                } else {
                                    with(prefs.edit()) {
                                        putBoolean("lndwalletopened", false)
                                        commit()
                                    }
                                    Toast.makeText(requireActivity(),"Wrong password.",Toast.LENGTH_LONG).show()
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
                File(requireContext().filesDir.toString() + "/lnd/data/chain/pkt/mainnet")
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
            return false
        } else if (result == "OK") {
            walletlocked = false
            with(prefs.edit()) {
                putBoolean("lndwalletopened", true)
                commit()
            }
            return true
        }
        return false
    }
}

