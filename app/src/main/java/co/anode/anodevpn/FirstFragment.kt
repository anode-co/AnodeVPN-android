package co.anode.anodevpn

import android.annotation.SuppressLint
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import kotlinx.android.synthetic.main.fragment_first.*
import java.net.URL
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class FirstFragment : Fragment() {
    val h = Handler()
    var ipv4address:String? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val link: TextView = view.findViewById(R.id.textViewLink)
        val text: Spanned = HtmlCompat.fromHtml("Open <a href='http://[fc50:71b5:aebf:7b70:6577:ec8:2542:9dd9]/'>CJDNS network</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        link.movementMethod = LinkMovementMethod.getInstance()
        link.text = text
        val pubkey: TextView = view.findViewById(R.id.textViewPubkey)
        pubkey.text = "Public key\n" + AnodeUtil(null).getPubKey()
        //Initialize runnable threads
        runnableUI.init(view,h)
        runnableConnection.init(view,h,activity,ipv4address)

        val switchVpn = view.findViewById<Switch>(R.id.switchVpn)
        //Listener for the Master switch
        switchVpn?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.i(LOGTAG,"Main Switch checked")
                //Inform user for action
                Toast.makeText(this.context, "Turning ON VPN", Toast.LENGTH_SHORT).show()
                //Start the VPN service
                requireActivity().startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
                //Enable 2nd switch
                switchInternet.isClickable = true
                //Start thread for status of peers
                h.postDelayed(runnableUI, 1000)
            } else {//Switch OFF
                Log.i(LOGTAG,"Main Switch unchecked")
                //Inform user for action
                Toast.makeText(this.context, "Turning OFF VPN", Toast.LENGTH_SHORT).show()
                //Stop UI thread
                h.removeCallbacks(runnableUI)
                h.removeCallbacks(runnableConnection)
                //Stop VPN service
                activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                val logText: TextView = view.findViewById(R.id.textViewLog)
                logText.text = "Disconnected"
                //Disable 2nd switch
                switchInternet.isChecked = false
                switchInternet.isClickable = false
            }}

        val switchInternet = view.findViewById<Switch>(R.id.switchInternet)
        switchInternet?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Log.i(LOGTAG,"Internet Switch checked")
                val logText: TextView = view.findViewById(R.id.textViewLog)
                logText.text = "Connecting..."
                Toast.makeText(this.context, "Turning ON Internet VPN", Toast.LENGTH_SHORT).show()
                //Start connecting thread
                val executor: ExecutorService = Executors.newSingleThreadExecutor()
                ConnectingThread.init(view,h,activity)
                executor.submit(ConnectingThread)
            } else {
                Log.i(LOGTAG,"Internet Switch unchecked")
                Toast.makeText(this.context, "Turning OFF Internet VPN", Toast.LENGTH_SHORT).show()
                val ipText: TextView = view.findViewById(R.id.textViewPublicIP)
                ipText.text = "Disconnected..."
                //Clear routes
                CjdnsSocket.clearRoutes()
                //Restart VPN Service
                activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
                //Start UI thread
                h.postDelayed(runnableUI, 1000)
            }
        }

    }

    companion object {
        private const val LOGTAG = "FirstFragment"
    }
}


class GetPublicIP(private val ipText: TextView) : AsyncTask<Any?, Any?, String>() {

    override fun doInBackground(objects: Array<Any?>): String {
        var ip = ""
        try {
            // Create a URL for the desired page
            ip = URL("https://api.ipify.org/").readText(Charsets.UTF_8)
        } catch (e: Exception) {

        }
        return ip
    }

    override fun onPostExecute(result: String?) {
        super.onPostExecute(result)
        ipText.post(Runnable { ipText.text = "Connected Public IP: $result"} )
    }
}

object runnableUI: Runnable {
    private var v: View? = null
    private var h:Handler? = null

    fun init(view: View, handler: Handler)  {
        v = view
        h = handler
    }

    var info = 0
    override fun run() {
        info = if (CjdnsSocket.ls.isConnected) {
            CjdnsSocket.getNumberofEstablishedPeers()
        } else {
            0
        }
        val logText: TextView = v!!.findViewById(R.id.textViewLog)
        logText.text = " $info active connection(s) established"
        h!!.postDelayed(this, 1000) //ms
    }
}

object runnableConnection: Runnable {
    private var v: View? = null
    private var h:Handler? = null
    private var a: FragmentActivity? = null
    private var ipv4address: String? = null

    fun init(view: View, handler: Handler, activity: FragmentActivity?, address:String?)  {
        v = view
        h = handler
        a = activity
        ipv4address = address
    }

    override fun run() {
        val newip4address = CjdnsSocket.getCjdnsIpv4Address()
        if (ipv4address != newip4address) {
            ipv4address = newip4address
            //Restart Service
            a?.startService(Intent(a, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
            a?.startService(Intent(a, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
        }
        val ipText: TextView = v!!.findViewById(R.id.textViewPublicIP)
        GetPublicIP(ipText).execute()

        h!!.postDelayed(this, 10000) //ms
    }
}

object ConnectingThread: Runnable {
    private var v: View? = null
    private var activity: FragmentActivity? = null
    private var h: Handler? = null
    private var ipv4address: String? = null

    fun init(view: View, handler:Handler, a:FragmentActivity?) {
        v = view
        h = handler
        activity = a
    }

    override fun run() {
        val logText: TextView = v!!.findViewById(R.id.textViewLog)
        //Connect to Internet
        CjdnsSocket.IpTunnel_connectTo("cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
        var tries = 0

        //Check for ip adress given by cjdns try for 20 times, 10secs
        while ((ipv4address == null) && (tries < 20)) {
            //Get ipv4 address
            ipv4address = CjdnsSocket.getCjdnsIpv4Address()
            tries++
            Thread.sleep(500)
        }
        if (ipv4address != null) {
            //Restart Service
            activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
            activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
            //Start Thread for checking connection
            h!!.postDelayed(runnableConnection, 10000)
            GetPublicIP(logText)
            logText.post(Runnable { logText.text = "Connected Public IP:" })
        } else {
            //Stop UI thread
            h!!.removeCallbacks(runnableUI)
            h!!.removeCallbacks(runnableConnection)
            logText.post(Runnable { logText.text = "Can not connect to VPN. Authorization needed" })
            val ipText: TextView = v!!.findViewById(R.id.textViewPublicIP)
            ipText.text = ""
        }
    }
}