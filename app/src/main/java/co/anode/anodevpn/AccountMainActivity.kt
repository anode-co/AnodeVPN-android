package co.anode.anodevpn

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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