package co.anode.anodevpn

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.apache.commons.text.RandomStringGenerator
import org.json.JSONObject


class AccountNicknameActivity : AppCompatActivity() {
    private val API_VERSION = "0.3"
    private var API_USERNAME_REGISTRATION_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/"
    var username = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accountnickname)
        val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
        val prefsusername = prefs!!.getString("username","username")
        val nicknameText: EditText = findViewById(R.id.editTextNickname)
        nicknameText.setText(prefsusername)

        val continueButton: Button = findViewById(R.id.button_continue)
        continueButton.setOnClickListener() {
            username = nicknameText.text.toString()
            if (username.isEmpty()) {
                val generator = RandomStringGenerator.Builder()
                        .withinRange('a'.toInt(), 'z'.toInt())
                        .build()
                username = generator.generate(10)
            }
            usernameRegistration().execute(username)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            this.finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    inner class usernameRegistration() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val jsonObject = JSONObject()
            jsonObject.accumulate("username", params[0])
            val resp = AnodeClient.httpAuthReq(API_USERNAME_REGISTRATION_URL, jsonObject.toString(), "POST")
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_USERNAME_REGISTRATION_URL: $result")
            if ((result.isNullOrBlank()) || ((result == "Internal Server Error"))) {
                finish()
            } else if (result.contains("400") || result.contains("401")) {
                Toast.makeText(baseContext, "Error: $result", Toast.LENGTH_SHORT).show()
            } else {
                val jsonObj = JSONObject(result)
                val passwordRecoveryToken = jsonObj.getString("passwordRecoveryToken")
                //Save username
                val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString("username",username)
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