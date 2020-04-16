package com.anode.anode

import android.content.Intent
import android.os.Bundle
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
import kotlinx.android.synthetic.*


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
            } else {
                //TODO turn off VPN
            }}

        //Thread.sleep(3000)
        updateStats()
    }

    fun updateStats() {
        //Update peerstats every second
        val thread = object: Thread(){
            override fun run(){
                val logText: TextView = view!!.findViewById<TextView>(R.id.textViewLabelLog);
                var cjdns = CjdnsSocket(AnodeUtil().CJDNS_PATH + "/" + AnodeUtil().CJDROUTE_SOCK)
                val info = cjdns!!.InterfaceController_peerStats(0)
                logText.text = info.toString()
                sleep(1000)
            }
        }

        thread.start()
    }

    companion object {
        private const val LOGTAG = "FirstFragment"
    }
}