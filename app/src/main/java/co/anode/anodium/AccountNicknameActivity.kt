@file:Suppress("DEPRECATION")

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
import java.lang.ref.WeakReference


class AccountNicknameActivity : AppCompatActivity() {
    private val apiVersion = "0.3"
    private var apiUsernameRegistrationURL = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/"
    private var apiUsernameGenerate = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/username/"
    var username = ""
    var usernameText: EditText? = null

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
            UsernameGenerate().execute()
        }

        val continueButton: Button = findViewById(R.id.button_continue)
        continueButton.setOnClickListener {
            AnodeClient.eventLog("Button: Continue pressed")
            username = usernameText?.text.toString()
            if (username.isEmpty()) {
                Toast.makeText(baseContext, "Please enter or generate a username", Toast.LENGTH_SHORT).show()
            } else {
                UsernameRegistration(this).execute(username)
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
            UsernameGenerate().execute()
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

    private inner class UsernameGenerate : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg params: String?): String {
            val resp = AnodeClient.APIHttpReq(apiUsernameGenerate, "", "GET", needsAuth = true, isRetry = false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $apiUsernameGenerate: $result")
            if ((result.isNullOrBlank())) {
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
    }

    private class UsernameRegistration(context: AccountNicknameActivity): AsyncTask<String, Void, String>() {
        private val activityReference: WeakReference<AccountNicknameActivity> = WeakReference(context)

        override fun doInBackground(vararg params: String?): String {
            val jsonObject = JSONObject()
            jsonObject.accumulate("username", params[0])
            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return ""
            val resp = AnodeClient.APIHttpReq(activity.apiUsernameRegistrationURL, jsonObject.toString(), "POST", needsAuth = true, isRetry = false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            val activity = activityReference.get()
            if (activity == null || activity.isFinishing) return
            val apiUsernameRegistrationURL = activity.apiUsernameRegistrationURL
            Log.i(LOGTAG,"Received from $apiUsernameRegistrationURL: $result")
            if ((result.isNullOrBlank())) {
                activity.finish()
            } else if (result.contains("ERROR: ") ) {
                val json = result.split("-")[1]
                var msg = result
                try {
                    val jsonObj = JSONObject(json)
                    msg = jsonObj.getString("username")
                }catch (e: JSONException) {
                    msg += " Invalid JSON"
                }
                Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
            } else {
                val jsonObj = JSONObject(result)
                val passwordRecoveryToken = jsonObj.getString("passwordRecoveryToken")
                //Save username
                val prefs = activity.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString("username",activity.username)
                    putBoolean("SignedIn",true)
                    putBoolean("Registered",false)
                    putString("passwordRecoveryToken",passwordRecoveryToken)
                    commit()
                }
                //Start activity
                val accountMainActivity = Intent(activity, AccountMainActivity::class.java)
                activity.startActivityForResult(accountMainActivity, 0)
            }
        }
    }
}