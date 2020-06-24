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
        val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
        val anodeUtil = AnodeUtil(application)
        val appversion = findViewById<TextView>(R.id.textAppVersion)
        appversion.text = "App Version: "+BuildConfig.VERSION_NAME
        val ipv4 = findViewById<TextView>(R.id.textIpv4)
        ipv4.text = "IPv4: "+CjdnsSocket.ipv4Address
        val ipv6 = findViewById<TextView>(R.id.textIpv6)
        ipv6.text = "IPv6: "+CjdnsSocket.ipv6Address
        val pubkey = findViewById<TextView>(R.id.textPubkey)
        pubkey.text = "Public Key: "+anodeUtil.getPubKey()
        val connections = findViewById<TextView>(R.id.lableConnections)
        val peers:ArrayList<Benc.Bdict> = CjdnsSocket.InterfaceController_peerStats()
        var peersinfo = ""
        for (i in 0 until peers.count()) {
            peersinfo += peers[i].toString()
        }
        connections.text = "CJDNS connections: $peersinfo"
    }
}