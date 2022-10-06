package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import co.anode.anodium.BuildConfig
import co.anode.anodium.R
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import com.anton46.stepsview.StepsView
import com.google.android.material.textfield.TextInputLayout
import com.ybs.passwordstrengthmeter.PasswordStrength
import org.json.JSONObject


class PasswordPrompt : AppCompatActivity() {
    private val LOGTAG = BuildConfig.APPLICATION_ID
    private var currentPasswordValidated = true
    private var currentPassword = ""
    private var changePassphrase = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_password_prompt)
        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        AnodeClient.eventLog("Activity: PasswordPrompt created")
        val param = intent.extras
        changePassphrase = param?.get("changepassphrase").toString().toBoolean()
        var walletName = ""
        if (param?.get("walletName").toString() != "null") {
            walletName = param?.get("walletName").toString()
            val topLabel = findViewById<TextView>(R.id.passwordprompt_top_label)
            topLabel.text = getString(R.string.password_prompt_label_password)+"\n$walletName"
        }
        val noWallet = param?.get("noWallet").toString().toBoolean()
        val recoverWallet = param?.get("recoverWallet").toString().toBoolean()

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
            val mStepsView = findViewById<StepsView>(R.id.stepsView)
            mStepsView.visibility = View.VISIBLE
            mStepsView.setLabels(arrayOf("Password","PIN","Seed"))
                .setBarColorIndicator(resources.getColor(R.color.colorlightGrey))
                .setProgressColorIndicator(resources.getColor(R.color.colorPrimary))
                .setLabelColorIndicator(resources.getColor(R.color.colorPrimary))
                .setCompletedPosition(0)
                .drawView()
            mStepsView.setBackgroundColor(getColor(R.color.colorPrimarybackground))
        }

        if(noWallet) {
            findViewById<TextView>(R.id.create_wallet_description).visibility = View.VISIBLE
        } else {
            findViewById<TextView>(R.id.create_wallet_description).visibility = View.GONE
        }

        val confirmPassLayout = findViewById<TextInputLayout>(R.id.confirmwalletpasswordLayout)
        val password = findViewById<TextView>(R.id.editTextWalletPassword)
        password.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updatePasswordStrengthView(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        nextButton.setOnClickListener {
            if (!currentPasswordValidated) {
                Toast.makeText(this,"Please validate current password first.",Toast.LENGTH_SHORT).show()
                checkCurrentPassphrase(false)
                return@setOnClickListener
            }
            if (passwordsMatch()) {
                confirmPassLayout.error = null
                //Reset stored encrypted password and PIN
                val prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                prefs.edit().remove("encrypted_wallet_password").apply()
                prefs.edit().remove("wallet_pin").apply()


                if (changePassphrase) {
                    changePassphrase(currentPassword, password.text.toString())
                } else {
                    loadPinPrompt(password.text.toString(), noNext = false, walletName, recoverWallet)
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

    private fun loadPinPrompt(password: String, noNext: Boolean, newWallet: String, recover: Boolean) {
        //Go to PIN prompt activity and pass it the password
        val pinPromptActivity = Intent(applicationContext, PinPrompt::class.java)
        pinPromptActivity.putExtra("password", password)
        pinPromptActivity.putExtra("noNext", noNext)
        pinPromptActivity.putExtra("walletName", newWallet)
        pinPromptActivity.putExtra("recoverWallet", recover)
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
        AnodeUtil.apiController.post(AnodeUtil.apiController.checkPassphraseURL,jsonRequest) { response ->
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
        AnodeUtil.apiController.post(AnodeUtil.apiController.changePassphraseURL,jsonRequest) { response ->
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
                val prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                prefs.edit().remove("wallet_pin").apply()
                prefs.edit().remove("encrypted_wallet_password")
                Toast.makeText(this, "Wallet password changed!", Toast.LENGTH_LONG).show()
                if (!changePassphrase) {
                    loadPinPrompt(newPassword, noNext = true, "", recover = false)
                }
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
        input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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
        alert.setCanceledOnTouchOutside(false)
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

    private fun updatePasswordStrengthView(password: String) {
        val progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        val strengthView = findViewById<View>(R.id.password_strength) as TextView
        if (TextView.VISIBLE != strengthView.visibility) return
        if (password.isEmpty()) {
            strengthView.text = ""
            progressBar.progress = 0
            return
        }
        val str = PasswordStrength.calculateStrength(password)
        strengthView.text = str.getText(this)
        strengthView.setTextColor(str.color)
        progressBar.progressDrawable.setColorFilter(str.color, PorterDuff.Mode.SRC_IN)
        if (str.getText(this) == "Weak") {
            progressBar.progress = 25
        } else if (str.getText(this) == "Medium") {
            progressBar.progress = 50
        } else if (str.getText(this) == "Strong") {
            progressBar.progress = 75
        } else {
            progressBar.progress = 100
        }
    }
}