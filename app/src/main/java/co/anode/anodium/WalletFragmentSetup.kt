package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import org.json.JSONObject


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

                if ((isAdded) && (password.isNotEmpty())) {
                    Thread({
                        activity?.runOnUiThread {
                            statusbar.text = "Creating wallet please wait..."
                            val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                            activity?.getColor(android.R.color.darker_gray)?.let { layout.setBackgroundColor(it) }
                        }
                        //B64encode password
                        val b64Password = Base64.encodeToString(password.toByteArray(), Base64.DEFAULT)
                        //Initialize Volley Service
                        val service = ServiceVolley()
                        val apiController = APIController(service)
                        val jsonData = JSONObject()
                        jsonData.put("wallet_password", b64Password)
                        Log.i(LOGTAG, "creating PKT wallet...")
                        apiController.post("http://localhost:8080/pkt/v1/createwallet", jsonData)
                        //Handle response from createwallet REST request
                        { response ->
                            if (response!!.has("seed")) {
                                //Wallet created succesfully
                                Log.i(LOGTAG, "PKT create wallet success")
                                //Get seed
                                val seedText = view.findViewById<TextView>(R.id.seed_column)
                                val seedArray = response.getJSONArray("seed")
                                var seedString = ""
                                for (i in 0 until seedArray.length() - 1) {
                                    seedString += seedArray.getString(i) + " "
                                }
                                seedText.text = seedString
                                //Store password to encrypted shared preferences
                                storePassword(password)
                            } else {
                                Log.i(LOGTAG, "PKT create wallet failed")
                                //Wallet creation failed, parse error, log and notify user
                                var errorString = response.getString("error")
                                Log.e(LOGTAG, errorString)
                                //Get user friendly message
                                errorString = errorString.substring(errorString.indexOf(" "), errorString.indexOf("\n\n"))
                                activity?.runOnUiThread {
                                    Toast.makeText(requireContext(), "Error: $errorString", Toast.LENGTH_LONG).show()
                                }
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

    private fun storePassword(password: String) {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        val masterKey = MasterKey.Builder(requireContext())
            .setKeyGenParameterSpec(spec)
            .build()

        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                requireContext(),
                "co.anode.anodium-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        encSharedPreferences.edit().putString("wallet_password", password).apply()
    }
}