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
import org.json.JSONObject


class AccountNicknameActivity : AppCompatActivity() {
    private val API_VERSION = "0.3"
    private var API_USERNAME_REGISTRATION_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/"
    private var API_USERNAME_GENERATE = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/username/"
    var username = ""
    var usernameText: EditText? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accountnickname)
        val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
        val prefsusername = prefs!!.getString("username","")
        usernameText = findViewById(R.id.editTextNickname)
        if (prefsusername.isEmpty()) {
            usernameGenerate().execute()
        } else {
            usernameText?.setText(prefsusername)
        }

        val generateusername: Button = findViewById(R.id.button_generateusername)
        generateusername.setOnClickListener() {
            usernameGenerate().execute()
        }

        val continueButton: Button = findViewById(R.id.button_continue)
        continueButton.setOnClickListener() {
            username = usernameText?.text.toString()
            if (username.isEmpty()) {
                Toast.makeText(baseContext, "Please enter or generate a username", Toast.LENGTH_SHORT).show()
            } else {
                usernameRegistration().execute(username)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0) {
            this.finish()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    inner class usernameGenerate() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val resp = AnodeClient.APIHttpReq(API_USERNAME_GENERATE,"", "GET",true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_USERNAME_GENERATE: $result")
            if ((result.isNullOrBlank()) || ((result == "Internal Server Error"))) {
                finish()
            } else if (result.contains("400") || result.contains("401")) {
                val json = result.split("|")[1]
                val jsonObj = JSONObject(json)
                val msg = jsonObj.getString("username")
                Toast.makeText(baseContext, "Error: $msg", Toast.LENGTH_SHORT).show()
            } else if (result.contains("403")) {
                Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
            } else {
                //{"username":"flattop-fence"}
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
            if ((result.isNullOrBlank()) || ((result == "Internal Server Error"))) {
                finish()
            } else if (result.contains("400") || result.contains("401")) {
                val json = result.split("|")[1]
                val jsonObj = JSONObject(json)
                val msg = jsonObj.getString("username")
                Toast.makeText(baseContext, "Error: $msg", Toast.LENGTH_SHORT).show()
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