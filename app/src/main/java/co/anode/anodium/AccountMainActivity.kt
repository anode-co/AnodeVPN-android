package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.LOGTAG
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors


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
        skipButton.setOnClickListener {
            AnodeClient.eventLog( "Button: SKIP pressed")
            setResult(0)
            finish()
        }

        //val signInLink = findViewById<TextView>(R.id.textSignIn)
        signIn.movementMethod = object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                AnodeClient.eventLog( "Button: Sing in Link pressed")
                val signInActivity = Intent(applicationContext, SignInActivity::class.java)
                startActivityForResult(signInActivity, 0)
            }
        }
        AnodeClient.eventLog("Activity: AcccountMain created")

    }

    override fun onResume() {
        super.onResume()
        val createAccountButton: Button = findViewById(R.id.buttonCreateAccount)
        createAccountButton.setOnClickListener {
            AnodeClient.eventLog("Button: CREATE ACCOUNT pressed")
            val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
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
                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                AnodeClient.mycontext = baseContext
                val username = prefs!!.getString("username", "")
                var response = ""
                if (password.text.toString().isNotEmpty()) {
                    executor.execute {
                        if (username != null) {
                            response = registration(username,password.text.toString(),"")
                        }
                        handler.post {
                            registrationHandler(response)
                        }
                    }
                }
                executor.execute {
                    if (username != null) {
                        response = registration(username,"",email.text.toString())
                    }
                    handler.post {
                        registrationHandler(response)
                    }
                }
            }
        }
    }

    private fun registrationHandler(response: String) {
        Log.i(LOGTAG, "Received: $response")
        if ((response.isBlank())) {
            finish()
        } else if (response.contains("ERROR: ")) {
            Toast.makeText(baseContext, "Error: $response", Toast.LENGTH_SHORT).show()
        } else {
            try {
                val jsonObj = JSONObject(response)
                if (jsonObj.has("status")) {//initial password response
                    val msg = jsonObj.getString("message")
                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
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
                Toast.makeText(baseContext, response, Toast.LENGTH_SHORT).show()
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
            if (link.isNotEmpty()) {
                onLinkClick(link[0].url)
            }
            return true
        }

        abstract fun onLinkClick(url: String?)
    }

    override fun onBackPressed() {
        AnodeClient.eventLog("Button: Back pressed")
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

    private fun registration(username:String, password:String, email: String): String {
        val jsonObject = JSONObject()
        var url = ""
        if (username.isEmpty()) {
            Log.i(LOGTAG, "Error empty username")
            return ""
        }
        if (email.isNotEmpty()) {
            url = apiEmailRegistrationUrl.replace("<username>", username, false)
            jsonObject.accumulate("email", email)
        } else if (password.isNotEmpty()) {
            url = apiPasswordRegistrationUrl.replace("<username>", username, false)
            jsonObject.accumulate("password", password)
        }
        //val resp = AnodeClient.httpAuthReq(url, jsonObject.toString(), "POST")
        val resp = AnodeClient.APIHttpReq(url, jsonObject.toString(), "POST", needsAuth = true, isRetry = false)
        Log.i(LOGTAG, resp)
        return resp
    }
}