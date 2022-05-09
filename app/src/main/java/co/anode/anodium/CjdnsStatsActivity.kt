@file:Suppress("DEPRECATION")

package co.anode.anodium

import android.annotation.SuppressLint
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
import co.anode.anodium.support.*
import java.net.URL

class CjdnsStatsActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cjdnsstats)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "View Logs"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)

        val buttonSubmitLogs = findViewById<Button>(R.id.button_SubmitLogs)
        buttonSubmitLogs.setOnClickListener {

            Toast.makeText(baseContext, "Sending log files to Anode server...", Toast.LENGTH_LONG).show()
            AnodeClient.mycontext = baseContext
            AnodeClient.storeError(baseContext, "other", Throwable("User submitted logs"))
            AnodeClient.PostLogs()
        }
        val buttonDeleteAccount = findViewById<Button>(R.id.button_Deleteaccount)
        buttonDeleteAccount.setOnClickListener {
            Log.i(LOGTAG, "Delete account")
            AnodeClient.eventLog("Delete account selected")
            AnodeClient.DeleteAccount().execute()
        }
        val text = findViewById<TextView>(R.id.text_errorfiles)
        val files = filesDir.listFiles { file -> file.name.startsWith("error-uploadme-") }
        if (files.isEmpty()) {
            text.text = "0"
        } else {
            text.text = files.size.toString()
        }
        Thread({
            Log.i(LOGTAG, "DebugActivity.RefreshValues startup")
            while (true) {
                this.runOnUiThread {
                    val appversion = findViewById<TextView>(R.id.text_AppVersion)
                    appversion.text = "App Version: " + BuildConfig.VERSION_NAME
                    val ipv4 = findViewById<TextView>(R.id.text_ipv4)
                    ipv4.text = CjdnsSocket.ipv4Address
                    val cjdnsipv6 = findViewById<TextView>(R.id.text_cjdnsipv6)
                    val nodeinfo = CjdnsSocket.Core_nodeInfo()
                    cjdnsipv6.text = nodeinfo["myIp6"].str()
                    val internetipv6 = findViewById<TextView>(R.id.text_internetipv6)
                    internetipv6.text = CjdnsSocket.ipv6Address
                    val pubkey = findViewById<TextView>(R.id.text_pubkey)
                    pubkey.text = AnodeUtil.getPubKey()
                    val nodeLink = findViewById<TextView>(R.id.text_nodes)
                    val link: Spanned = HtmlCompat.fromHtml("<a href='http://h.snode.cjd.li/#" + nodeinfo["myIp6"].str() + "'>Find yourself on the map</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    nodeLink.movementMethod = LinkMovementMethod.getInstance()
                    nodeLink.text = link
                    val username = findViewById<TextView>(R.id.text_username)
                    username.text = prefs.getString("username", "")
                    val node = findViewById<TextView>(R.id.text_node)
                    node.text = prefs.getString("ServerPublicKey", "")
                    val peerstats = findViewById<TextView>(R.id.text_peerstats)
                    val peers: ArrayList<Benc.Bdict> = CjdnsSocket.InterfaceController_peerStats()
                    var info = ""
                    for (i in 0 until peers.count()) {
                        info += peers[i]["lladdr"].toString() + " " + peers[i]["addr"].toString() + " " + peers[i]["state"].toString() + " in " + peers[i]["recvKbps"].toString() + " out " + peers[i]["sendKbps"].toString() + " " + peers[i]["user"].toString() + "\n"
                    }
                    peerstats.text = info

                    val listconnnections = findViewById<TextView>(R.id.text_listconnections)
                    val connections: ArrayList<Benc.Bdict> = CjdnsSocket.IpTunnel_listConnections()
                    info = ""
                    for (i in 0 until connections.count()) {
                        info += connections[i].toString()
                    }
                    listconnnections.text = info
                    val publicip = findViewById<TextView>(R.id.text_publicIP)
                    publicip.text = "updating ip..."
                    GetPublicIP().execute(publicip)
                }
                Thread.sleep(8000)
            }
        }, "DebugActivity.RefreshValues").start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class GetPublicIP : AsyncTask<TextView, Void, String>() {
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
            ipText?.post { ipText?.text = result }
        }
    }
}