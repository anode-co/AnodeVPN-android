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
import java.io.IOException


class AccountNicknameActivity : AppCompatActivity() {
    private val API_VERSION = "0.3"
    private var API_USERNAME_REGISTRATION_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/"
    private var API_USERNAME_GENERATE = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/username/"
    var username = ""
    var usernameText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accountnickname)

        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)

        val signin: TextView = findViewById(R.id.textSignIn)
        val link: Spanned = HtmlCompat.fromHtml("already have an account? <a href='#'>Sign in</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        signin.movementMethod = LinkMovementMethod.getInstance()
        signin.text = link
        signin.setMovementMethod(object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                AnodeClient.eventLog(baseContext,"Button: Sing in link pressed")
                val signInActivity = Intent(applicationContext, SignInActivity::class.java)
                startActivityForResult(signInActivity, 0)
            }
        })

        val prefsusername = prefs!!.getString("username","")
        usernameText = findViewById(R.id.editTextNickname)
        if (prefsusername.isEmpty()) {
            usernameGenerate().execute()
        } else {
            usernameText?.setText(prefsusername)
        }

        val generateusername: Button = findViewById(R.id.button_generateusername)
        generateusername.setOnClickListener() {
            AnodeClient.eventLog(baseContext,"Button: Generate username pressed")
            usernameGenerate().execute()
        }

        val continueButton: Button = findViewById(R.id.button_continue)
        continueButton.setOnClickListener() {
            AnodeClient.eventLog(baseContext,"Button: Continue pressed")
            username = usernameText?.text.toString()
            if (username.isEmpty()) {
                Toast.makeText(baseContext, "Please enter or generate a username", Toast.LENGTH_SHORT).show()
            } else {
                usernameRegistration().execute(username)
            }
        }
        AnodeClient.eventLog(baseContext,"Activity: Nickname created")
    }

    override fun onBackPressed() {
        AnodeClient.eventLog(baseContext,"Button: Back pressed")
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
            if (link.size != 0) {
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

    inner class usernameGenerate() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val resp = AnodeClient.APIHttpReq(API_USERNAME_GENERATE, "", "GET", true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_USERNAME_GENERATE: $result")
            if ((result.isNullOrBlank())) {
                finish()
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
                    usernameText?.post(Runnable { usernameText?.setText(jsonObj.getString("username")) })
                }
            }
        }
    }

    inner class usernameRegistration() : AsyncTask<String, Void, String>() {
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
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            } else {
                val jsonObj = JSONObject(result)
                val passwordRecoveryToken = jsonObj.getString("passwordRecoveryToken")
                //Save username
                val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString("username",username)
                    putBoolean("SignedIn",true)
                    putBoolean("Registered",false)
                    putString("passwordRecoveryToken",passwordRecoveryToken)
                    commit()
                }
                //Start activity
                val accountMainActivity = Intent(applicationContext, AccountMainActivity::class.java)
                startActivityForResult(accountMainActivity, 0)
            }
        }
    }
}