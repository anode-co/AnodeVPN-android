package co.anode.anodium.wallet

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.R
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONArray
import org.json.JSONObject

class SendPaymentActivity : AppCompatActivity() {
    lateinit var apiController: APIController
    private var myPKTAddress = ""

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

        sendButton.setOnClickListener {
            var sendcoins = true
            val walletPassword = AnodeUtil.getKeyFromEncSharedPreferences("wallet_password")
            val storedb64Password = android.util.Base64.encodeToString(walletPassword.toByteArray(), android.util.Base64.DEFAULT)
            val passwordField = findViewById<EditText>(R.id.editTextPKTPassword)
            val b64Password = android.util.Base64.encodeToString(passwordField.text.toString().toByteArray(), android.util.Base64.DEFAULT)
            //Check fields
            if (address.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the receiver's address", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            val amount = findViewById<EditText>(R.id.editTextPKTAmount)
            if (amount.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the PKT amount", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            if (passwordField.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the password", Toast.LENGTH_SHORT).show()
                sendcoins = false
            } else if (b64Password != storedb64Password) {
                Toast.makeText(applicationContext, "Wrong password", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            //Send coins
            if (sendcoins) {
                //val result = LndRPCController.sendCoins(address.text.toString(), amount.text.toString().toLong())
                val params = JSONObject()
                //Exclude mining transactions
                params.put("to_address", address.text.toString())
                params.put("amount", amount.text.toString().toFloat())
                val fromAddresses = JSONArray()
                fromAddresses.put(myPKTAddress)
                params.put("from_address", fromAddresses)
                apiController.post(apiController.sendFromURL, params) { response ->
                    if ((response != null) && response.has("txHash") && !response.isNull("txHash")) {
                        Toast.makeText(applicationContext, "Payment of ${amount.text}PKT send", Toast.LENGTH_SHORT).show()
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
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}