package co.anode.anodevpn

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import java.net.URL

class DebugActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)

        val anodeUtil = AnodeUtil(application)
        val appversion = findViewById<TextView>(R.id.text_AppVersion)
        appversion.text = "App Version: "+BuildConfig.VERSION_NAME
        val ipv4 = findViewById<TextView>(R.id.text_ipv4)
        ipv4.text = "IPv4: "+CjdnsSocket.ipv4Address
        val ipv6 = findViewById<TextView>(R.id.text_ipv6)
        ipv6.text = "IPv6: "+CjdnsSocket.ipv6Address
        val pubkey = findViewById<TextView>(R.id.text_pubkey)
        pubkey.text = "Public Key: "+anodeUtil.getPubKey()
        val username = findViewById<TextView>(R.id.text_username)
        val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
        username.text = "Username: "+prefs.getString("username","")
        val node = findViewById<TextView>(R.id.text_node)
        node.text = "Last Node: "+prefs.getString("ServerPublicKey","")
        val peerstats = findViewById<TextView>(R.id.text_peerstats)
        val peers:ArrayList<Benc.Bdict> = CjdnsSocket.InterfaceController_peerStats()
        var info = ""
        for (i in 0 until peers.count()) {
            info += peers[i].toString()
        }
        peerstats.text = "Peer stats: $info"
        val publicip = findViewById<TextView>(R.id.text_publicIP)
        publicip.text = "Public IP: Retrieving ip..."
        GetPublicIP().execute(publicip)
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
            ipText?.post(Runnable { ipText?.text  = "Public IP: $result" } )
        }
    }
}