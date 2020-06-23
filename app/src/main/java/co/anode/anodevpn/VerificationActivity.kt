package co.anode.anodevpn

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
        //TODO: Make async Task
        statuschecker.init(h, url!!, applicationContext)
        h.postDelayed(statuschecker, 1000)
    }
}

class checkstatusURL(context: Context?, handler: Handler?) : AsyncTask<String, Void, String>() {
    private var c: Context? = null
    private var h: Handler? = null

    init {
        c = context
        h = handler
    }

    override fun doInBackground(vararg params: String?): String? {
        val result = AnodeClient.httpAuthReq(params[0]!!, "", "GET")
        return result
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        Log.i(LOGTAG,"Received: $result")
        if (result.isNullOrEmpty()) {
            h?.postDelayed(statuschecker, 10000)
            return
        } else if (result == "202") {
            val signInActivity = Intent(c, SignInActivity::class.java)
            signInActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
            c?.startActivity(signInActivity)
        }
        val jsonObj = JSONObject(result)
        if (jsonObj.has("status")) {
            val status = jsonObj.getString("status")
            if (status == "pending") {
                h?.postDelayed(statuschecker, 10000)
                return
            } else if (status == "complete") {
                h?.removeCallbacks(statuschecker)
                val backupWalletPassword = jsonObj.getString("backupWalletPassword")
                val prefs = c?.getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
                with(prefs!!.edit()) {
                    putString("backupWalletPassword", backupWalletPassword)
                    commit()
                }
                val signInActivity = Intent(c, SignInActivity::class.java)
                signInActivity.flags = Intent.FLAG_ACTIVITY_NEW_TASK;
                c?.startActivity(signInActivity)
            }
        }
    }
}

object statuschecker: Runnable {
    private const val PollingInterval: Long = 10000
    private var h: Handler? = null
    private var url: String? = null
    private var context: Context? = null

    fun init(handler: Handler, u: String, c: Context)  {
        h = handler
        url = u
        context = c
    }

    @SuppressLint("SetTextI18n")
    override fun run() {
        try {
           checkstatusURL(context,h).execute(url)
        } catch (e: Exception) {
            Log.i(LOGTAG,"error in getting confirmation result")
            h!!.postDelayed(this, PollingInterval)
        }
    }
}

