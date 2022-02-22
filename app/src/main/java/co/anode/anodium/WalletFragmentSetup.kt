package co.anode.anodium

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.Fragment
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.textfield.TextInputLayout
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
        //Hide seed layout
        val seedLayout = view.findViewById<LinearLayout>(R.id.create_wallet_show_seed)
        seedLayout.visibility = View.GONE
        //Show passwords
        val passwordsLayout = view.findViewById<LinearLayout>(R.id.create_wallet_password_layout)
        passwordsLayout.visibility = View.VISIBLE
        val createButton = view.findViewById<Button>(R.id.button_wallet_create)
        statusbar = view.findViewById<TextView>(R.id.textview_status)
        val passwordText = view.findViewById<TextView>(R.id.editTextWalletPassword)
        val confirmPasswordText = view.findViewById<TextView>(R.id.editTextconfirmWalletPassword)
        val newPassLayout = view.findViewById<TextInputLayout>(R.id.newwalletpasswordLayout)
        val confirmPassLayout = view.findViewById<TextInputLayout>(R.id.confirmwalletpasswordLayout)
        //Initialize Volley Service
        val service = ServiceVolley()
        val apiController = APIController(service)

        createButton.setOnClickListener {
            AnodeClient.eventLog(requireContext(), "Button: Create PKT wallet clicked")
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
                        //Set background color to white
                        val layout = view.findViewById<ConstraintLayout>(R.id.wallet_fragmentCreate)
                        activity?.getColor(android.R.color.white)?.let { layout.setBackgroundColor(it) }
                        //Hide soft keyboard
                        val keyboard = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        keyboard.hideSoftInputFromWindow(view.windowToken, 0)
                        //Show Seed and hide passwords
                        passwordsLayout.visibility = View.GONE
                        seedLayout.visibility = View.VISIBLE
                        //Hide create button and show Close one
                        createButton.visibility = View.GONE
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

            closeButton.setOnClickListener {
                activity?.finish()
            }
        }
    }
}