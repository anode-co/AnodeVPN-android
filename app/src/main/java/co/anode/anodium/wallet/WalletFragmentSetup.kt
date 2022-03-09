package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.R
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
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
        AnodeClient.eventLog("Activity: WalletFragmentCreate created")

        val closeButton = view.findViewById<Button>(R.id.button_wallet_close)
        closeButton.visibility = View.GONE
        val instructionsTextview = view.findViewById<TextView>(R.id.text_walletcreate_seed_instructions)
        instructionsTextview.visibility = View.GONE
        //Hide seed layout
        val seedLayout = view.findViewById<LinearLayout>(R.id.create_wallet_show_seed)
        seedLayout.visibility = View.GONE
        //Show passwords
        val passwordsLayout = view.findViewById<LinearLayout>(R.id.create_wallet_password_layout)
        passwordsLayout.visibility = View.VISIBLE
        val createButton = view.findViewById<Button>(R.id.button_wallet_create)
        val recoverButton = view.findViewById<Button>(R.id.button_wallet_recover_from_seed)

        statusbar = view.findViewById<TextView>(R.id.textview_status)
        val passwordText = view.findViewById<TextView>(R.id.editTextWalletPassword)
        val confirmPasswordText = view.findViewById<TextView>(R.id.editTextconfirmWalletPassword)
        val newPassLayout = view.findViewById<TextInputLayout>(R.id.newwalletpasswordLayout)
        val confirmPassLayout = view.findViewById<TextInputLayout>(R.id.confirmwalletpasswordLayout)
        val recoverWalletLayout = view.findViewById<LinearLayout>(R.id.recover_seed_layout)
        val createWalletLayout = view.findViewById<LinearLayout>(R.id.create_wallet_password_layout)
        recoverWalletLayout.visibility = View.GONE
        //Initialize Volley Service
        val service = ServiceVolley()
        val apiController = APIController(service)

        createButton.setOnClickListener {
            AnodeClient.eventLog("Button: Create PKT wallet clicked")
            Log.i(LOGTAG, "WalletFragmentSetup creating wallet")
            //Remove old error message
            newPassLayout.error = null
            confirmPassLayout.error = null
            //Check that passwords are valid (at least 8 characters and same)
            var password = passwordText.text.toString()
            val confirmPassword = confirmPasswordText.text.toString()
            if (password.length < 8) {
                newPassLayout.error = "Password must be at least 8 characters."
                password = ""
            } else if (password != confirmPassword) {
                confirmPassLayout.error = "Passwords do not match. Please try again."
                password = ""
            }

            if ((isAdded) && (password.isNotEmpty())) {
                statusbar.text = "Creating wallet please wait..."
                val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                activity?.getColor(android.R.color.darker_gray)?.let { layout.setBackgroundColor(it) }
                createButton.isEnabled = false

                //B64encode password
                var b64Password = Base64.encodeToString(password.toByteArray(), Base64.DEFAULT)
                b64Password = b64Password.replace("\n", "")

                val jsonData = JSONObject()
                jsonData.put("wallet_password", b64Password)
                Log.i(LOGTAG, "creating PKT wallet...")
                apiController.post("http://localhost:8080/api/v1/wallet/create", jsonData)
                //Handle response from createwallet REST request
                { response ->
                    if ((response != null) && (response.has("seed") && !response.isNull("seed"))) {
                        //Wallet created successfully
                        Log.i(LOGTAG, "PKT create wallet success")
                        instructionsTextview.visibility = View.VISIBLE
                        //Get seed
                        val seedText = view.findViewById<TextView>(R.id.seed_column)
                        val seedArray = response.getJSONArray("seed")
                        var seedString = ""
                        for (i in 0 until seedArray.length() - 1) {
                            seedString += seedArray.getString(i) + " "
                        }
                        seedText.text = seedString
                        //Set background color to white
                        activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
                        //Hide soft keyboard
                        val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        keyboard.hideSoftInputFromWindow(view.windowToken, 0)
                        //Show Seed and hide passwords
                        passwordsLayout.visibility = View.GONE
                        seedLayout.visibility = View.VISIBLE
                        //Hide create button and show Close one
                        createButton.visibility = View.GONE
                        recoverButton.visibility = View.GONE
                        closeButton.visibility = View.VISIBLE
                        //Store password to encrypted shared preferences
                        AnodeUtil.storePassword(password)
                        statusbar.text = ""
                        //Update header text
                        val label = view.findViewById<TextView>(R.id.text_walletcreate_label)
                        label.text = context?.resources?.getString(R.string.wallet_create_seed_label)
                    } else {
                        Log.i(LOGTAG, "PKT create wallet failed")
                        //Wallet creation failed, parse error, log and notify user
                        var errorString = response?.getString("error")
                        if (errorString != null) {
                            Log.e(LOGTAG, errorString)
                            //Get user friendly message
                            errorString = errorString.substring(errorString.indexOf(" "), errorString.indexOf("\n\n"))
                            activity?.runOnUiThread {
                                Toast.makeText(requireContext(), "Error: $errorString", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            activity?.runOnUiThread {
                                Toast.makeText(requireContext(), "Failed to create wallet with unknown error.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }

        closeButton.setOnClickListener {
            val walletActivity = activity as WalletActivity
            walletActivity.switchToMain()
        }

        recoverButton.setOnClickListener {
            if (recoverButton.text == getString(R.string.button_wallet_recover_from_seed)) {
                //Hide Create Wallet layout
//                createWalletLayout.visibility = View.GONE
                createButton.visibility = View.GONE
                //Show recover layout
                recoverWalletLayout.visibility = View.VISIBLE
                //change button text
                recoverButton.text = getString(R.string.button_wallet_recover_wallet)
            } else {
                //Recover
                var password = passwordText.text.toString()
                val aezeedText = view.findViewById<EditText>(R.id.editTextWalletAezeedPass)
                val aezeedPass = aezeedText.text.toString()
                val seedInputText = view.findViewById<EditText>(R.id.input_seed)
                val confirmPassword = confirmPasswordText.text.toString()
                if (password.length < 8) {
                    newPassLayout.error = "Password must be at least 8 characters."
                    password = ""
                } else if (password != confirmPassword) {
                    confirmPassLayout.error = "Passwords do not match. Please try again."
                    password = ""
                }
                //Check for seed length
                val seedWords = seedInputText.text.split(" ")
                if (seedWords.size != 15) {
                    seedInputText.error = "Seed must be 15 words."
                    password = ""
                }
                if ((isAdded) && (password.isNotEmpty())) {
                    statusbar.text = "Recovering wallet please wait..."
                    val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                    activity?.getColor(android.R.color.darker_gray)?.let { layout.setBackgroundColor(it) }
                    createButton.isEnabled = false

                    //B64encode password
                    var b64Password = Base64.encodeToString(password.toByteArray(), Base64.DEFAULT)
                    b64Password = b64Password.replace("\n", "")
                    var b64AezeedPass = Base64.encodeToString(aezeedPass.toByteArray(), Base64.DEFAULT)
                    b64AezeedPass = b64AezeedPass.replace("\n", "")
                    val jsonData = JSONObject()
                    jsonData.put("wallet_password", b64Password)
                    jsonData.put("aezeed_pass",b64AezeedPass)
                    val seedArray = JSONArray()
                    for (i in seedWords.indices) {
                        seedArray.put(seedWords[i])
                    }
                    jsonData.put("cipher_seed_mnemonic",seedArray)
                    Log.i(LOGTAG, "recovering PKT wallet from seed...")
                    apiController.post("http://localhost:8080/api/v1/wallet/create", jsonData)
                    //Handle response from createwallet REST request
                    { response ->
                        if ((response != null) && (response.has("seed") && !response.isNull("seed"))) {
                            //Wallet created successfully
                            Log.i(LOGTAG, "PKT recovery wallet success")
                            val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                            keyboard.hideSoftInputFromWindow(view.windowToken, 0)
                            //Recovery success go straight to wallet main
                            val walletActivity = activity as WalletActivity
                            walletActivity.switchToMain()
                        } else {
                            Log.i(LOGTAG, "PKT create wallet failed")
                            //Wallet creation failed, parse error, log and notify user
                            var errorString = response?.getString("error")
                            if (errorString != null) {
                                Log.e(LOGTAG, errorString!!)
                                //Get user friendly message
                                errorString = errorString!!.substring(errorString!!.indexOf(" "), errorString!!.indexOf("\n\n"))
                                activity?.runOnUiThread {
                                    Toast.makeText(requireContext(), "Error: $errorString", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                activity?.runOnUiThread {
                                    Toast.makeText(requireContext(), "Failed to create wallet with unknown error.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}