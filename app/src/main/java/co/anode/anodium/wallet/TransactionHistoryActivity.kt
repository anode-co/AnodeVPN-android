package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.R
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class TransactionHistoryActivity : AppCompatActivity() {
    lateinit var apiController: APIController
    private var transactionsLastTimeUpdated: Long = 0
    lateinit var h: Handler
    private var prevTransactions = 0
    private var skipTransactions = 0
    private var updateConfirmations = arrayListOf<Boolean>()

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
        h = Handler(Looper.getMainLooper())
        val extras = intent.extras
        if (extras != null) {
            skipTransactions = extras.getInt("skip")
        }
        h.postDelayed(refreshValues,0)
    }

    private val refreshValues = Runnable {
        if ((System.currentTimeMillis()-5000) > transactionsLastTimeUpdated) {
            getWalletTransactions(skipTransactions)
        }
    }

    fun clearLines(lineID: Int) {
        val l = findViewById<ConstraintLayout>(lineID)
        l?.setBackgroundColor(Color.WHITE)
    }

    @SuppressLint("SetTextI18n")
    private fun getWalletTransactions(skip: Int) {
        val listsLayout = findViewById<LinearLayout>(R.id.paymentsList)
        val simpleDate = SimpleDateFormat("yyyy-MM-dd HH:mm")
        val params = JSONObject()
        //Exclude mining transactions
        params.put("coinbase", 1)
        params.put("txnsSkip", skip)
        params.put("txnsLimit", 51)
        params.put("reversed", true)
        val textSize = 15.0f
        apiController.post(apiController.getTransactionsURL, params) { response ->
            if ((response != null) &&
                !response.has("error") &&
                response.has("transactions") &&
                !response.isNull("transactions")) {
                transactionsLastTimeUpdated = System.currentTimeMillis()
                val transactions = response.getJSONArray("transactions")
                if (transactions.length() == 0) return@post
                if ((transactions.length() > prevTransactions) || updateConfirmations.contains(true)){
                    updateConfirmations.clear()
                    listsLayout.removeAllViews()
                    //When we get one new transaction
                    //and is a receiving one, push notification
                    val lastAmount = transactions.getJSONObject(0).getString("amount").toLong()
                    if (( prevTransactions+1 == transactions.length()) &&
                        (lastAmount > 0)) {
                        AnodeUtil.pushNotification("Got paid!", AnodeUtil.satoshisToPKT(lastAmount))
                    }
                    var txnsSize = transactions.length()
                    if (txnsSize > 50) {
                        txnsSize = 50
                        findViewById<TextView>(R.id.button_next_page).visibility = View.VISIBLE
                    } else {
                        findViewById<TextView>(R.id.button_next_page).visibility = View.GONE
                    }
                    for (i in 0 until txnsSize) {
                        val transaction = transactions.getJSONObject(i)
                        //Add new line
                        val line = ConstraintLayout(this)
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
                            bundle.putInt("lineID", i)
                            bundle.putBoolean("history", true)
                            transactionDetailsFragment.arguments = bundle
                            transactionDetailsFragment.show(supportFragmentManager, "")
                            line.setBackgroundColor(Color.GRAY)
                        }
                        line.setBackgroundColor(Color.WHITE)
                        line.id = i
                        line.tag = "TxLine$i"
                        //line.orientation = LinearLayout.HORIZONTAL
                        val llParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
                        line.layoutParams = llParams
                        line.setPadding(20, 20, 10, 20)
                        //ADDRESS
                        val textAddress = TextView(this)
                        textAddress.id = View.generateViewId()
                        textAddress.textSize = textSize
                        //AMOUNT
                        val textAmount = TextView(this)
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
                        val textDate = TextView(this)
                        textDate.id = View.generateViewId()
                        textDate.text = simpleDate.format(Date(transaction.getString("timeStamp").toLong() * 1000))
                        textDate.textSize = textSize
                        //Add columns
                        //confirmations indicator
                        val icon = ImageView(this)
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
                    //If more than 50 transactions show link to next page
                    if (transactions.length() > 25) {
                        findViewById<TextView>(R.id.texthistory).visibility = View.VISIBLE
                    }
                    prevTransactions = transactions.length()
                }
            } else if ((response != null) &&
                response.has("message") &&
                !response.isNull("message")) {
                Log.d(LOGTAG, response.getString("message"))
            }
        }
    }
    /*
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
    }*/

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}