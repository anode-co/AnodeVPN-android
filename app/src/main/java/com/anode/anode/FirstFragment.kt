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
    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Set link on UI
        val link: TextView = view.findViewById<TextView>(R.id.textViewLink);
        val text: Spanned = HtmlCompat.fromHtml("Open <a href='http://[fc50:71b5:aebf:7b70:6577:ec8:2542:9dd9]/'>CJDNS network</a>", HtmlCompat.FROM_HTML_MODE_LEGACY);
        link.movementMethod = LinkMovementMethod.getInstance();
        link.text = text;

        val switchVpn = view.findViewById<Switch>(R.id.switchVpn)
        switchVpn?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this.context, "Turning on VPN", Toast.LENGTH_SHORT).show()
                activity!!.startService(Intent(activity, AnodeVpnService::class.java))
                updateStats()
            } else {
                //TODO turn off VPN
            }}

        //Thread.sleep(3000)
        //updateStats()
    }

    fun updateStats() {
        val h = Handler()
        val r: Runnable = object : Runnable {
            var count = 0
            override fun run() {
                var cjdns = CjdnsSocket(AnodeUtil().CJDNS_PATH + "/" + AnodeUtil().CJDROUTE_SOCK)
                val info = cjdns!!.InterfaceController_peerStats(0)
                val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLog);
                logText.text = "active $info connections established"
                h.postDelayed(this, 1000) //ms
            }
        }
        h.postDelayed(r, 1000) // one second in ms

        /*
        //var cjdns = CjdnsSocket(AnodeUtil().CJDNS_PATH + "/" + AnodeUtil().CJDROUTE_SOCK)
        activity!!.runOnUiThread {
            //Toast.makeText(activity, "any mesage", Toast.LENGTH_LONG).show()
            val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLabelLog);
            val info = cjdns!!.InterfaceController_peerStats(0)
            logText.text = "active $info connections established"
        }

         */
    }

    companion object {
        private const val LOGTAG = "FirstFragment"
    }
}