package co.anode.anodium

import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import java.net.URL

class DebugActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Debug"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        val buttonSubmitLogs = findViewById<Button>(R.id.button_SubmitLogs)
        buttonSubmitLogs.setOnClickListener {
            Toast.makeText(baseContext, "Sending log files to Anode server...", Toast.LENGTH_LONG).show()
            AnodeClient.mycontext = baseContext
            AnodeClient.storeError(baseContext, "other", Throwable("User submitted logs"))
            AnodeClient.PostLogs()
        }

        Thread(Runnable {
            Log.i(LOGTAG, "DebugActivity.RefreshValues startup")
            val anodeUtil = AnodeUtil(application)
            while (true) {
                this.runOnUiThread(Runnable {
                    val appversion = findViewById<TextView>(R.id.text_AppVersion)
                    appversion.text = "App Version: " + BuildConfig.VERSION_NAME
                    val ipv4 = findViewById<TextView>(R.id.text_ipv4)
                    ipv4.text = "IPv4: " + CjdnsSocket.ipv4Address
                    val cjdnsipv6 = findViewById<TextView>(R.id.text_cjdnsipv6)
                    val nodeinfo = CjdnsSocket.Core_nodeInfo()
                    cjdnsipv6.text = "cjdns IPv6: " + nodeinfo["myIp6"].str()
                    val internetipv6 = findViewById<TextView>(R.id.text_internetipv6)
                    internetipv6.text = "internet IPv6: " + CjdnsSocket.ipv6Address
                    val pubkey = findViewById<TextView>(R.id.text_pubkey)
                    pubkey.text = "Public Key: " + anodeUtil.getPubKey()
                    val nodeLink = findViewById<TextView>(R.id.text_nodes)
                    val link: Spanned = HtmlCompat.fromHtml("<a href='http://h.snode.cjd.li/#" + nodeinfo["myIp6"].str() + "'>Find yourself on the map</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    nodeLink.movementMethod = LinkMovementMethod.getInstance()
                    nodeLink.text = link
                    val username = findViewById<TextView>(R.id.text_username)
                    val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                    username.text = "Username: " + prefs.getString("username", "")
                    val node = findViewById<TextView>(R.id.text_node)
                    node.text = "VPN exit key: " + prefs.getString("ServerPublicKey", "")
                    val peerstats = findViewById<TextView>(R.id.text_peerstats)
                    val peers: ArrayList<Benc.Bdict> = CjdnsSocket.InterfaceController_peerStats()
                    var info = ""
                    for (i in 0 until peers.count()) {
                        info += peers[i]["lladdr"].toString() + " " + peers[i]["addr"].toString() + " " + peers[i]["state"].toString() + " in " + peers[i]["recvKbps"].toString() + " out " + peers[i]["sendKbps"].toString() + " " + peers[i]["user"].toString() + "\n"
                    }
                    peerstats.text = "Peer stats: $info"
                    val publicip = findViewById<TextView>(R.id.text_publicIP)
                    publicip.text = "Public IP: updating ip..."
                    GetPublicIP().execute(publicip)
                })
                Thread.sleep(8000)
            }
        }, "DebugActivity.RefreshValues").start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class GetPublicIP(): AsyncTask<TextView, Void, String>() {
        private var ipText:TextView? = null

        override fun doInBackground(vararg params: TextView?): String {
            ipText = params[0]
            return try {
                URL("https://api.ipify.org").readText(Charsets.UTF_8)
            } catch (e: Exception) {
                "error in getting public ip"
            }
        }
        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            ipText?.post(Runnable { ipText?.text = "Public IP: $result" })
        }
    }
}

class DebugException(message: String): Exception(message)