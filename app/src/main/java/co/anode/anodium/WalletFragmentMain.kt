package co.anode.anodium

import android.content.Context
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import java.text.SimpleDateFormat
import java.util.*

class WalletFragmentMain : Fragment() {
    lateinit var walletBalance:TextView

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val prefs = requireContext().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        LndRPCController.openWallet(prefs)
        while (!LndRPCController.isOpen) {
            //TODO: add timeout
        }
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.walletfragment_main, container, false)
    }

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        super.onViewCreated(v, savedInstanceState)

        //Open Wallet
        //openWallet().execute(prefs, v)
        //Retrieve Balance
        //val balance = LndRPCController.getTotalBalance()
        //Retrieve payments

        //Set balance
        walletBalance = v.findViewById(R.id.walletBalanceNumber)
        walletBalance.text = LndRPCController.getTotalBalance().toString()

        val transactions = LndRPCController.getTransactions()
        for (i in 0 until transactions.count()) {
            transactions[i].amount
            transactions[i].destAddressesList[0]
            transactions[i].timeStamp
        }
        val walletAddress = v.findViewById<TextView>(R.id.walletAddress)

        val firstaddress = v.findViewById<TextView>(R.id.firstaddress)
        firstaddress.text = transactions[0].destAddressesList[0]
        val firstamount = v.findViewById<TextView>(R.id.firstpaymentamount)
        firstamount.text = getString(R.string.wallet_coin) + (transactions[0].amount/1073741824).toString()
        val firstdate = v.findViewById<TextView>(R.id.firstpaymentdate)
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")
        firstdate.text = simpleDate.format(Date(transactions[0].timeStamp))

        val thirdaddress = v.findViewById<TextView>(R.id.Thirdaddress)
        val fourthaddress = v.findViewById<TextView>(R.id.Fourthaddress)


        val secondaddress = v.findViewById<TextView>(R.id.Secondaddress)
        val secondamount = v.findViewById<TextView>(R.id.Secondpaymentamount)
        val thirdamount = v.findViewById<TextView>(R.id.Thirdpaymentamount)
        val fourthamount = v.findViewById<TextView>(R.id.Fourthpaymentamount)

    }

    class openWallet(): AsyncTask<Any?, Any?, String>() {
        override fun doInBackground(vararg params: Any?): String? {
            val prefs = params[0] as SharedPreferences
            val v = params[1] as View

            val balance = v.findViewById<TextView>(R.id.walletBalanceNumber)
            balance.text = LndRPCController.getTotalBalance().toString()
            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
        }
    }
}

