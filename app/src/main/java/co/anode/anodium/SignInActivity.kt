package co.anode.anodium

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.Spannable
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import org.json.JSONException
import org.json.JSONObject

class SignInActivity : AppCompatActivity() {
    private val API_VERSION = "0.3"
    private var API_SIGNIN_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/authorize/"
    private var API_USERNAME_REGISTRATION_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val signup: TextView = findViewById(R.id.textSignUp)
        val singupspanlink: Spanned = HtmlCompat.fromHtml("don't have an account yet? <a href='#'>Sign Up</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        signup.movementMethod = LinkMovementMethod.getInstance()
        signup.text = singupspanlink

        signup.setMovementMethod(object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                AnodeClient.eventLog(baseContext,"Button: Sign up pressed")
                val nicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
                startActivityForResult(nicknameActivity, 0)
            }
        })

        val forgotpassword: TextView = findViewById(R.id.textForgotPassword)
        val forgotpasswordpanlink: Spanned = HtmlCompat.fromHtml("<a href='#'>forgot password?</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        forgotpassword.movementMethod = LinkMovementMethod.getInstance()
        forgotpassword.text = forgotpasswordpanlink

        forgotpassword.setMovementMethod(object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                AnodeClient.eventLog(baseContext,"Button: Forgot password pressed")
                val forgotpasswordActivity = Intent(applicationContext, ForgotPasswordActivity::class.java)
                //val forgotpasswordActivity = Intent(applicationContext, TestActivity2::class.java)
                startActivityForResult(forgotpasswordActivity, 0)
            }
        })

        val buttonSignin = findViewById<Button>(R.id.buttonSingIn)
        buttonSignin.setOnClickListener() {
            AnodeClient.eventLog(baseContext,"Button: Sign in pressed")
            val email = findViewById<EditText>(R.id.editTextTextEmailAddress)
            val password = findViewById<EditText>(R.id.editTextTextPassword)
            if (email.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in email or username", Toast.LENGTH_SHORT).show()
            } else if (password.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in password", Toast.LENGTH_SHORT).show()
            }else {
                signIn().execute(email.text.toString(), password.text.toString())
            }
        }
        AnodeClient.eventLog(baseContext,"Activity: Sign in created")
    }

    override fun onBackPressed() {
        AnodeClient.eventLog(baseContext,"Button: Back pressed")
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        with (prefs.edit()) {
            putBoolean("SignInActivity_BackPressed",true)
            commit()
        }
        finish()
    }

    abstract class TextViewLinkHandler : LinkMovementMethod() {
        override fun onTouchEvent(widget: TextView, buffer: Spannable, event: MotionEvent): Boolean {
            if (event.action != MotionEvent.ACTION_UP) return super.onTouchEvent(widget, buffer, event)
            var x = event.x.toInt()
            var y = event.y.toInt()
            x -= widget.totalPaddingLeft
            y -= widget.totalPaddingTop
            x += widget.scrollX
            y += widget.scrollY
            val layout = widget.layout
            val line = layout.getLineForVertical(y)
            val off = layout.getOffsetForHorizontal(line, x.toFloat())
            val link = buffer.getSpans(off, off, URLSpan::class.java)
            if (link.size != 0) {
                onLinkClick(link[0].url)
            }
            return true
        }

        abstract fun onLinkClick(url: String?)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        if ((requestCode == 0) && (!prefs.getBoolean("ForgotPasswordActivity_BackPressed",true) || !prefs.getBoolean("SingUp_BackPressed",true))){
            with(prefs.edit()) {
                putBoolean("ForgotPasswordActivity_BackPressed",false)
                putBoolean("SingUp_BackPressed",false)
                commit()
            }
            this.finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    inner class isUserRegistered() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val jsonObject = JSONObject()
            jsonObject.accumulate("username", params[0])
            val resp = AnodeClient.APIHttpReq(API_USERNAME_REGISTRATION_URL, jsonObject.toString(), "POST", true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_USERNAME_REGISTRATION_URL: $result")
            if ((result.isNullOrBlank())) {
                //Do nothing
            } else if (result.contains("ERROR: ")) {
                //Do nothing
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                    if (jsonObj.has("username")) {
                        with(prefs.edit()) {
                            putBoolean("SignedIn", true)
                            putBoolean("Registered", true)
                            commit()
                        }
                    } else {
                        with(prefs.edit()) {
                            putBoolean("Registered", false)
                            commit()
                        }
                    }
                } catch (e: JSONException) {

                }
            }
        }
    }

    inner class signIn() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val jsonObject = JSONObject()
            jsonObject.accumulate("emailOrUsername", params[0])
            jsonObject.accumulate("password", params[1])
            val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            with (prefs.edit()) {
                putString("username", params[0])
                commit()
            }
            val resp = AnodeClient.APIHttpReq(API_SIGNIN_URL, jsonObject.toString(), "POST", true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received: $result")
            if (result.isNullOrBlank()) {
                //unknown
                finish()
            }
            else if (result.contains("Internal Server Error")) {
                finish()
            } else if (result.contains("400") || result.contains("401")) {
                Toast.makeText(baseContext, "Error: $result", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    if (jsonObj.has("username")) {
                        Toast.makeText(baseContext, "User signed in successfully", Toast.LENGTH_SHORT).show()
                        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                        with(prefs.edit()) {
                            putBoolean("SignedIn", true)
                            putString("username", jsonObj.getString("username"))
                            commit()
                        }
                        isUserRegistered().execute(jsonObj.getString("username"))
                        thread.start()
                    }
                } catch (e: JSONException) {

                }
            }
        }
    }

    var thread: Thread = object : Thread() {
        override fun run() {
            try {
                sleep(Toast.LENGTH_LONG.toLong()+3000)
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}