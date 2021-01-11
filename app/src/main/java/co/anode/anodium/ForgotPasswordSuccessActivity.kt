package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class ForgotPasswordSuccessActivity : AppCompatActivity() {
    val h = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password_success)

        Log.i(LOGTAG, "Started ForgotPasswordSuccessActivity")
        setResult(0)
        val param = intent.extras
        val url = param?.getString("passwordResetStatusUrl")

        passwordresetchecker.init(h, url!!, applicationContext)
        h.postDelayed(passwordresetchecker, 1000)

        val buttonGoToEmail = findViewById<Button>(R.id.buttonGoToEmail)
        buttonGoToEmail.setOnClickListener() {
            AnodeClient.eventLog(baseContext,"Button: Go to email pressed")
            val emailClient = Intent(Intent.ACTION_MAIN)
            emailClient.addCategory(Intent.CATEGORY_APP_EMAIL)
            startActivity(emailClient)
        }

        val buttonResendEmail = findViewById<Button>(R.id.buttonResendEmail)
        buttonResendEmail.setOnClickListener() {
            AnodeClient.eventLog(baseContext,"Button: Resend email pressed")
            resendEmail().execute(url)
        }
        AnodeClient.eventLog(baseContext,"Activity: Forgot password success created")
    }
    //TODO: close activity after password reset

    inner class resendEmail() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val url = params[0]
            val resp = AnodeClient.APIHttpReq(url!!, "", "POST", true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received: $result")
            if (result.isNullOrBlank()) {
                //unknown
                finish()
            } else if (result.contains("ERROR: ")) {
                Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    if (jsonObj.has("passwordResetStatusUrl")) {
                        //TODO: go to change password with forgot password true?
                    } else if (jsonObj.has("status")) {
                        Toast.makeText(baseContext, jsonObj.getString("status") + ": " + jsonObj.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: JSONException) {
                    Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            this.finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

class passwordResetURL(context: Context?, handler: Handler?) : AsyncTask<String, Void, String>() {
    private var c: Context? = null
    private var h: Handler? = null

    init {
        c = context
        h = handler
    }

    override fun doInBackground(vararg params: String?): String? {
        return AnodeClient.APIHttpReq(params[0]!!, "", "GET", true, false)
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Log.i(LOGTAG, "Received: $result")
        if (result.isNullOrEmpty()) {
            h?.postDelayed(passwordresetchecker, 10000)
            return
        } else if (result == "202") {
            /*
            val signInActivity = Intent(c, SignInActivity::class.java)
            signInActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            c?.startActivity(signInActivity)
             */
        } else if (result.contains("ERROR: ")) {
            // nothing
        } else {
            try {
                val jsonObj = JSONObject(result)
                if (jsonObj.has("status")) {
                    val status = jsonObj.getString("status")
                    if (status == "pending") {
                        h?.postDelayed(passwordresetchecker, 10000)
                        return
                    } else if (status == "complete") {
                        h?.removeCallbacks(passwordresetchecker)
                        val passwordResetToken = jsonObj.getString("passwordResetToken")
                        val prefs = c?.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                        with(prefs!!.edit()) {
                            putString("passwordResetToken", passwordResetToken)
                            commit()
                        }
                        val changePasswordActivity = Intent(c, ChangePasswordActivity::class.java)
                        changePasswordActivity.putExtra("ForgotPassword", true)
                        changePasswordActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
                        c?.startActivity(changePasswordActivity)
                    }
                }
            } catch (e: JSONException) {
                h?.postDelayed(passwordresetchecker, 10000)
                return
            }
        }
    }
}

object passwordresetchecker: Runnable {
    private const val PollingInterval: Long = 10000
    private var h: Handler? = null
    private var url: String? = null
    private var context: Context? = null

    fun init(handler: Handler, u: String, c: Context)  {
        h = handler
        url = u
        context = c
    }

    @SuppressLint("SetTextI18n")
    override fun run() {
        try {
            passwordResetURL(context, h).execute(url)
        } catch (e: Exception) {
            Log.i(LOGTAG, "error in getting confirmation result")
            h!!.postDelayed(this, PollingInterval)
        }
    }
}