package co.anode.anodevpn

import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

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
        val peerstats = findViewById<TextView>(R.id.text_peerstats)
        val peers:ArrayList<Benc.Bdict> = CjdnsSocket.InterfaceController_peerStats()
        var info = ""
        for (i in 0 until peers.count()) {
            info += peers[i].toString()
        }
        peerstats.text = "Peer stats: $info"
    }
}