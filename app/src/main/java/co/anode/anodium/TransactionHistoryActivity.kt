package co.anode.anodium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.github.lightningnetwork.lnd.lnrpc.Transaction
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
        Thread(Runnable {
            var transactions = LndRPCController.getTransactions()
            var prevtransactions : MutableList<Transaction> = ArrayList()
            while(true) {
                var transactions = LndRPCController.getTransactions()
                if (transactions.count() > prevtransactions.count()) {
                    prevtransactions = transactions
                    runOnUiThread {
                        for (i in 0 until transactions.count()) {
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
                            line.setPadding(10, 20, 10, 20)

                            //ADDRESS
                            val textaddress = TextView(context)
                            textaddress.id = View.generateViewId()
                            textaddress.width = 550
                            textaddress.text =
                                transactions[i].destAddressesList[0].substring(4, 20) + "..."
                            line.addView(textaddress)
                            //In/Out Icon
                            val icon = ImageView(context)
                            icon.id = View.generateViewId()
                            val amount: Float = transactions[i].amount.toFloat() / 1073741824
                            if (amount < 0) {
                                icon.setBackgroundResource(R.drawable.ic_baseline_arrow_upward_24)
                            } else {
                                icon.setBackgroundResource(R.drawable.ic_baseline_arrow_downward_24)
                            }
                            line.addView(icon)
                            //AMOUNT
                            val textamount = TextView(context)
                            textamount.id = View.generateViewId()
                            textamount.width = 300
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