package co.anode.anodium

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
        //Set balance
        val walletBalance = v.findViewById<TextView>(R.id.walletBalanceNumber)
        walletBalance.text = LndRPCController.getTotalBalance().toString()

        val transactions = LndRPCController.getTransactions()
        for (i in 0 until transactions.count()) {
            transactions[i].amount
            transactions[i].destAddressesList[0]
            transactions[i].timeStamp
        }

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
        val simpleDate = SimpleDateFormat("dd/MM/yyyy")

        if (transactions.count() > 0) {
            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowFirstLayout)
            layout.visibility = View.VISIBLE
            val firstaddress = v.findViewById<TextView>(R.id.firstaddress)
            firstaddress.text = transactions[0].destAddressesList[0]
            val firstamount = v.findViewById<TextView>(R.id.firstpaymentamount)
            if (transactions[0].amount < 0) {
                val icon = v.findViewById<ImageView>(R.id.firstpaymenticon)
                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                DrawableCompat.setTint(
                    icon.drawable,
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                )
            }
            firstamount.text =
                getString(R.string.wallet_coin) + (transactions[0].amount / 1073741824).toString()
            val firstdate = v.findViewById<TextView>(R.id.firstpaymentdate)
            firstdate.text = simpleDate.format(Date(transactions[0].timeStamp))
        }
        if (transactions.count() > 1) {
            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowSecondLayout)
            layout.visibility = View.VISIBLE
            val secondaddress = v.findViewById<TextView>(R.id.Secondaddress)
            secondaddress.text = transactions[1].destAddressesList[0]
            val secondamount = v.findViewById<TextView>(R.id.Secondpaymentamount)
            secondamount.text =
                getString(R.string.wallet_coin) + (transactions[1].amount / 1073741824).toString()
            val seconddate = v.findViewById<TextView>(R.id.Secondpaymentdate)
            seconddate.text = simpleDate.format(Date(transactions[1].timeStamp))
            if (transactions[1].amount < 0) {
                val icon = v.findViewById<ImageView>(R.id.Secondpaymenticon)
                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                DrawableCompat.setTint(
                    icon.drawable,
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                )
            }
        }
        if (transactions.count() > 2) {
            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowThirdLayout)
            layout.visibility = View.VISIBLE
            val thirdaddress = v.findViewById<TextView>(R.id.Thirdaddress)
            thirdaddress.text = transactions[2].destAddressesList[0]
            val thirdamount = v.findViewById<TextView>(R.id.Thirdpaymentamount)
            thirdamount.text =
                getString(R.string.wallet_coin) + (transactions[2].amount / 1073741824).toString()
            val thirdddate = v.findViewById<TextView>(R.id.Thirdpaymentdate)
            thirdddate.text = simpleDate.format(Date(transactions[2].timeStamp))
            if (transactions[2].amount < 0) {
                val icon = v.findViewById<ImageView>(R.id.Thirdpaymenticon)
                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                DrawableCompat.setTint(
                    icon.drawable,
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                )
            }
        }
        if (transactions.count() > 3) {
            val layout = v.findViewById<LinearLayout>(R.id.PaymentRowFourthLayout)
            layout.visibility = View.VISIBLE
            val fourthaddress = v.findViewById<TextView>(R.id.Fourthaddress)
            fourthaddress.text = transactions[3].destAddressesList[0]
            val fourthamount = v.findViewById<TextView>(R.id.Fourthpaymentamount)
            fourthamount.text =
                getString(R.string.wallet_coin) + (transactions[3].amount / 1073741824).toString()
            val fourthdate = v.findViewById<TextView>(R.id.Fourthpaymentdate)
            fourthdate.text = simpleDate.format(Date(transactions[3].timeStamp))
            if (transactions[3].amount < 0) {
                val icon = v.findViewById<ImageView>(R.id.Fourthpaymenticon)
                icon.setImageResource(R.drawable.ic_baseline_arrow_upward_24)
                DrawableCompat.setTint(
                    icon.drawable,
                    ContextCompat.getColor(requireContext(), android.R.color.holo_red_light)
                )
            }
        }

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
            val sendpaymentFragment: BottomSheetDialogFragment = SendPaymentFragment()
            sendpaymentFragment.show(childFragmentManager, "")
        }
    }
}

