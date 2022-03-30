package co.anode.anodium.wallet

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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
import java.io.File

class WalletFragmentSetup : Fragment() {
    lateinit var statusbar: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.walletfragment_setup, container, false)
    }

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
        val changePassphraseButton = view.findViewById<Button>(R.id.button_wallet_change_passphrase)
        changePassphraseButton.visibility = View.GONE

        statusbar = view.findViewById(R.id.textview_status)
        val pinText = view.findViewById<TextView>(R.id.editTextWalletPassword)
        val confirmPinText = view.findViewById<TextView>(R.id.editTextconfirmWalletPassword)
        val newPassLayout = view.findViewById<TextInputLayout>(R.id.newwalletpasswordLayout)
        val confirmPassLayout = view.findViewById<TextInputLayout>(R.id.confirmwalletpasswordLayout)
        val recoverWalletLayout = view.findViewById<LinearLayout>(R.id.recover_seed_layout)

        recoverWalletLayout.visibility = View.GONE
        //Initialize Volley Service
        val service = ServiceVolley()
        val apiController = APIController(service)

        val loading = view.findViewById<ProgressBar>(R.id.loadingAnimation)
        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
        val walletFile = File(requireActivity().filesDir.toString() + "/pkt/wallet.db")
        if (walletFile.exists() && !prefs.getBoolean("PINGeneratedPassphrase",false)) {
            //wallet exists but not created using pin generated password
            createButton.visibility = View.GONE
            recoverButton.visibility = View.GONE
            changePassphraseButton.visibility = View.VISIBLE
            val label = view.findViewById<TextView>(R.id.text_walletcreate_label)
            label.text = getString(R.string.label_wallet_change_password_msg)
        }

        changePassphraseButton.setOnClickListener {
            AnodeClient.eventLog("Button: Change Password clicked")
            Log.i(LOGTAG, "WalletFragmentSetup changing password")
            //Remove old error message
            newPassLayout.error = null
            confirmPassLayout.error = null
            //Generate password using pin
            var pin = pinText.text.toString()
            val confirmpin = confirmPinText.text.toString()
            var password = ""
            if (pin != confirmpin) {
                confirmPassLayout.error = getString(R.string.pins_not_match)
                pin = ""
            } else {
                password = AnodeUtil.getTrustedPassword(pin)
            }
            loading.visibility = View.VISIBLE
            if ((isAdded) && (password.isNotEmpty())) {
                statusbar.text = "changing wallet password..."
                //Hide soft keyboard
                val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(view.windowToken, 0)
                val jsonData = JSONObject()
                val currentPassword = AnodeUtil.getKeyFromEncSharedPreferences("wallet_password")
                var b64currentPass = android.util.Base64.encodeToString(currentPassword.toByteArray(), android.util.Base64.DEFAULT)
                b64currentPass = b64currentPass.replace("\n","")
                jsonData.put("current_password_bin", b64currentPass)
                jsonData.put("new_passphrase", password)
                apiController.post(apiController.changePassphraseURL, jsonData)
                { response ->
                    if ((response != null) && (!response.has("error"))){
                        //Empty response is success
                        //Update flag
                        val prefs = requireActivity().getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
                        prefs.edit().putBoolean("PINGeneratedPassphrase", true).apply()
                        //Close fragment switch to wallet
                        val walletActivity = activity as WalletActivity
                        walletActivity.switchToMain()
                    } else {
                        //Failed to change password
                        Log.e(LOGTAG, "Error in changing pkt wallet password: "+response.toString())
                        loading.visibility = View.GONE
                        Toast.makeText(requireContext(), "An error occured while trying to change the wallet password.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        createButton.setOnClickListener {
            AnodeClient.eventLog("Button: Create PKT wallet clicked")
            Log.i(LOGTAG, "WalletFragmentSetup creating wallet")
            //Remove old error message
            newPassLayout.error = null
            confirmPassLayout.error = null
            //Generate password using pin
            var pin = pinText.text.toString()
            val confirmpin = confirmPinText.text.toString()
            var password = ""
            if (pin != confirmpin) {
                confirmPassLayout.error = getString(R.string.pins_not_match)
                pin = ""
            } else {
                password = AnodeUtil.getTrustedPassword(pin)
            }

            if ((isAdded) && (password.isNotEmpty())) {
                statusbar.text = getString(R.string.creating_wallet)
                loading.visibility = View.VISIBLE
                val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                activity?.getColor(android.R.color.darker_gray)?.let { layout.setBackgroundColor(it) }
                createButton.visibility = View.GONE
                recoverButton.visibility = View.GONE
                //Hide soft keyboard
                val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(view.windowToken, 0)
                val jsonData = JSONObject()
                jsonData.put("seed_passphrase", password)
                Log.i(LOGTAG, "generating wallet seed...")
                apiController.post(apiController.createSeedURL, jsonData)
                { response ->
                    if ((response != null) && (response.has("seed") && !response.isNull("seed"))) {
                        Log.i(LOGTAG, "wallet seed created")
                        instructionsTextview.visibility = View.VISIBLE
                        //Get seed
                        val seedText = view.findViewById<TextView>(R.id.seed_column)
                        val seedArray = response.getJSONArray("seed")
                        var seedString = ""
                        for (i in 0 until seedArray.length()) {
                            seedString += seedArray.getString(i) + " "
                        }
                        seedText.text = seedString
                        //Create wallet
                        createWallet(view, apiController, password, seedArray)
                    } else {
                        Log.e(LOGTAG, "Error in generating wallet seed")
                        loading.visibility = View.GONE
                        Toast.makeText(requireContext(), "Failed to generate wallet seed.", Toast.LENGTH_LONG).show()
                        resetLayout(view)
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
                var pin = pinText.text.toString()
                val confirmpin = confirmPinText.text.toString()
                var password = ""
                if (pin != confirmpin) {
                    confirmPassLayout.error = getString(R.string.pins_not_match)
                    pin = ""
                } else {
                    password = AnodeUtil.getTrustedPassword(pin)
                }

                //Hide soft keyboard
                val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                keyboard.hideSoftInputFromWindow(view.windowToken, 0)
                val seedInputText = view.findViewById<EditText>(R.id.input_seed)
                //Check for seed length
                val seedWords = seedInputText.text.split(" ")
                if (seedWords.size != 15) {
                    seedInputText.error = getString(R.string.wallet_seed_length_msg)
                    pin = ""
                }
                val seedArray = JSONArray()
                for (i in seedWords.indices) {
                    seedArray.put(seedWords[i])
                }
                if ((isAdded) && (pin.isNotEmpty())) {
                    loading.visibility = View.VISIBLE
                    val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                    activity?.getColor(android.R.color.darker_gray)?.let { layout.setBackgroundColor(it) }
                    createButton.visibility = View.GONE
                    recoverButton.visibility = View.GONE
                    //Hide soft keyboard
                    val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    keyboard.hideSoftInputFromWindow(view.windowToken, 0)

                    val seedPassText = view.findViewById<EditText>(R.id.editTextWalletAezeedPass)
                    recoverWallet(view, apiController, password, seedPassText.text.toString(), seedArray)
                }
            }
        }
    }

    private fun recoverWallet(view: View, apiController: APIController, password: String, seedPass:String, seedArray: JSONArray) {
        Log.i(LOGTAG, "recovering PKT wallet...")
        //store password
        AnodeUtil.storePassword(password)
        statusbar.text = getString(R.string.recovering_wallet)
        val jsonData = JSONObject()
        if (seedPass.isNotEmpty()) {
            jsonData.put("seed_passphrase", seedPass)
        }
        jsonData.put("wallet_passphrase", password)
        jsonData.put("wallet_seed", seedArray)
        initWallet(view, apiController, jsonData, true)
    }

    private fun createWallet(view: View, apiController: APIController, password: String, seedArray: JSONArray) {
        Log.i(LOGTAG, "creating PKT wallet...")
        //store password
        AnodeUtil.storePassword(password)
        statusbar.text = getString(R.string.creating_wallet)
        val jsonData = JSONObject()
        jsonData.put("wallet_passphrase", password)
        jsonData.put("wallet_seed", seedArray)
        initWallet(view, apiController, jsonData, false)
    }

    private fun initWallet(view: View, apiController: APIController, jsonData: JSONObject, isRecovery:Boolean) {
        apiController.post(apiController.walletCreateURL, jsonData)
        { response ->
            val loading = view.findViewById<ProgressBar>(R.id.loadingAnimation)
            if ((response != null) &&
                (!response.has("message")) &&
                (!response.has("error"))) {
                //Hide loading animation
                loading.visibility = View.GONE
                //Set background color to white
                val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
                val passwordsLayout = view.findViewById<LinearLayout>(R.id.create_wallet_password_layout)
                passwordsLayout.visibility = View.GONE

                //Hide create button and show Close one
                val createButton = view.findViewById<Button>(R.id.button_wallet_create)
                val recoverButton = view.findViewById<Button>(R.id.button_wallet_recover_from_seed)
                val closeButton = view.findViewById<Button>(R.id.button_wallet_close)
                createButton.visibility = View.GONE
                recoverButton.visibility = View.GONE
                closeButton.visibility = View.VISIBLE
                statusbar.text = ""
                val label = view.findViewById<TextView>(R.id.text_walletcreate_label)
                //Show Seed if not from recovery mode
                if (!isRecovery) {
                    val seedLayout = view.findViewById<LinearLayout>(R.id.create_wallet_show_seed)
                    seedLayout.visibility = View.VISIBLE
                    label.text = getString(R.string.wallet_create_seed_label)
                } else {
                    val recoverWalletLayout = view.findViewById<LinearLayout>(R.id.recover_seed_layout)
                    recoverWalletLayout.visibility = View.GONE
                    label.text = getString(R.string.wallet_recovery_success)
                }
                val prefs = requireActivity().getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
                prefs.edit().putBoolean("PINGeneratedPassphrase", true).apply()
            } else {
                loading.visibility = View.GONE
                Log.i(LOGTAG, "PKT create wallet failed")
                //Wallet creation failed, parse error, log and notify user
                var errorString = response?.getString("error")
                if ((isRecovery) && (errorString!!.contains("The birthday of this seed appears to be"))) {
                    Toast.makeText(requireContext(), "Wrong seed password.", Toast.LENGTH_LONG).show()
                } else  if (errorString != null) {
                    Log.e(LOGTAG, errorString)
                    //Get user friendly message
                    errorString = errorString.substring(errorString.indexOf(" "), errorString.indexOf("\n\n"))
                    Toast.makeText(requireContext(), "Error: $errorString", Toast.LENGTH_LONG).show()
                }
                resetLayout(view)
            }
        }
    }
    /**
     * Will reset the layout to initial state
     */
    private fun resetLayout(view: View) {
        val closeButton = view.findViewById<Button>(R.id.button_wallet_close)
        val instructionsTextview = view.findViewById<TextView>(R.id.text_walletcreate_seed_instructions)
        val seedLayout = view.findViewById<LinearLayout>(R.id.create_wallet_show_seed)
        val passwordsLayout = view.findViewById<LinearLayout>(R.id.create_wallet_password_layout)
        val createButton = view.findViewById<Button>(R.id.button_wallet_create)
        val recoverButton = view.findViewById<Button>(R.id.button_wallet_recover_from_seed)
        statusbar = view.findViewById(R.id.textview_status)
        val pinText = view.findViewById<TextView>(R.id.editTextWalletPassword)
        val confirmPinText = view.findViewById<TextView>(R.id.editTextconfirmWalletPassword)
        val newPassLayout = view.findViewById<TextInputLayout>(R.id.newwalletpasswordLayout)
        val confirmPassLayout = view.findViewById<TextInputLayout>(R.id.confirmwalletpasswordLayout)
        val recoverWalletLayout = view.findViewById<LinearLayout>(R.id.recover_seed_layout)
        val loading = view.findViewById<ProgressBar>(R.id.loadingAnimation)
        //Hide
        seedLayout.visibility = View.GONE
        closeButton.visibility = View.GONE
        instructionsTextview.visibility = View.GONE
        newPassLayout.visibility = View.GONE
        recoverWalletLayout.visibility = View.GONE
        loading.visibility = View.GONE
        //Show
        passwordsLayout.visibility = View.VISIBLE
        createButton.visibility = View.VISIBLE
        recoverButton.visibility = View.VISIBLE
        confirmPassLayout.visibility = View.VISIBLE
        //Clear
        statusbar.text = ""
        pinText.text = ""
        confirmPinText.text = ""
    }
}