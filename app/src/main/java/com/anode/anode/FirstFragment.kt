package com.anode.anode

import android.content.Intent
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
        val runnable = null
        val switchVpn = view.findViewById<Switch>(R.id.switchVpn)
        switchVpn?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this.context, "Turning ON VPN", Toast.LENGTH_SHORT).show()
                activity!!.startService(Intent(activity, AnodeVpnService::class.java))
                val runnable: Runnable = object : Runnable {
                    var count = 0
                    override fun run() {
                        var cjdns = CjdnsSocket(AnodeUtil().CJDNS_PATH + "/" + AnodeUtil().CJDROUTE_SOCK)
                        val info = cjdns!!.getNumberofEstablishedPeers()
                        val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLog);
                        logText.text = " $info active connection(s) established"
                        h.postDelayed(this, 1000) //ms
                    }
                }
                h.postDelayed(runnable, 1000) // one second in ms
            } else {
                Toast.makeText(this.context, "Turning OFF VPN", Toast.LENGTH_SHORT).show()
                h.removeCallbacksAndMessages(runnable)
                //var cjdns = CjdnsSocket(AnodeUtil().CJDNS_PATH + "/" + AnodeUtil().CJDROUTE_SOCK)
                //cjdns!!.Core_stopTun()
                activity!!.stopService(Intent(activity, AnodeVpnService::class.java))
                val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLog);
                logText.text = "Disconnected..."
            }}
    }

    companion object {
        private const val LOGTAG = "FirstFragment"
    }
}