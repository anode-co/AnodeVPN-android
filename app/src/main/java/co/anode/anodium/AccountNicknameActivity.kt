package co.anode.anodium

import android.content.Context
import android.content.Intent
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


class AccountNicknameActivity : AppCompatActivity() {
    private val apiVersion = "0.3"
    private var apiUsernameRegistrationURL = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/"
    private var apiUsernameGenerate = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/username/"
    private var username = ""
    private var usernameText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accountnickname)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Sign up"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        val signIn: TextView = findViewById(R.id.textSignIn)
        val link: Spanned = HtmlCompat.fromHtml("already have an account? <a href='#'>Sign in</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        signIn.movementMethod = LinkMovementMethod.getInstance()
        signIn.text = link
        signIn.movementMethod = object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                AnodeClient.eventLog("Button: Sing in link pressed")
                val signInActivity = Intent(applicationContext, SignInActivity::class.java)
                startActivityForResult(signInActivity, 0)
            }
        }

        val generateUsername: Button = findViewById(R.id.button_generateusername)
        generateUsername.setOnClickListener {
            AnodeClient.eventLog("Button: Generate username pressed")
            getUsername()
        }

        val continueButton: Button = findViewById(R.id.button_continue)
        continueButton.setOnClickListener {
            AnodeClient.eventLog("Button: Continue pressed")
            username = usernameText?.text.toString()
            if (username.isEmpty()) {
                Toast.makeText(baseContext, "Please enter or generate a username", Toast.LENGTH_SHORT).show()
            } else {
                val executor = Executors.newSingleThreadExecutor()
                val handler = Handler(Looper.getMainLooper())
                var registrationResponse: String
                executor.execute {
                    registrationResponse = usernameRegistration(username)
                    handler.post {
                        usernameRegistrationHandler(registrationResponse)
                    }
                }
            }
        }
        AnodeClient.eventLog("Activity: Nickname created")
    }

    override fun onStart() {
        super.onStart()
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val prefsUsername = prefs.getString("username","")
        usernameText = findViewById(R.id.editTextNickname)
        if (prefsUsername!!.isEmpty()) {
            getUsername()
        } else {
            usernameText?.setText(prefsUsername)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onBackPressed() {
        AnodeClient.eventLog("Button: Back pressed")
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        with (prefs.edit()) {
            putBoolean("NicknameActivity_BackPressed",true)
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
            if (link.isNotEmpty()) {
                onLinkClick(link[0].url)
            }
            return true
        }

        abstract fun onLinkClick(url: String?)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            this.finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun generateUsername():String {
        val resp = AnodeClient.APIHttpReq(apiUsernameGenerate, "", "GET", needsAuth = true, isRetry = false)
        Log.i(LOGTAG, resp)
        return resp
    }

    private fun generateUsernameHandler(result: String) {
        Log.i(LOGTAG,"Received from $apiUsernameGenerate: $result")
        if ((result.isBlank())) {
            return
        } else if (result.contains("400") || result.contains("401")) {
            val json = result.split("-")[1]
            var msg = result
            try {
                val jsonObj = JSONObject(json)
                msg = jsonObj.getString("username")
            } catch (e: JSONException) {
                msg += " Invalid JSON"
            }
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        } else if (result.contains("ERROR: ")) {
            Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
        } else {
            val jsonObj = JSONObject(result)
            if (jsonObj.has("username")) {
                usernameText?.post { usernameText?.setText(jsonObj.getString("username")) }
            }
        }
    }

    private fun usernameRegistration(username: String):String {
        val jsonObject = JSONObject()
        jsonObject.accumulate("username", username)
        val resp = AnodeClient.APIHttpReq(apiUsernameRegistrationURL, jsonObject.toString(), "POST", needsAuth = true, isRetry = false)
        Log.i(LOGTAG, resp)
        return resp
    }

    private fun usernameRegistrationHandler(result: String) {
        Log.i(LOGTAG,"Received from $apiUsernameRegistrationURL: $result")
        if ((result.isBlank())) {
            finish()
        } else if (result.contains("ERROR: ") ) {
            val json = result.split("-")[1]
            var msg = result
            try {
                val jsonObj = JSONObject(json)
                msg = jsonObj.getString("username")
            }catch (e: JSONException) {
                msg += " Invalid JSON"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        } else {
            val jsonObj = JSONObject(result)
            val passwordRecoveryToken = jsonObj.getString("passwordRecoveryToken")
            //Save username
            val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            with (prefs.edit()) {
                putString("username", username)
                putBoolean("SignedIn", true)
                putBoolean("Registered", false)
                putString("passwordRecoveryToken", passwordRecoveryToken)
                commit()
            }
            //Start activity
            val accountMainActivity = Intent(applicationContext, AccountMainActivity::class.java)
            startActivityForResult(accountMainActivity, 0)
        }
    }

    private fun getUsername() {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var usernameResponse: String
        executor.execute {
            usernameResponse = generateUsername()
            handler.post {
                generateUsernameHandler(usernameResponse)
            }
        }
    }
}