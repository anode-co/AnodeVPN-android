package co.anode.anodevpn

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
import org.json.JSONObject

class SignInActivity : AppCompatActivity() {
    private val API_VERSION = "0.3"
    private var API_SIGNIN_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/authorize/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        val signup: TextView = findViewById(R.id.textSignUp)
        val link: Spanned = HtmlCompat.fromHtml("don't have an account yet? <a href='#'>Sign Up</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        signup.movementMethod = LinkMovementMethod.getInstance()
        signup.text = link

        val signUpLink = findViewById<TextView>(R.id.textSignUp)
        signUpLink.setMovementMethod(object : TextViewLinkHandler() {
            override fun onLinkClick(url: String?) {
                val nicknameActivity = Intent(applicationContext, AccountNicknameActivity::class.java)
                startActivityForResult(nicknameActivity, 0)
            }
        })

        val buttonSignin = findViewById<Button>(R.id.buttonSingIn)
        buttonSignin.setOnClickListener() {
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

    inner class signIn() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val jsonObject = JSONObject()
            jsonObject.accumulate("emailOrUsername", params[0])
            jsonObject.accumulate("password", params[1])
            val resp = AnodeClient.APIHttpReq(API_SIGNIN_URL, jsonObject.toString(), "POST", true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received: $result")
            if (result.isNullOrBlank()) {
                Toast.makeText(baseContext, "User signed in successfully", Toast.LENGTH_SHORT).show()
                val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putBoolean("SignedIn", true)
                    commit()
                }
                thread.start();
            }
            else if (result.contains("Internal Server Error")) {
                finish()
            } else if (result.contains("400") || result.contains("401")) {
                Toast.makeText(baseContext, "Error: $result", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var thread: Thread = object : Thread() {
        override fun run() {
            try {
                sleep(Toast.LENGTH_LONG.toLong()) // As I am using LENGTH_LONG in Toast
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}