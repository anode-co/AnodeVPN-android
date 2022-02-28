package co.anode.anodium

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TransactionDetailsFragment : BottomSheetDialogFragment(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_transactiondetails, container, false)
//        val prefs = requireContext().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
//        var myaddress = prefs.getString("lndwalletaddress", "")
        var destAddress = ""
        if (!requireArguments().getString("address").isNullOrEmpty()) {
            destAddress = requireArguments().getString("address").toString()
        }
        val address = v.findViewById<TextView>(R.id.trdetails_addr)
        val paidToLabel = v.findViewById<TextView>(R.id.trdetails_addr_label)
        if (destAddress.isNullOrEmpty()) {
            paidToLabel.visibility = View.INVISIBLE
        } else {
            paidToLabel.visibility = View.VISIBLE
            address.text = destAddress
        }

        val textAmount = v.findViewById<TextView>(R.id.trdetails_amount)
        textAmount.text = requireArguments().getString("amount")

        val block = v.findViewById<TextView>(R.id.trdetails_block)
        val bheight = requireArguments().getInt("blockheight")
        val bhash = requireArguments().getString("blockhash")
        val confirmations = v.findViewById<TextView>(R.id.trdetails_confirmations)
        if (bheight>0) {
            val blockLink: Spanned = HtmlCompat.fromHtml("<a href='https://explorer.pkt.cash/block/$bhash'>$bheight</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
            block.movementMethod = LinkMovementMethod.getInstance()
            block.text = blockLink
            confirmations.visibility = View.VISIBLE
            confirmations.text = "Confirmations: "+requireArguments().getInt("confirmations").toString()
        } else {
            block.text = "Unconfirmed"
            confirmations.visibility = View.INVISIBLE
        }
        val txidlink = v.findViewById<TextView>(R.id.trdetails_txid)
        val txid = requireArguments().getString("txid")
        val transactionlink: Spanned = HtmlCompat.fromHtml("<a href='https://explorer.pkt.cash/tx/$txid'>${txid!!.substring(0,12)}...</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        txidlink.movementMethod = LinkMovementMethod.getInstance()
        txidlink.text = transactionlink
        return v
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RatingFragment.
         */
        @JvmStatic
        //fun newInstance(param1: String, param2: String) =
        fun newInstance() =
            TransactionDetailsFragment().apply {
                arguments = Bundle().apply {
                    //putString(ARG_PARAM1, param1)
                    //putString(ARG_PARAM2, param2)
                }
            }
    }
}

