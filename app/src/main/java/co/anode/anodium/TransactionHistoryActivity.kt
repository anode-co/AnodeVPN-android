package co.anode.anodium

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import lnrpc.Rpc.Transaction
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {
    lateinit var apiController: APIController
    private val getTransactionsURL = "http://127.0.0.1:8080/api/v1/lightning/gettransactions"
    private var transactionsLastTimeUpdated: Long = 0
    private val refreshValuesInterval: Long = 10000
    lateinit var h: Handler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction_history)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.action_older_transactions)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        //Initialize handlers
        val service = ServiceVolley()
        apiController = APIController(service)

        val listsLayout = findViewById<LinearLayout>(R.id.paymentsList)
        h.postDelayed(refreshValues,refreshValuesInterval)
    }

    private val refreshValues = object : Runnable {
        override fun run() {
            if ((System.currentTimeMillis()-5000) > transactionsLastTimeUpdated) {
                getWalletTransactions()
            }
        }
    }

    private fun getWalletTransactions() {
        val listsLayout = findViewById<LinearLayout>(R.id.paymentsList)
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
        val context = applicationContext
        var prevTransactions = 0
        var transactions = JSONArray()
        val params = JSONObject()
        //Exclude mining transactions
        params.put("coinbase", 1)

        apiController.post(getTransactionsURL, params) { response ->
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

                    runOnUiThread {
                        listsLayout.removeAllViews()
                        for (i in 0 until tcount) {
                            val transaction = transactions.getJSONObject(i)

                            //Add new line
                            val line = ConstraintLayout(context)
                            line.setOnClickListener {
                                val transactiondetailsFragment: BottomSheetDialogFragment = TransactionDetailsFragment()
                                val bundle = Bundle()
                                bundle.putString("txid", transaction.getString("txHash"))
                                bundle.putLong("amount", transaction.getString("amount").toLong())
                                bundle.putInt("blockheight", transaction.getInt("blockHeight"))
                                bundle.putString("blockhash", transaction.getString("blockHash"))
                                //Do not include confirmations
                                //bundle.putInt("confirmations", transactions[i].numConfirmations)
                                transactiondetailsFragment.arguments = bundle
                                transactiondetailsFragment.show(this.supportFragmentManager, "")
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
                            }
                            line.addView(textAddress)
                            line.addView(icon)
                            //AMOUNT
                            val textAmount = TextView(context)
                            textAmount.id = View.generateViewId()
                            textAmount.width = 350
                            textAmount.text = AnodeUtil.satoshisToPKT(amount)
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
                    }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}