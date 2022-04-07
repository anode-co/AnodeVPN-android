package co.anode.anodium.wallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import co.anode.anodium.R
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class PinPrompt : AppCompatActivity() {
    private lateinit var apiController: APIController
    private var currentPasswordValidated = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_prompt)
        val actionbar = supportActionBar
        actionbar!!.title = getString(R.string.pin_prompt_title)
        actionbar.setDisplayHomeAsUpEnabled(true)
        AnodeClient.eventLog("Activity: PinPrompt created")
        val param = intent.extras
        val passwordString = param?.get("password").toString()
        val changePassphrase = param?.get("changepassphrase").toString().toBoolean()

        val confirmPinLayout = findViewById<TextInputLayout>(R.id.confirmwalletpinLayout)
        val service = ServiceVolley()
        apiController = APIController(service)
        if (changePassphrase) {
            currentPasswordValidated = false
            checkCurrentPassphrase(false)
        } else {
            currentPasswordValidated = true
        }

        val nextButton = findViewById<Button>(R.id.button_pinprompt_next)
        nextButton.setOnClickListener {
            if (!currentPasswordValidated) {
                Toast.makeText(this,"Please validate password first.", Toast.LENGTH_SHORT).show()
                checkCurrentPassphrase(false)
                return@setOnClickListener
            }
            if (pinsMatch()) {
                confirmPinLayout.error = null
                val pin = findViewById<TextView>(R.id.editTextWalletPin).text.toString()
                //Encrypt password using PIN and save it in encrypted shared preferences
                val encryptedPassword = AnodeUtil.encrypt(passwordString,pin)
                AnodeUtil.storeWalletPassword(encryptedPassword)
                //Store PIN in encrypted shared preferences
                AnodeUtil.storeWalletPin(pin)
                //Go to seed activity, where wallet is actually created
                val recoveryActivity = Intent(applicationContext, RecoverySeed::class.java)
                recoveryActivity.putExtra("password", passwordString)
                startActivity(recoveryActivity)
            } else {
                confirmPinLayout.error = getString(R.string.passwords_not_match)
            }
        }
    }

    private fun pinsMatch(): Boolean {
        val pin = findViewById<TextView>(R.id.editTextWalletPin)
        val confirmPin = findViewById<TextView>(R.id.editTextconfirmPin)
        return (pin.text.toString() == confirmPin.text.toString())
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun checkCurrentPassphrase(wrongPass: Boolean) {
        val builder = AlertDialog.Builder(this)
        if (wrongPass) {
            builder.setTitle("Wrong password")
        } else {
            builder.setTitle("")
        }
        builder.setMessage("Enter wallet password")
        val input = EditText(this)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        builder.setPositiveButton("OK"
        ) { dialog, _ ->
            val password = input.text.toString()
            validateWalletPassphrase(password)
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel"
        ) { dialog, _ ->
            currentPasswordValidated = false
            dialog.dismiss()
        }
        val alert = builder.create()
        alert.show()
    }

    private fun validateWalletPassphrase(password: String) {
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        apiController.post(apiController.checkPassphraseURL,jsonRequest) { response ->
            if (response == null) {
                Log.i(LOGTAG, "unknown status for wallet/checkpassphrase")
            } else if ((response.has("error")) &&
                response.getString("error").contains("ErrWrongPassphrase")) {
                Log.d(LOGTAG, "Validating password, wrong password")
                currentPasswordValidated = false
                checkCurrentPassphrase(true)
            } else if (response.has("error")) {
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                currentPasswordValidated = false
            } else if (response.length() == 0) {
                //empty response is success
                currentPasswordValidated = true
            }
        }
    }
}