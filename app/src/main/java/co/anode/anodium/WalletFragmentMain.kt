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

        var address = prefs.getString("lndwalletaddress", "")
        if (address == "") {
            address = LndRPCController.generateAddress()
            with(prefs.edit()) {
                putString("lndwalletaddress", address)
                commit()
            }
        }
        val walletAddress = v.findViewById<TextView>(R.id.walletAddress)
        walletAddress.text = address
        walletAddress.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Copy wallet address clicked")
            Toast.makeText(context, "address has been copied", Toast.LENGTH_LONG)
        }

        val parentlayout = v.findViewById<ConstraintLayout>(R.id.wallet_fragmentMain)

        val walletBalance = v.findViewById<TextView>(R.id.walletBalanceNumber)
        val listsLayout = v.findViewById<LinearLayout>(R.id.paymentsList)
        val context = requireContext()
        var prevtransactions : MutableList<Transaction> = ArrayList()
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
        var transactions = LndRPCController.getTransactions()

        if (transactions.count() > prevtransactions.count()) {
            prevtransactions = transactions
            var tcount = transactions.count()
            if (tcount > 25) {
                tcount = 25
            }
            for (i in 0 until tcount) {
                //Add new line
                var line = ConstraintLayout(context)
                line.id = i
                //line.orientation = LinearLayout.HORIZONTAL
                val llparams =
                    ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.MATCH_PARENT,
                        ConstraintLayout.LayoutParams.WRAP_CONTENT
                    )
                line.layoutParams = llparams
                line.setPadding(10, 10, 10, 10)

                //ADDRESS
                val textaddress = TextView(context)
                textaddress.id = View.generateViewId()
                textaddress.text = transactions[i].destAddressesList[0].substring(4,20) +"..."
                line.addView(textaddress)
                //In/Out Icon
                val icon = ImageView(context)
                icon.id = View.generateViewId()
                val amount:Float = transactions[i].amount.toFloat()/ 1073741824
                if (amount < 0) {
                    icon.setBackgroundResource(R.drawable.ic_baseline_arrow_upward_24)
                } else {
                    icon.setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
                }
                line.addView(icon)
                //AMOUNT
                val textamount = TextView(context)
                textamount.id = View.generateViewId()
                textamount.text = "PKT%.2f".format(amount)
                line.addView(textamount)
                //DATE
                val textDate = TextView(context)
                textDate.id = View.generateViewId()
                textDate.text = simpleDate.format(Date(transactions[i].timeStamp * 1000))
                line.addView(textDate)

                val set = ConstraintSet()
                set.clear(textaddress.id)
                set.clear(icon.id)
                set.clear(textamount.id)
                set.clear(textDate.id)
                //address with start
                set.connect(textaddress.id, ConstraintSet.START, line.id, ConstraintSet.START, 10)
                //date with end
                set.connect(textDate.id, ConstraintSet.END, line.id, ConstraintSet.END, 10)
                //address with icon
                set.connect(icon.id, ConstraintSet.START, textaddress.id, ConstraintSet.END,0)
                set.connect(textaddress.id, ConstraintSet.END, icon.id, ConstraintSet.START,0)
                //icon with amount
                set.connect(textamount.id, ConstraintSet.START, icon.id, ConstraintSet.END,0)
                set.connect(icon.id, ConstraintSet.END, textamount.id, ConstraintSet.START,0)
                //amount with date
                set.connect(textDate.id, ConstraintSet.START, textamount.id, ConstraintSet.END,0)
                set.connect(textamount.id, ConstraintSet.END, textDate.id, ConstraintSet.START,0)
                val chainViews = intArrayOf(textaddress.id, textDate.id)
                val chainWeights = floatArrayOf(0f,0f)
                set.createHorizontalChain(
                    ConstraintSet.PARENT_ID, ConstraintSet.RIGHT,
                    ConstraintSet.PARENT_ID, ConstraintSet.LEFT,
                    chainViews, chainWeights,
                    ConstraintSet.CHAIN_SPREAD)
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
        }//transactions

        Thread(Runnable {
            var prevtransactions : MutableList<Transaction> = ArrayList()
            while(true) {
                var transactions = LndRPCController.getTransactions()
                if (transactions.count() > prevtransactions.count()) {
                    prevtransactions = transactions

                    //for (i in 0 until transactions.count()) {

                    activity?.runOnUiThread {
                        //Set balance
                        walletBalance.text = "%.2f".format(LndRPCController.getTotalBalance())
                        /*
                        val simpleDate = SimpleDateFormat("dd/MM/yyyy")

                        if (transactions.count() > 0) {
                            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowFirstLayout)
                            layout.visibility = View.VISIBLE
                            firstaddress.text = transactions[0].destAddressesList[0]
                            if (transactions[0].amount < 0) {
                                val icon = v.findViewById<ImageView>(R.id.firstpaymenticon)
                                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                                DrawableCompat.setTint(
                                    icon.drawable,
                                    ContextCompat.getColor(
                                        requireContext(),
                                        android.R.color.holo_red_light
                                    )
                                )
                            }
                            firstamount.text =
                                getString(R.string.wallet_coin) + (transactions[0].amount / 1073741824).toString()

                            firstdate.text = simpleDate.format(Date(transactions[0].timeStamp))
                        }
                        if (transactions.count() > 1) {
                            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowSecondLayout)
                            layout.visibility = View.VISIBLE
                            secondaddress.text = transactions[1].destAddressesList[0]
                            secondamount.text =
                                getString(R.string.wallet_coin) + (transactions[1].amount / 1073741824).toString()
                            seconddate.text = simpleDate.format(Date(transactions[1].timeStamp))
                            if (transactions[1].amount < 0) {
                                val icon = v.findViewById<ImageView>(R.id.Secondpaymenticon)
                                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                                DrawableCompat.setTint(
                                    icon.drawable,
                                    ContextCompat.getColor(
                                        requireContext(),
                                        android.R.color.holo_red_light
                                    )
                                )
                            }
                        }
                        if (transactions.count() > 2) {
                            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowThirdLayout)
                            layout.visibility = View.VISIBLE
                            thirdaddress.text = transactions[2].destAddressesList[0]
                            thirdamount.text =
                                getString(R.string.wallet_coin) + (transactions[2].amount / 1073741824).toString()
                            thirdddate.text = simpleDate.format(Date(transactions[2].timeStamp))
                            if (transactions[2].amount < 0) {
                                val icon = v.findViewById<ImageView>(R.id.Thirdpaymenticon)
                                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                                DrawableCompat.setTint(
                                    icon.drawable,
                                    ContextCompat.getColor(
                                        requireContext(),
                                        android.R.color.holo_red_light
                                    )
                                )
                            }
                        }
                        if (transactions.count() > 3) {
                            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowFourthLayout)
                            layout.visibility = View.VISIBLE
                            fourthaddress.text = transactions[3].destAddressesList[0]
                            fourthamount.text =
                                getString(R.string.wallet_coin) + (transactions[3].amount / 1073741824).toString()
                            fourthdate.text = simpleDate.format(Date(transactions[3].timeStamp))
                            if (transactions[3].amount < 0) {
                                val icon = v.findViewById<ImageView>(R.id.Fourthpaymenticon)
                                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                                DrawableCompat.setTint(
                                    icon.drawable,
                                    ContextCompat.getColor(
                                        requireContext(),
                                        android.R.color.holo_red_light
                                    )
                                )
                            }
                        }*/
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
                putExtra(Intent.EXTRA_TEXT, "This is my PKT wallet address: $address")
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

