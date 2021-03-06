package co.anode.anodium

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONException
import org.json.JSONObject

class VerificationActivity : AppCompatActivity() {
    val h = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verification)
        Log.i(LOGTAG, "Started VerificationActivity")
        val msgText = findViewById<TextView>(R.id.textView)
        msgText.text = "We 've sent a verification link to your email.\nPlease check your email."
        setResult(0)
        val param = intent.extras
        val url = param?.getString("accountConfirmationStatusUrl")

        statuschecker.init(h, url!!, applicationContext, this)
        h.postDelayed(statuschecker, 1000)
    }
}

class checkstatusURL(context: Context?, handler: Handler?, activity: AppCompatActivity?) : AsyncTask<String, Void, String>() {
    private var c: Context? = null
    private var h: Handler? = null
    private var a: AppCompatActivity? = null
    init {
        c = context
        h = handler
        a = activity
    }

    override fun doInBackground(vararg params: String?): String? {
        return AnodeClient.APIHttpReq(params[0]!!, "", "GET", true, false)
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Log.i(LOGTAG,"Received: $result")
        if (result.isNullOrEmpty() || (result.contains("ERROR: "))) {
            h?.postDelayed(statuschecker, 10000)
            return
        } else if (result == "202") {
            val signInActivity = Intent(c, SignInActivity::class.java)
            signInActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            c?.startActivity(signInActivity)
            a?.finish()
        } else {
            try {
                val jsonObj = JSONObject(result)
                if (jsonObj.has("status")) {
                    val status = jsonObj.getString("status")
                    if (status == "pending") {
                        h?.postDelayed(statuschecker, 10000)
                        return
                    } else if (status == "complete") {
                        h?.removeCallbacks(statuschecker)
                        val backupWalletPassword = jsonObj.getString("backupWalletPassword")
                        val prefs = c?.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                        with(prefs!!.edit()) {
                            putString("backupWalletPassword", backupWalletPassword)
                            commit()
                        }
                        val signInActivity = Intent(c, SignInActivity::class.java)
                        signInActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
                        c?.startActivity(signInActivity)
                        a?.finish()
                    }
                }
            } catch (e: JSONException) {
                h?.postDelayed(statuschecker, 10000)
                return
            }
        }
    }
}

@SuppressLint("StaticFieldLeak")
object statuschecker: Runnable {
    private const val PollingInterval: Long = 10000
    private var h: Handler? = null
    private var url: String? = null
    private var context: Context? = null
    private var activity: AppCompatActivity? = null

    fun init(handler: Handler, u: String, c: Context, a: AppCompatActivity)  {
        h = handler
        url = u
        context = c
        activity = a
    }

    @SuppressLint("SetTextI18n")
    override fun run() {
        try {
           checkstatusURL(context, h, activity).execute(url)
        } catch (e: Exception) {
            Log.i(LOGTAG,"error in getting confirmation result")
            h!!.postDelayed(this, PollingInterval)
        }
    }
}

