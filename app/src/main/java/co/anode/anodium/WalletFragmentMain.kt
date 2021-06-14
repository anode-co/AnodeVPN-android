package co.anode.anodium

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.marginBottom
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.github.lightningnetwork.lnd.lnrpc.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class WalletFragmentMain : Fragment() {
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
        if (myaddress == "") {
            myaddress = LndRPCController.generateAddress()
            with(prefs.edit()) {
                putString("lndwalletaddress", myaddress)
                commit()
            }
        }
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
        val listsLayout = v.findViewById<LinearLayout>(R.id.paymentsList)
        val context = requireContext()
        var prevtransactions : MutableList<Transaction> = ArrayList()
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
        var transactions = LndRPCController.getTransactions()

        //Updating main wallet screen every 10 secs
        Thread(Runnable {
            var prevtransactions = 0
            while(true) {
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
                                //TODO: open transaction details activity
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
                                textaddress.text = transactions[i].destAddressesList[0].substring(0, 6) + "..." + transactions[i].destAddressesList[0].substring(transactions[i].destAddressesList[0].length-8)
                            } else {
                                icon.setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
                                //TODO: get next address
                                if (transactions[i].destAddressesList[0] == myaddress) {
                                    textaddress.text = transactions[i].destAddressesList[1].substring(0, 6) + "..." + transactions[i].destAddressesList[1].substring(transactions[i].destAddressesList[0].length-8)
                                }
                            }
                            line.addView(textaddress)
                            line.addView(icon)
                            //AMOUNT
                            val textamount = TextView(context)
                            textamount.id = View.generateViewId()
                            textamount.width = 250
                            textamount.text = "PKT%.2f".format(amount)
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
                Thread.sleep(10000)
            }
        }, "SendPaymentActivity.RefreshValues").start()

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
}

