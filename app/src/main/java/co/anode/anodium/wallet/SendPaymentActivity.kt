package co.anode.anodium.wallet

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.R
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject

class SendPaymentActivity : AppCompatActivity() {
    lateinit var apiController: APIController
    private var myPKTAddress = ""
    private var canSendCoins = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_payment)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.send_pkt_send_payment)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        val sendButton = findViewById<Button>(R.id.button_sendPKTPayment)
        val address = findViewById<EditText>(R.id.editTextReceiverAddress)
        var ignoreTextChanged = false
        //Initialize handlers
        val service = ServiceVolley()
        apiController = APIController(service)

        //Get our PKT wallet address
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        myPKTAddress = prefs.getString("lndwalletaddress", "").toString()
        var prevLength = 0
        val addressLayout = findViewById<TextInputLayout>(R.id.pktaddressLayout)
        //automatically trim pasted addresses
        address.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                prevLength = s!!.length
                if (ignoreTextChanged) {
                    ignoreTextChanged = false
                    return
                }
                if (s!!.length < prevLength) {
                    return
                }
                val longRegex = "(pkt1)([a-zA-Z0-9]{59})".toRegex()
                val shortRegex = "(pkt1)([a-zA-Z0-9]{39})".toRegex()
                var trimmedAddress = longRegex.find(s.toString(),0)?.value
                if (trimmedAddress.isNullOrEmpty()){
                    trimmedAddress = shortRegex.find(s.toString(),0)?.value
                    if (trimmedAddress.isNullOrEmpty()) {
                        addressLayout.error = "Invalid PKT address"
                    } else {
                        ignoreTextChanged = true
                        address.setText(trimmedAddress)
                        addressLayout.error = null
                    }
                } else {
                    ignoreTextChanged = true
                    address.setText(trimmedAddress)
                    addressLayout.error = null
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        //TODO: set edittext to pin or password
        val passwordField = findViewById<EditText>(R.id.editTextPKTPassword)
        val storedPin = AnodeUtil.getWalletPin()
        if (storedPin.isNotEmpty()) {
            passwordField.inputType = InputType.TYPE_CLASS_NUMBER
            passwordField.hint = getString(R.string.prompt_newpin)
        } else {
            passwordField.inputType = InputType.TYPE_CLASS_TEXT
            passwordField.hint = getString(R.string.send_pkt_password)
        }


        sendButton.setOnClickListener {
            val amount = findViewById<EditText>(R.id.editTextPKTAmount)
            //Check fields
            if (address.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the receiver's address", Toast.LENGTH_SHORT).show()
                canSendCoins = false
                return@setOnClickListener
            }
            if (amount.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the PKT amount", Toast.LENGTH_SHORT).show()
                canSendCoins = false
                return@setOnClickListener
            }
            var password = ""
            if (storedPin.isNotEmpty()) {
                val encryptedPassword = AnodeUtil.getWalletPassword()
                password = AnodeUtil.decrypt(encryptedPassword, passwordField.text.toString())
            } else {
                password = passwordField.text.toString()
            }
            validateWalletPassphraseAndSendCoins(password, address.text.toString(), amount.text.toString().toFloat())
        }
    }

    private fun sendCoins(address:String, amount: Float) {
        val params = JSONObject()
        params.put("to_address", address)
        params.put("amount", amount)
        val fromAddresses = JSONArray()
        fromAddresses.put(myPKTAddress)
        params.put("from_address", fromAddresses)
        apiController.post(apiController.sendFromURL, params) { response ->
            if ((response != null) && response.has("txHash") && !response.isNull("txHash")) {
                Toast.makeText(applicationContext, "Payment of $amount PKT send", Toast.LENGTH_SHORT).show()
                finish()
            } else if (response.toString().contains("InsufficientFundsError",true)){
                Toast.makeText(applicationContext, "Wallet does not have enough balance", Toast.LENGTH_SHORT).show()
            } else if (response.toString().contains("custom checksum failed", true)) {
                Toast.makeText(applicationContext, "Invalid address.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(applicationContext, "Unknown error when trying to send", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateWalletPassphraseAndSendCoins(password: String, address: String, amount: Float) {
        val loading = findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.VISIBLE
        val layout = findViewById<ConstraintLayout>(R.id.activitysendPayment)
        layout.setBackgroundColor(getColor(android.R.color.darker_gray))
        Toast.makeText(this,"Validating your password.", Toast.LENGTH_SHORT).show()
        val sendButton = findViewById<Button>(R.id.button_sendPKTPayment)
        sendButton.isEnabled = false
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        apiController.post(apiController.checkPassphraseURL,jsonRequest) { response ->
            //Reset UI elements
            loading.visibility = View.GONE
            val layout = findViewById<ConstraintLayout>(R.id.activitysendPayment)
            layout.setBackgroundColor(getColor(android.R.color.white))
            sendButton.isEnabled = true
            if (response == null) {
                Log.i(LOGTAG, "unknown status for wallet/checkpassphrase")
            } else if ((response.has("error")) &&
                response.getString("error").contains("ErrWrongPassphrase")) {
                Log.d(LOGTAG, "Validating password, wrong password")
                canSendCoins = false
            } else if (response.has("error")) {
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                canSendCoins = false
            } else if (response.length() == 0) {
                //empty response is success
                sendCoins(address, amount)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}