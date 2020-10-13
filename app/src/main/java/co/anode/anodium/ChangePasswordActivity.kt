package co.anode.anodium

import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import org.json.JSONException
import org.json.JSONObject

class ChangePasswordActivity : AppCompatActivity() {
    private val API_VERSION = "0.3"
    private var API_CHANGEPASSWORD_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/<username>/changepassword/"
    private var API_CHANGEPASSWORD_RESET_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/accounts/<username>/password/change/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        val param = intent.extras
        setResult(0)
        //Check if activity started from Forgot password flow, then hide "old password"
        var bForgotPassword = false
        bForgotPassword = param?.getBoolean("ForgotPassword")!!
        val buttonchangePassword = findViewById<Button>(R.id.buttonChangePassword)
        val labelnewPassword = findViewById<TextView>(R.id.label_newpassword)
        labelnewPassword.visibility = View.GONE
        if (bForgotPassword) {
            val oldpassword = findViewById<EditText>(R.id.editTextOldPassword)
            oldpassword.visibility = View.GONE
            buttonchangePassword.text = "CONTINUE"
            labelnewPassword.visibility = View.VISIBLE
        }

        buttonchangePassword.setOnClickListener() {
            AnodeClient.eventLog(baseContext,"Button: Change password pressed")
            val oldpassword = findViewById<EditText>(R.id.editTextOldPassword)
            val newpassword = findViewById<EditText>(R.id.editTextnewPassword)
            val confirmpassword = findViewById<EditText>(R.id.editTextconfirmPassword)

            if ( (!bForgotPassword && oldpassword.text.isNullOrEmpty()) || newpassword.text.isNullOrEmpty() || confirmpassword.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (newpassword.text.toString() != confirmpassword.text.toString()) {
                Toast.makeText(baseContext, "New password and confirm password fields do not match", Toast.LENGTH_SHORT).show()
                newpassword.text.clear()
                confirmpassword.text.clear()
            }else {
                changePassword().execute(bForgotPassword.toString(),oldpassword.text.toString(), newpassword.text.toString())
            }
        }
        AnodeClient.eventLog(baseContext,"Activity: Change password created")
    }

    inner class changePassword() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            val jsonObject = JSONObject()
            var url = API_CHANGEPASSWORD_URL.replace("<username>", prefs.getString("username",""), true)
            if (params[0] == "false") {
                jsonObject.accumulate("currentPassword", params[1])
            } else {
                //jsonObject.accumulate("passwordRecoveryToken", prefs.getString("passwordResetToken",""))
                jsonObject.accumulate("passwordResetToken", prefs.getString("passwordResetToken",""))
                url = API_CHANGEPASSWORD_RESET_URL.replace("<username>", prefs.getString("username",""), true)
            }
            jsonObject.accumulate("newPassword", params[2])
            val resp = AnodeClient.APIHttpReq(url, jsonObject.toString(), "POST", true, false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received: $result")
            if (result.isNullOrBlank()) {
                //unknown
                finish()
            } else if (result.contains("ERROR: ")) {
                Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    if (jsonObj.has("status") && jsonObj.getString("status") == "success") {
                        Toast.makeText(baseContext, "Password changed successfully", Toast.LENGTH_SHORT).show()
                        thread.start();
                    }
                }catch (e: JSONException) {
                    Toast.makeText(baseContext, result , Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    var thread: Thread = object : Thread() {
        override fun run() {
            try {
                sleep(Toast.LENGTH_LONG.toLong())
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}