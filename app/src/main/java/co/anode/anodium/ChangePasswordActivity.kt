@file:Suppress("DEPRECATION")

package co.anode.anodium

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.LOGTAG
import org.json.JSONException
import org.json.JSONObject

class ChangePasswordActivity : AppCompatActivity() {
    private val apiVersion = "0.3"
    private var apiChangePasswordUrl = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/<username>/changepassword/"
    private var apiChangePasswordResetUrl = "https://vpn.anode.co/api/$apiVersion/vpn/accounts/<username>/password/change/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.action_changepassword)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        val param = intent.extras
        setResult(0)
        //Check if activity started from Forgot password flow, then hide "old password"
        val bForgotPassword = param?.getBoolean("ForgotPassword")!!
        val buttonChangePassword = findViewById<Button>(R.id.buttonChangePassword)
        val labelNewPassword = findViewById<TextView>(R.id.label_newpassword)
        val image = findViewById<ImageView>(R.id.imageViewPassword)
        labelNewPassword.visibility = View.GONE
        image.visibility = View.GONE
        val oldPassword = findViewById<EditText>(R.id.editTextOldPassword)
        if (bForgotPassword) {
            oldPassword.visibility = View.GONE
            buttonChangePassword.text = getString(R.string.action_continue)
            labelNewPassword.visibility = View.VISIBLE
            image.visibility = View.VISIBLE
        }

        buttonChangePassword.setOnClickListener {
            AnodeClient.eventLog("Button: Change password pressed")
            val newPassword = findViewById<EditText>(R.id.editTextnewPassword)
            val confirmPassword = findViewById<EditText>(R.id.editTextconfirmPassword)

            if ( (!bForgotPassword && oldPassword.text.isNullOrEmpty()) || newPassword.text.isNullOrEmpty() || confirmPassword.text.isNullOrEmpty()) {
                Toast.makeText(baseContext, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            } else if (newPassword.text.toString() != confirmPassword.text.toString()) {
                Toast.makeText(baseContext, "New password and confirm password fields do not match", Toast.LENGTH_SHORT).show()
                newPassword.text.clear()
                confirmPassword.text.clear()
            }else {
                changePassword().execute(bForgotPassword.toString(), oldPassword.text.toString(), newPassword.text.toString())
            }
        }
        AnodeClient.eventLog("Activity: Change password created")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    inner class changePassword : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String {
            val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            val jsonObject = JSONObject()
            var url = apiChangePasswordUrl.replace("<username>", prefs.getString("username", "")!!, true)
            if (params[0] == "false") {
                jsonObject.accumulate("currentPassword", params[1])
            } else {
                //jsonObject.accumulate("passwordRecoveryToken", prefs.getString("passwordResetToken",""))
                jsonObject.accumulate("passwordResetToken", prefs.getString("passwordResetToken", ""))
                url = apiChangePasswordResetUrl.replace("<username>", prefs.getString("username", "")!!, true)
            }
            jsonObject.accumulate("newPassword", params[2])
            val resp = AnodeClient.APIHttpReq(url, jsonObject.toString(), "POST", needsAuth = true, isRetry = false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG, "Received: $result")
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
                        thread.start()
                    }
                }catch (e: JSONException) {
                    Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
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