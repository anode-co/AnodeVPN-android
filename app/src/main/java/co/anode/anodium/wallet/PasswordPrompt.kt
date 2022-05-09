package co.anode.anodium.wallet

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import co.anode.anodium.R
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class PasswordPrompt : AppCompatActivity() {
    private lateinit var apiController: APIController
    private var currentPasswordValidated = true
    private var currentPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_prompt)
        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        AnodeClient.eventLog("Activity: PasswordPrompt created")
        val param = intent.extras
        val changePassphrase = param?.get("changepassphrase").toString().toBoolean()
        val noWallet = param?.get("noWallet").toString().toBoolean()
        val service = ServiceVolley()
        apiController = APIController(service)
        val nextButton = findViewById<Button>(R.id.button_passwordprompt_next)
        if (changePassphrase) {
            currentPasswordValidated = false
            checkCurrentPassphrase(false)
            nextButton.text = getString(R.string.action_changepassword)
            actionbar.title = getString(R.string.password_prompt_title)
        } else {
            currentPasswordValidated = true
            nextButton.text = getString(R.string.action_next)
            actionbar.title = getString(R.string.wallet_create_title)
        }

        if(noWallet) {
            findViewById<TextView>(R.id.create_wallet_description).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.create_wallet_description).visibility = View.GONE
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
                val password = findViewById<TextView>(R.id.editTextWalletPassword)
                if (changePassphrase) {
                    changePassphrase(currentPassword, password.text.toString())
                } else {
                    loadPinPrompt(password.text.toString(), noNext = false)
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

    private fun loadPinPrompt(password: String, noNext: Boolean) {
        //Go to PIN prompt activity and pass it the password
        val pinPromptActivity = Intent(applicationContext, PinPrompt::class.java)
        pinPromptActivity.putExtra("password", password)
        pinPromptActivity.putExtra("noNext", noNext)
        startActivity(pinPromptActivity)
    }

    private fun passwordsMatch():Boolean {
        val password = findViewById<TextView>(R.id.editTextWalletPassword)
        val confirmPassword = findViewById<TextView>(R.id.editTextconfirmWalletPassword)
        return (password.text.toString() == confirmPassword.text.toString())
    }

    private fun validateWalletPassphrase(password: String) {
        showLoading()
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        apiController.post(apiController.checkPassphraseURL,jsonRequest) { response ->
            hideLoading()
            if (response == null) {
                Log.i(LOGTAG, "unknown status for wallet/checkpassphrase")
            } else if ((response.has("error")) &&
                response.getString("error").contains("ErrWrongPassphrase")) {
                Log.d(LOGTAG, "Validating password, wrong password")
                currentPasswordValidated = false
            } else if (response.has("error")) {
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                currentPasswordValidated = false
            } else if (response.has("validPassphrase")) {
                currentPasswordValidated = response.getBoolean("validPassphrase")
                Log.i(LOGTAG, "validPassphrase: $currentPasswordValidated")
            } else {
                Log.e(LOGTAG, "UNEXPECTED response: $response")
                //Something unexpected has happened, close activity
                finish()
            }
            if (!currentPasswordValidated) {
                checkCurrentPassphrase(true)
            } else {
                Toast.makeText(this, "Current password validated!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun changePassphrase(currentPassword: String, newPassword: String) {
        showLoading()
        val jsonRequest = JSONObject()
        jsonRequest.put("current_passphrase", currentPassword)
        jsonRequest.put("new_passphrase", newPassword)
        apiController.post(apiController.changePassphraseURL,jsonRequest) { response ->
            hideLoading()
            if (response == null) {
                Log.i(LOGTAG, "unknown status for wallet/checkpassphrase")
            } else if ((response.has("error")) &&
                response.getString("error").contains("ErrWrongPassphrase")) {
                Log.d(LOGTAG, "Changing password, wrong current password")
                checkCurrentPassphrase(true)
            } else if (response.has("error")) {
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                Toast.makeText(this,"Error trying to change wallet password.", Toast.LENGTH_LONG).show()
            } else if (response.length() == 0) {
                Log.d(LOGTAG, "Wallet password changed")
                val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                prefs.edit().remove("wallet_pin").apply()
                prefs.edit().remove("encrypted_wallet_password")
                Toast.makeText(this, "Wallet password changed!", Toast.LENGTH_LONG).show()
                loadPinPrompt(newPassword, noNext = true)
                delayedFinish.start()
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
            currentPassword = input.text.toString()
            validateWalletPassphrase(currentPassword)
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

    private fun makeBackgroundWhite(){
        val layout = findViewById<ConstraintLayout>(R.id.password_prompt_layout)
        layout.setBackgroundColor(getColor(android.R.color.white))
    }

    private fun makeBackgroundGrey() {
        val layout = findViewById<ConstraintLayout>(R.id.password_prompt_layout)
        layout.setBackgroundColor(getColor(android.R.color.darker_gray))
    }

    private fun showLoading() {
        makeBackgroundGrey()
        val loading = findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        makeBackgroundWhite()
        val loading = findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.GONE
    }

    var delayedFinish: Thread = object : Thread() {
        override fun run() {
            try {
                sleep(Toast.LENGTH_LONG.toLong())
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}