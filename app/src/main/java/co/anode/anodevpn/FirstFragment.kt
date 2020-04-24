package co.anode.anodevpn

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.os.Handler
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_first.*


class FirstFragment : Fragment() {
    val h = Handler()

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val link: TextView = view.findViewById<TextView>(R.id.textViewLink);
        val text: Spanned = HtmlCompat.fromHtml("Open <a href='http://[fc50:71b5:aebf:7b70:6577:ec8:2542:9dd9]/'>CJDNS network</a>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        link.movementMethod = LinkMovementMethod.getInstance();
        link.text = text;
        val pubkey: TextView = view.findViewById<TextView>(R.id.textViewPubkey);
        pubkey.text = "Public key\n" + AnodeUtil().getPubKey()

        //Start a thread to update the status of the peers on the screen
        val runnable: Runnable = object : Runnable {
            var info = 0
            override fun run() {
                info = if (CjdnsSocket.ls.isConnected) {
                    CjdnsSocket.getNumberofEstablishedPeers()
                } else {
                    0
                }
                val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLog);
                logText.text = " $info active connection(s) established"
                h.postDelayed(this, 1000) //ms
            }
        }

        val switchVpn = view.findViewById<Switch>(R.id.switchVpn)

        //Listener for the Master switch
        switchVpn?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                //Inform user for action
                Toast.makeText(this.context, "Turning ON VPN", Toast.LENGTH_SHORT).show()
                //Start the VPN service
                activity!!.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
                //Enable 2nd switch
                switchInternet.isClickable = true
                //Start thread for status of peers
                h.postDelayed(runnable, 1000)
            } else {//Switch OFF
                //Inform user for action
                Toast.makeText(this.context, "Turning OFF VPN", Toast.LENGTH_SHORT).show()
                //Stop UI thread
                h.removeCallbacks(runnable)
                //Stop VPN service
                activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLog);
                logText.text = "Disconnected"
                //Disable 2nd switch
                switchInternet.isChecked = false
                switchInternet.isClickable = false
            }}

        val switchInternet = view.findViewById<Switch>(R.id.switchInternet)
        switchInternet?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLog);
                logText.text = "Connecting..."
                //Connect to Internet
                CjdnsSocket.IpTunnel_connectTo("cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
                //Get ipv4 address
                var ipv4address:String? = null
                var tries = 0
                while ((ipv4address == null) && (tries < 20)){
                    ipv4address = CjdnsSocket.getCjdnsIpv4Address()
                    tries++
                    Thread.sleep(500)
                }
                if (ipv4address != null) {
                    //Restart Service
                    activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                    activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
                } else {
                    //Stop UI thread
                    h.removeCallbacks(runnable)
                    logText.text = "Can not connect to VPN. Authorization needed."
                }
            } else {
                //TODO: ...
            }
        }

    }

    companion object {
        private const val LOGTAG = "FirstFragment"
    }
}