package co.anode.anodium

import android.annotation.SuppressLint
import android.content.DialogInterface
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
    private var lineID = 0
    private var fromHistory = false

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_transactiondetails, container, false)
        var destAddress = ""
        if (!requireArguments().getString("address").isNullOrEmpty()) {
            destAddress = requireArguments().getString("address").toString()
        }
        fromHistory = requireArguments().getBoolean("history")
        val address = v.findViewById<TextView>(R.id.trdetails_addr)
        val paidToLabel = v.findViewById<TextView>(R.id.trdetails_addr_label)
        if (destAddress.isEmpty()) {
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
        lineID = requireArguments().getInt("lineID")
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

    override fun onDismiss(dialog: DialogInterface) {
        if (fromHistory) {
            (activity as TransactionHistoryActivity).clearLines(lineID)
        } else {
            (activity as WalletActivity).transactionDetailsClosed(lineID)
        }
        super.onDismiss(dialog)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            TransactionDetailsFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}

