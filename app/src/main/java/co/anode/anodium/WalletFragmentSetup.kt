package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment


class WalletFragmentSetup : Fragment() {
    lateinit var statusbar: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.walletfragment_setup, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        AnodeClient.eventLog(requireContext(), "Activity: WalletFragmentCreate created")
        val prefs = context?.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        var seed = ""
        val closeButton = view.findViewById<Button>(R.id.button_wallet_close)
        closeButton.visibility = View.GONE
        val textview = view.findViewById<TextView>(R.id.text_walletcreate_seed_instructions)
        textview.visibility = View.GONE
        val seedLayout = view.findViewById<LinearLayout>(R.id.seed_layout)
        seedLayout.visibility = View.GONE
        val createButton = view.findViewById<Button>(R.id.button_wallet_create)
        statusbar = view.findViewById<TextView>(R.id.textview_status)
        createButton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Create PKT wallet clicked")
            Log.i(LOGTAG, "WalletFragmentSetup creating wallet")
            var password = ""
            val builder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("PKT Wallet password")
            builder.setMessage("Please set your password")
            val input = EditText(context)
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT)
            input.layoutParams = lp
            input.inputType = InputType.TYPE_TEXT_VARIATION_PASSWORD
            input.transformationMethod = PasswordTransformationMethod.getInstance()
            builder.setView(input)
            builder.setPositiveButton("Submit", DialogInterface.OnClickListener { dialog, _ ->
                password = input.text.toString()
                dialog.dismiss()

                if ((isAdded) && (prefs != null) && (password.isNotEmpty())) {
                    Thread({
                        activity?.runOnUiThread {
                            statusbar.text = "Creating wallet please wait..."
                            val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                            activity?.getColor(android.R.color.darker_gray)?.let { layout.setBackgroundColor(it) }
                        }
                        val result = LndRPCController.createLocalWallet(prefs, password)
                        if (result.contains("Success")) {
                            activity?.runOnUiThread {
                                Toast.makeText(requireContext(), "PKT wallet created", Toast.LENGTH_LONG).show()
                                statusbar.text = ""
                                val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                                activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
                                Log.i(LOGTAG, "WalletFragmentSetup retrieved seed phrase")
                                seed = result.substring("Success".length)
                                //Change label
                                val label = view.findViewById<TextView>(R.id.text_walletcreate_label)
                                label.text = context?.resources?.getString(R.string.wallet_create_seed_label)
                                textview.visibility = View.VISIBLE
                                val seedArray = seed.split(" ")
                                val seedCol1 = view.findViewById<TextView>(R.id.seed_column1)
                                val seedCol2 = view.findViewById<TextView>(R.id.seed_column2)
                                var seed1 = ""
                                var seed2 = ""
                                for (i in 0 until 12) {
                                    seed1 += (i + 1).toString() + ". " + seedArray[i] + "\n"
                                }
                                for (i in 12 until seedArray.size - 1) {
                                    seed2 += (i + 1).toString() + ". " + seedArray[i] + "\n"
                                }
                                seedCol1.text = seed1
                                seedCol2.text = seed2
                                seedLayout.visibility = View.VISIBLE
                                createButton.visibility = View.GONE
                                closeButton.visibility = View.VISIBLE
                            }
                        } else {
                            Log.i(LOGTAG, "WalletFragmentSetup Error from trying to create wallet")
                            activity?.runOnUiThread {
                                Toast.makeText(requireContext(), "Error: $result", Toast.LENGTH_LONG).show()
                            }
                        }
                    }, "WalletFragmentSetup.CreateWallet").start()
                }
            })

            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            val alert: androidx.appcompat.app.AlertDialog = builder.create()
            alert.show()
        }

        closeButton.setOnClickListener {
            activity?.finish()
        }

    }
}