package co.anode.anodium

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import lnrpc.Rpc.Transaction
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.action_older_transactions)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        val listsLayout = findViewById<LinearLayout>(R.id.paymentsList)
        val context = applicationContext
        var prevtransactions : MutableList<Transaction> = ArrayList()
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        var myaddress = prefs.getString("lndwalletaddress", "")
        Thread(Runnable {
            var prevtransactions : JSONArray = JSONArray()
            while(true) {
                var transactions = LndRPCController.getTransactions(context)
                if (transactions.length() > prevtransactions.length()) {
                    prevtransactions = transactions
                    runOnUiThread {
                        for (i in 0 until transactions.length()) {
                            val transaction = transactions.getJSONObject(i)
                            //Add new line
                            var line = ConstraintLayout(context)
                            line.setOnClickListener {
                                val transactiondetailsFragment: BottomSheetDialogFragment = TransactionDetailsFragment()
                                val bundle = Bundle()
                                bundle.putString("txid", transaction.getString("tx_hash"))
                                for (a in 0 until transaction.getJSONArray("dest_addresses").length()) {
                                    bundle.putString("address$a",transaction.getJSONArray("dest_addresses").getString(a))
                                }
                                bundle.putLong("amount", transaction.getString("amount").toLong())
                                bundle.putInt("blockheight", transaction.getInt("block_height"))
                                bundle.putString("blockhash", transaction.getString("block_hash"))

                                transactiondetailsFragment.arguments = bundle
                                transactiondetailsFragment.show(supportFragmentManager, "")
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
                            textaddress.width = 350
                            val destAddress = transaction.getJSONArray("dest_addresses").getString(0)
                            textaddress.text = destAddress.substring(4, 20) + "..."
                            line.addView(textaddress)
                            //In/Out Icon
                            val icon = ImageView(context)
                            icon.id = View.generateViewId()
                            var amount: Float = transaction.getString("amount").toFloat() / 1073741824
                            if (amount < 0) {
                                icon.setBackgroundResource(R.drawable.ic_baseline_arrow_upward_24)
                                textaddress.text = destAddress.substring(0, 6) + "..." + destAddress.substring(destAddress.length-8)
                            } else {
                                icon.setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
                                if (destAddress == myaddress) {
                                    val destAddress2 = transaction.getJSONArray("dest_addresses").getString(1)
                                    textaddress.text = destAddress2.substring(0, 6) + "..." + destAddress2.substring(destAddress2.length-8)
                                }
                            }
                            line.addView(icon)
                            //AMOUNT
                            val textamount = TextView(context)
                            textamount.id = View.generateViewId()
                            textamount.width = 350
                            val onePKT = 1073741824
                            val mPKT = 1073741.824F
                            val uPKT  = 1073.741824F
                            val nPKT  = 1.073741824F
                            if (amount > 1000000000) {
                                amount /= onePKT
                                textamount.text = "PKT %.2f".format(amount)
                            } else if (amount > 1000000) {
                                amount /= mPKT
                                textamount.text = "mPKT %.2f".format(amount)
                            } else if (amount > 1000) {
                                amount /= uPKT
                                textamount.text = "μPKT %.2f".format(amount)
                            } else if (amount < 1000000000) {
                                amount /= onePKT
                                textamount.text = "PKT %.2f".format(amount)
                            } else if (amount < 1000000) {
                                amount /= mPKT
                                textamount.text = "mPKT %.2f".format(amount)
                            } else if (amount < 1000) {
                                amount /= uPKT
                                textamount.text = "μPKT %.2f".format(amount)
                            } else {
                                amount /= nPKT
                                textamount.text = "nPKT %.2f".format(amount)
                            }
                            line.addView(textamount)
                            //DATE
                            val textDate = TextView(context)
                            textDate.id = View.generateViewId()
                            textDate.text =
                                simpleDate.format(Date(transaction.getString("time_stamp").toLong() * 1000))
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
                    }
                }
                Thread.sleep(10000)
            }
        }, "SendPaymentActivity.RefreshValues").start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}