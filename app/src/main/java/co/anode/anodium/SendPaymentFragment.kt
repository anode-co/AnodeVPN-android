package co.anode.anodium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.protobuf.ByteString

class SendPaymentFragment: BottomSheetDialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_sendpayment, container, false)

        val sendbutton = v.findViewById<Button>(R.id.button_sendPKTPayment)
        sendbutton.setOnClickListener {
            var sendcoins = true
            val bspassword: ByteString = ByteString.copyFrom("password", Charsets.UTF_8)
            //Check fields
            val address = v.findViewById<EditText>(R.id.editTextReceiverAddress)
            if (address.text.toString().isNullOrEmpty()) {
                Toast.makeText(context, "Please fill in the receiver's address", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            val amount = v.findViewById<EditText>(R.id.editTextPKTAmount)
            if (amount.text.toString().isNullOrEmpty()) {
                Toast.makeText(context, "Please fill in the PKT amount", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            val password = v.findViewById<EditText>(R.id.editTextPKTPassword)
            if (password.text.toString().isNullOrEmpty()) {
                Toast.makeText(context, "Please fill in the password", Toast.LENGTH_SHORT).show()
                sendcoins = false
            } else if (ByteString.copyFrom(password.text.toString(),Charsets.UTF_8) != bspassword ) {
                Toast.makeText(context, "Wrong password", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            //Send coins
            if (sendcoins) {
                LndRPCController.sendCoins(address.text.toString(), amount.text.toString().toLong())
                Toast.makeText(context, "Payment of ${amount.text}PKT send", Toast.LENGTH_SHORT).show()
                dismiss()
            }
        }
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
                SendPaymentFragment().apply {
                    arguments = Bundle().apply {
                        //putString(ARG_PARAM1, param1)
                        //putString(ARG_PARAM2, param2)
                    }
                }
    }
}