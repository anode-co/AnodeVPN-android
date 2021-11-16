package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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


class AccountMainActivity : AppCompatActivity() {
    private val apiVersion = "0.3"
    private var apiEmailRegistrationUrl = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/<username>/initialemail/"
    private var apiPasswordRegistrationUrl = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/<username>/initialpassword/"
    var prefs: SharedPreferences? = null

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_main)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.action_sign_up)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        prefs = baseContext.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val signIn: TextView = findViewById(R.id.textSignIn)
        val link: Spanned = HtmlCompat.fromHtml("already have an account? <a href='#'>Sign in</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        signIn.movementMethod = LinkMovementMethod.getInstance()
        signIn.text = link

        val skipButton: Button = findViewById(R.id.buttonSkip)
        skipButton.setOnClickListener() {
            AnodeClient.eventLog(baseContext, "Button: SKIP pressed")
            setResult(0)
            finish()
        }

        //val signInLink = findViewById<TextView>(R.id.textSignIn)
        signIn.movementMethod = object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                AnodeClient.eventLog(baseContext, "Button: Sing in Link pressed")
                val signInActivity = Intent(applicationContext, SignInActivity::class.java)
                startActivityForResult(signInActivity, 0)
            }
        }
        AnodeClient.eventLog(baseContext, "Activity: AcccountMain created")

    }

    override fun onStart() {
        super.onStart()
        val createAccountButton: Button = findViewById(R.id.buttonCreateAccount)
        createAccountButton.setOnClickListener() {
            AnodeClient.eventLog(baseContext, "Button: CREATE ACCOUNT pressed")
            val emailPattern: String = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
            val email = findViewById<EditText>(R.id.editTextTextEmailAddress)
            //val passwordPattern: String = "(?=.*\\d)(?=.*[A-Za-z]).{9,}"
            val password = findViewById<EditText>(R.id.editTextTextPassword)
            //Check email field
            if (email.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in email field", Toast.LENGTH_SHORT).show()
            } else if (!email.text.toString().trim().matches(emailPattern.toRegex())) {
                Toast.makeText(baseContext, "Email is not valid", Toast.LENGTH_SHORT).show()
            }
            else {
                AnodeClient.mycontext = baseContext
                val username = prefs!!.getString("username", "")
                if (password.text.toString().isNotEmpty())
                    fieldRegistration().execute("password", password.text.toString(), username)
                fieldRegistration().execute("email", email.text.toString(), username)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
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

    override fun onBackPressed() {
        AnodeClient.eventLog(baseContext, "Button: Back pressed")
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putBoolean("SingUp_BackPressed", true)
            commit()
        }
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            this.finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    inner class fieldRegistration() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val jsonObject = JSONObject()
            val username = params[2]
            var url = ""
            if (username.isNullOrEmpty()) {
                Log.i(LOGTAG, "Error empty username")
                return ""
            }
            when {
                params[0] == "email" -> {
                    url = apiEmailRegistrationUrl.replace("<username>", username, false)
                    jsonObject.accumulate("email", params[1])
                }
                params[0] == "password" -> {
                    url = apiPasswordRegistrationUrl.replace("<username>", username, false)
                    jsonObject.accumulate("password", params[1])
                }
                else -> {
                    Log.i(LOGTAG, "Error unknown field: ${params[0]}")
                }
            }
            //val resp = AnodeClient.httpAuthReq(url, jsonObject.toString(), "POST")
            val resp = AnodeClient.APIHttpReq(url, jsonObject.toString(), "POST", true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG, "Received: $result")
            if ((result.isNullOrBlank())) {
                finish()
            } else if (result.contains("ERROR: ")) {
                Toast.makeText(baseContext, "Error: $result", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    if (jsonObj.has("status")) {//initial password response
                        val msg = jsonObj.getString("message")
                        Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    } else if (jsonObj.has("passwordRecoveryToken")) {
                    } else if (jsonObj.has("accountConfirmationStatusUrl")) { //initial email response
                        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                        with(prefs.edit()) {
                            putBoolean("SignedIn", true)
                            putBoolean("Registered", true)
                            commit()
                        }
                        val accountConfirmation = jsonObj.getString("accountConfirmationStatusUrl")
                        val verificationActivity = Intent(applicationContext, VerificationActivity::class.java)
                        verificationActivity.putExtra("accountConfirmationStatusUrl", accountConfirmation)
                        startActivityForResult(verificationActivity, 0)
                    }
                }catch (e: JSONException) {
                    Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}