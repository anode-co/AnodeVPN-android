package co.anode.anodium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.protobuf.ByteString

class SendPaymentActivity : AppCompatActivity() {
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

        sendButton.setOnClickListener {
            var sendcoins = true
            val bsPassword: ByteString = ByteString.copyFrom("password", Charsets.UTF_8)
            //Check fields
            val address = findViewById<EditText>(R.id.editTextReceiverAddress)
            if (address.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the receiver's address", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            val amount = findViewById<EditText>(R.id.editTextPKTAmount)
            if (amount.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the PKT amount", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            val password = findViewById<EditText>(R.id.editTextPKTPassword)
            if (password.text.toString().isEmpty()) {
                Toast.makeText(applicationContext, "Please fill in the password", Toast.LENGTH_SHORT).show()
                sendcoins = false
            } else if (ByteString.copyFrom(password.text.toString(),Charsets.UTF_8) != bsPassword ) {
                Toast.makeText(applicationContext, "Wrong password", Toast.LENGTH_SHORT).show()
                sendcoins = false
            }
            //Send coins
            if (sendcoins) {
                val result = LndRPCController.sendCoins(address.text.toString(), amount.text.toString().toLong())
                if (result == "OK") {
                    Toast.makeText(applicationContext, "Payment of ${amount.text}PKT send", Toast.LENGTH_SHORT).show()
                    finish()
                } else if (result.contains("InsufficientFundsError",true)){
                    Toast.makeText(applicationContext, "Wallet does not have enough balance", Toast.LENGTH_SHORT).show()
                } else if (result.contains("custom checksum failed", true)) {
                    Toast.makeText(applicationContext, "Invalid address.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}