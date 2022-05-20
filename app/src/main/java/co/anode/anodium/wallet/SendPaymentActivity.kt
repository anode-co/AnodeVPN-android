package co.anode.anodium.wallet

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.*
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
    lateinit var statusBar: TextView
    private var forcePassword = false

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

        statusBar = findViewById(R.id.textview_status)
        //Get our PKT wallet address
        val param = intent.extras
        myPKTAddress = param?.getString("walletAddress").toString()
        val walletName = param?.getString("walletName").toString()

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
                if (s.length < prevLength) {
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

        val passwordField = findViewById<EditText>(R.id.editTextPKTPassword)
        val storedPin = AnodeUtil.getWalletPin(walletName)
        if (storedPin.isNotEmpty()) {
            passwordField.inputType = InputType.TYPE_CLASS_NUMBER
            passwordField.hint = getString(R.string.prompt_newpin)
        } else {
            passwordField.inputType = InputType.TYPE_CLASS_TEXT
            passwordField.hint = getString(R.string.send_pkt_password)
        }
        passwordField.transformationMethod = PasswordTransformationMethod.getInstance()

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
            if (storedPin.isNotEmpty() && !forcePassword) {
                val encryptedPassword = AnodeUtil.getWalletPassword(walletName)
                password = AnodeUtil.decrypt(encryptedPassword, passwordField.text.toString()).toString()
            } else {
                password = passwordField.text.toString()
            }
            validateWalletPassphraseAndSendCoins(password, address.text.toString(), amount.text.toString().toFloat())
        }
    }

    private fun sendCoins(address:String, amount: Float) {
        statusBar.text = getString(R.string.wallet_sending_coins)
        val params = JSONObject()
        params.put("to_address", address)
        params.put("amount", amount)
        val fromAddresses = JSONArray()
        fromAddresses.put(myPKTAddress)
        params.put("from_address", fromAddresses)
        apiController.post(apiController.sendFromURL, params) { response ->
            if ((response != null) && response.has("txHash") && !response.isNull("txHash")) {
                statusBar.text = "Payment of $amount PKT send"
                finish()
            } else if (response.toString().contains("InsufficientFundsError",true)){
                statusBar.text = getString(R.string.wallet_not_enough_balance)
            } else if (response.toString().contains("custom checksum failed", true)) {
                statusBar.text = getString(R.string.wallet_invalid_address)
            } else {
                statusBar.text = getString(R.string.wallet_unknown_error_sending_pkt)
            }
        }
    }

    private fun validateWalletPassphraseAndSendCoins(password: String, address: String, amount: Float) {
        val layout = findViewById<ConstraintLayout>(R.id.activitysendPayment)
        layout.setBackgroundColor(getColor(android.R.color.darker_gray))
        statusBar.text = getString(R.string.wallet_password_validating)
        val loading = findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.VISIBLE
        val sendButton = findViewById<Button>(R.id.button_sendPKTPayment)
        sendButton.isEnabled = false
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        apiController.post(apiController.checkPassphraseURL,jsonRequest) { response ->
            //Reset UI elements
            loading.visibility = View.GONE
            statusBar.text = ""
            val layout = findViewById<ConstraintLayout>(R.id.activitysendPayment)
            layout.setBackgroundColor(getColor(android.R.color.white))
            sendButton.isEnabled = true
            if (response == null) {
                Log.i(LOGTAG, "unknown status for wallet/checkpassphrase")
            } else if (response.has("error")) {
                Log.e(LOGTAG, "Error: "+response.getString("error").toString())
                canSendCoins = false
            } else if (response.has("validPassphrase")) {
                if (response.getBoolean("validPassphrase")) {
                    sendCoins(address, amount)
                } else {
                    Log.d(LOGTAG, "Validating password, wrong password")
                    statusBar.text = getString(R.string.wallet_wrong_password)
                    val passwordField = findViewById<EditText>(R.id.editTextPKTPassword)
                    passwordField.text.clear()
                    passwordField.error = getString(R.string.wallet_wrong_password)
                    //switch to using password
                    forcePassword = true
                    passwordField.inputType = InputType.TYPE_CLASS_TEXT
                    passwordField.hint = getString(R.string.send_pkt_password)
                    passwordField.transformationMethod = PasswordTransformationMethod.getInstance()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}