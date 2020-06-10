package co.anode.anodevpn

import android.annotation.SuppressLint
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
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection


class AccountMainActivity : AppCompatActivity() {
    private val API_VERSION = "0.3"
    private val API_REGISTRATION_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/"
    private var anodeUtil: AnodeUtil? = null

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_main)
        val signin: TextView = findViewById(R.id.textSignIn)
        val link: Spanned = HtmlCompat.fromHtml("already have an account? <a href='#'>Sign in</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        signin.movementMethod = LinkMovementMethod.getInstance()
        signin.text = link
        val createAccountButton: Button = findViewById(R.id.buttonCreateAccount)
        createAccountButton.setOnClickListener() {

            val emailPattern: String = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
            val email = findViewById<EditText>(R.id.editTextTextEmailAddress)
            val password = findViewById<EditText>(R.id.editTextTextPassword)
            //Check email and passwords fields
            if (email.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in email field", Toast.LENGTH_SHORT).show()
            }
            else if (password.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in password field", Toast.LENGTH_SHORT).show()
            }
            else if (!email.text.toString().trim().matches(emailPattern.toRegex())) {
                Toast.makeText(baseContext, "Email is not valid", Toast.LENGTH_SHORT).show()
            }
            else {
                AnodeClient.mycontext = baseContext
                emailRegistration().execute(email.text.toString())
            }
        }
        val skipButton: Button = findViewById(R.id.buttonSkip)
        skipButton.setOnClickListener() {
            setResult(0)
            finish()
        }

        val signinLink = findViewById<TextView>(R.id.textSignIn)
        signinLink.setMovementMethod(object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                val signInActivity = Intent(applicationContext, SignInActivity::class.java)
                startActivityForResult(signInActivity, 0)
            }
        })
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

    inner class emailRegistration() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            return try {
                val url = URL(API_REGISTRATION_URL)
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val jsonObject = JSONObject()
                jsonObject.accumulate("email", params[0])
                AnodeClient.setPostRequestContent(conn, jsonObject)
                conn.connect()
                return conn.responseMessage
            } catch (e: Exception) {
                Log.i(LOGTAG,"Failed to get publick key ID from API $e")
                null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_REGISTRATION_URL: $result")
            if (result.isNullOrBlank()) {
                finish()
            } else if (result == "Created") {
                val verificationActivity = Intent(applicationContext, VerificationActivity::class.java)
                startActivityForResult(verificationActivity, 0)
            } else if (result == "Internal Server Error") {
                Toast.makeText(baseContext, "Error registering: $result", Toast.LENGTH_SHORT).show()
                Thread.sleep(1000)
                finish()
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    val msg = jsonObj.getString("email")
                    if (!msg.isNullOrEmpty()) {
                        Toast.makeText(baseContext, "Error registering: $result", Toast.LENGTH_SHORT).show()
                        Thread.sleep(1000)
                        finish()
                    }
                } catch (e: Exception) {
                  Log.i(LOGTAG,"Error: $e")
                }
            }
        }
    }
}