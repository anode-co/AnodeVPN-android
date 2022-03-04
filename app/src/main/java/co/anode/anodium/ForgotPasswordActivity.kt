@file:Suppress("DEPRECATION")

package co.anode.anodium

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class ForgotPasswordActivity : AppCompatActivity() {
    private val apiVersion = "0.3"
    private var apiForgotPasswordUrl = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/<email_or_username>/password/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.title_forgotpassword)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        val buttonContinue = findViewById<Button>(R.id.buttonForgotPasswordContinue)
        buttonContinue.setOnClickListener {
            AnodeClient.eventLog("Button: Continue pressed")
            val email = findViewById<EditText>(R.id.editTextForgotPassEmailAddress)
            if (email.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in email field", Toast.LENGTH_SHORT).show()
            } else if (!email.text.toString().trim().matches(emailPattern.toRegex())) {
                Toast.makeText(baseContext, "Email is not valid", Toast.LENGTH_SHORT).show()
            } else {//Send API call
                forgotPassword().execute(email.text.toString())
            }
        }
        AnodeClient.eventLog("Activity: Forgot password created")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("ForgotPasswordActivity_BackPressed", true)
            commit()
        }
        finish()
    }

    inner class forgotPassword : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val email = params[0]
            apiForgotPasswordUrl = email?.let { apiForgotPasswordUrl.replace("<email_or_username>", it,true) }.toString()
            val resp = AnodeClient.APIHttpReq(apiForgotPasswordUrl, "", "POST", needsAuth = true, isRetry = false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received: $result")
            if (result.isNullOrBlank()) {
                //unknown
                finish()
            } else if (result.contains("ERROR: ") ) {
                Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    if (jsonObj.has("passwordResetStatusUrl")) {
                        //Start activity
                        val forgotPasswordSuccessActivityActivity = Intent(applicationContext, ForgotPasswordSuccessActivity::class.java)
                        forgotPasswordSuccessActivityActivity.putExtra("passwordResetStatusUrl", jsonObj.getString("passwordResetStatusUrl"))
                        startActivityForResult(forgotPasswordSuccessActivityActivity, 0)
                    } else if (jsonObj.has("status")) {
                        Toast.makeText(baseContext, jsonObj.getString("status") + ": " + jsonObj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                }catch (e: JSONException) {
                    Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}