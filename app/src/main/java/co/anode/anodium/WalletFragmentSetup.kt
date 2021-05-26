package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment


class WalletFragmentSetup : Fragment() {

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
        AnodeClient.eventLog(requireContext(),"Activity: WalletFragmentCreate created")
        val prefs = context?.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        var seed = ""
        val closebutton = view.findViewById<Button>(R.id.button_wallet_close)
        closebutton.visibility = View.GONE
        val textview = view.findViewById<TextView>(R.id.text_walletcreate_seed_instructions)
        textview.visibility = View.GONE
        val seedlayout = view.findViewById<LinearLayout>(R.id.seed_layout)
        seedlayout.visibility = View.GONE
        val createbutton = view.findViewById<Button>(R.id.button_wallet_create)
        createbutton.setOnClickListener {
            AnodeClient.eventLog(requireContext(),"Button: Create PKT wallet clicked")
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
            builder.setView(input)
            builder.setPositiveButton("Submit", DialogInterface.OnClickListener { dialog, which ->
                password = input.text.toString()
                dialog.dismiss()

                if ((prefs != null) && (!password.isNullOrEmpty())) {
                    val result = LndRPCController.createLocalWallet(prefs, password)
                    if (result.contains("Success")) {
                        Toast.makeText(requireContext(), "PKT wallet created", Toast.LENGTH_LONG).show()
                        Log.i(LOGTAG, "WalletFragmentSetup retrieved seed phrase")
                        seed = result.substring("Success".length)
                        //Change label
                        val label = view.findViewById<TextView>(R.id.text_walletcreate_label)
                        label.text = context?.resources?.getString(R.string.wallet_create_seed_label)
                        textview.visibility = View.VISIBLE
                        val seedarray = seed.split(" ")
                        val seedcol1 = view.findViewById<TextView>(R.id.seed_column1)
                        val seedcol2 = view.findViewById<TextView>(R.id.seed_column2)
                        var seed1 = ""
                        var seed2 = ""
                        for (i in 0 until 12) {
                            seed1 += (i+1).toString()+". " + seedarray[i]+"\n"
                        }
                        for (i in 12 until seedarray.size-1) {
                            seed2 += (i+1).toString()+". " + seedarray[i]+"\n"
                        }
                        seedcol1.text = seed1
                        seedcol2.text = seed2
                        seedlayout.visibility = View.VISIBLE
                        createbutton.visibility = View.GONE
                        closebutton.visibility = View.VISIBLE
                    } else {
                        Log.i(LOGTAG, "WalletFragmentSetup Error from trying to create wallet")
                        Toast.makeText(requireContext(), "Error: $result", Toast.LENGTH_LONG).show()
                    }
                }
            })

            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
                dialog.dismiss()
            })
            val alert: androidx.appcompat.app.AlertDialog = builder.create()
            alert.show()

        }

        closebutton.setOnClickListener {
            //Open wallet before closing
            val prefs = requireContext().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            var result = LndRPCController.openWallet(prefs)
            if (result == "OK"){
                with(prefs.edit()) {
                    putBoolean("lndwalletopened", true)
                    commit()
                }
            }
            activity?.finish()
        }

    }
}