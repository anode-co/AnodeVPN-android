package co.anode.anodium.wallet

import android.content.Context
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
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class PasswordPrompt : AppCompatActivity() {
    private lateinit var apiController: APIController
    private var currentPasswordValidated = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_prompt)
        val actionbar = supportActionBar
        actionbar!!.title = getString(R.string.password_prompt_title)
        actionbar.setDisplayHomeAsUpEnabled(true)
        AnodeClient.eventLog("Activity: PasswordPrompt created")
        val param = intent.extras
        val changePassphrase = param?.get("changepassphrase").toString().toBoolean()
        val service = ServiceVolley()
        apiController = APIController(service)
        val nextButton = findViewById<Button>(R.id.button_passwordprompt_next)
        if (changePassphrase) {
            currentPasswordValidated = false
            checkCurrentPassphrase(false)
            nextButton.text = getString(R.string.action_changepassword)
        } else {
            currentPasswordValidated = true
            nextButton.text = getString(R.string.action_next)
        }
        val confirmPassLayout = findViewById<TextInputLayout>(R.id.confirmwalletpasswordLayout)

        nextButton.setOnClickListener {
            if (!currentPasswordValidated) {
                Toast.makeText(this,"Please validate current password first.",Toast.LENGTH_SHORT).show()
                checkCurrentPassphrase(false)
                return@setOnClickListener
            }
            if (passwordsMatch()) {
                confirmPassLayout.error = null
                //Reset stored encrypted password and PIN
                val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                prefs.edit().remove("encrypted_wallet_password").apply()
                prefs.edit().remove("wallet_pin").apply()
                if (changePassphrase) {
                    //TODO: call change password REST API
                } else {
                    val password = findViewById<TextView>(R.id.editTextWalletPassword)
                    //Go to PIN prompt activity and pass it the password
                    val pinPromptActivity = Intent(applicationContext, PinPrompt::class.java)
                    pinPromptActivity.putExtra("password", password.text)
                    startActivity(pinPromptActivity)
                }
            } else {
                confirmPassLayout.error = getString(R.string.passwords_not_match)
            }
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onBackPressed() {
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun passwordsMatch():Boolean {
        val password = findViewById<TextView>(R.id.editTextWalletPassword)
        val confirmPassword = findViewById<TextView>(R.id.editTextconfirmWalletPassword)
        return (password.text.toString() == confirmPassword.text.toString())
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

    private fun checkCurrentPassphrase(wrongPass: Boolean) {
        val builder = AlertDialog.Builder(this)
        if (wrongPass) {
            builder.setTitle("Wrong password")
        } else {
            builder.setTitle("")
        }
        builder.setMessage("Enter current wallet password")
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
}