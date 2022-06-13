package co.anode.anodium.wallet

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import co.anode.anodium.R
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.LOGTAG
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import com.anton46.stepsview.StepsView
import com.google.android.material.textfield.TextInputLayout
import org.json.JSONObject

class PinPrompt : AppCompatActivity() {
    private lateinit var apiController: APIController
    private var currentPasswordValidated = true
    private var validPassword: String? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_prompt)
        val actionbar = supportActionBar
        actionbar!!.setDisplayHomeAsUpEnabled(true)
        AnodeClient.eventLog("Activity: PinPrompt created")
        val param = intent.extras
        //Getting password from password prompt
        val passwordString = param?.getString("password")
        val recoverWallet = param?.get("recoverWallet").toString().toBoolean()
        var walletName = ""
        if (param?.get("walletName").toString() != "null") {
            walletName = param?.get("walletName").toString()
            val topLabel = findViewById<TextView>(R.id.text_walletcreate_label)
            topLabel.text = getString(R.string.prompt_label_pin)+"\n$walletName"
        }
        if (!passwordString.isNullOrEmpty()) {
            //This is for when entering the activity from the wallet menu
            //in which case we ask the user to enter their password for validation
            validPassword = passwordString

        }
        val changePassphrase = param?.get("changepassphrase").toString().toBoolean()
        val noNext = param?.get("noNext").toString().toBoolean()
        val confirmPinLayout = findViewById<TextInputLayout>(R.id.confirmwalletpinLayout)
        val service = ServiceVolley()
        apiController = APIController(service)
        if (changePassphrase) {
            currentPasswordValidated = false
            checkCurrentPassphrase(false)
            actionbar.title = getString(R.string.pin_prompt_title)
        } else {
            currentPasswordValidated = true
            actionbar.title = getString(R.string.wallet_create_title)
        }
        val nextButton = findViewById<Button>(R.id.button_pinprompt_next)
        if (noNext && changePassphrase) {
            nextButton.text = getString(R.string.button_wallet_update_pin)
        } else {
            nextButton.text = getString(R.string.action_next)
            val mStepsView = findViewById<StepsView>(R.id.stepsView)
            mStepsView.visibility = View.VISIBLE
            mStepsView.setLabels(arrayOf("Password","PIN","Seed"))
                .setBarColorIndicator(resources.getColor(R.color.colorlightGrey))
                .setProgressColorIndicator(resources.getColor(R.color.colorPrimary))
                .setLabelColorIndicator(resources.getColor(R.color.colorPrimary))
                .setCompletedPosition(1)
                .drawView()
            mStepsView.setBackgroundColor(getColor(R.color.colorPrimarybackground))
        }


        nextButton.setOnClickListener {
            if (!currentPasswordValidated) {
                Toast.makeText(this,"Please validate password first.", Toast.LENGTH_SHORT).show()
                checkCurrentPassphrase(false)
                return@setOnClickListener
            }
            if (pinsMatch()) {
                confirmPinLayout.error = null

                val pin = findViewById<TextView>(R.id.editTextWalletPin).text.toString()
                if (validPassword != null) {
                    //Encrypt password using PIN and save it in encrypted shared preferences
                    val encryptedPassword = AnodeUtil.encrypt(validPassword!!, pin)
                    AnodeUtil.storeWalletPassword(encryptedPassword,walletName)
                    //Store PIN in encrypted shared preferences
                    AnodeUtil.storeWalletPin(pin,walletName)
                    if (noNext) {
                        //coming from passwordchange or pin reset
                        Toast.makeText(this, "Wallet PIN set.", Toast.LENGTH_LONG).show()
                        delayedFinish.start()
                    } else {
                        //Go to seed activity, where wallet is actually created
                        val recoveryActivity = Intent(applicationContext, RecoverySeed::class.java)
                        recoveryActivity.putExtra("password", passwordString)
                        recoveryActivity.putExtra("walletName", walletName)
                        recoveryActivity.putExtra("recoverWallet", recoverWallet)
                        startActivity(recoveryActivity)
                    }
                } else {
                    //Password is null, prompt user to enter it for validation
                    Toast.makeText(this, "No password to encrypt.", Toast.LENGTH_LONG).show()
                    checkCurrentPassphrase(false)
                }
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
        input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
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
        alert.setCanceledOnTouchOutside(false)
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
            } else if (response.has("error")) {
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
                currentPasswordValidated = false
            } else if (response.has("validPassphrase")) {
                currentPasswordValidated = response.getBoolean("validPassphrase")
                if (currentPasswordValidated) {
                    validPassword = password
                } else {
                    validPassword = null
                }
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