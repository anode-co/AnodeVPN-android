package co.anode.anodium

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment


class FirstFragment : Fragment() {
    val h = Handler()
    var ipv4address:String? = null

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? { // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false)
    }

    @SuppressLint("SetTextI18n", "UseRequireInsteadOfGet")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
/*
        val link: TextView = view.findViewById(R.id.textViewLink)
        val text: Spanned = HtmlCompat.fromHtml("Open <a href='http://[fc50:71b5:aebf:7b70:6577:ec8:2542:9dd9]/'>CJDNS network</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
        link.movementMethod = LinkMovementMethod.getInstance()
        link.text = text
        val pubkey: TextView = view.findViewById(R.id.textViewPubkey)
        pubkey.text = context?.resources?.getString(R.string.public_key) +" "+ AnodeUtil(context).getPubKey()
        //Show version number
        val welcomemsg: TextView = view.findViewById(R.id.textview_first)
        welcomemsg.text = welcomemsg.text.toString()+"\nv"+BuildConfig.VERSION_NAME
        //Initialize runnable threads
        runnableUI.init(view,h)
        runnableConnection.init(view,h,activity,ipv4address)

        val switchVpn = view.findViewById<Switch>(R.id.switchVpn)
        //Listener for the Master switch
        switchVpn?.setOnClickListener {
            val isChecked = switchVpn.isChecked
            if (isChecked) {
                Log.i(LOGTAG,"Main Switch checked")
                Toast.makeText(this.context, context?.resources?.getString(R.string.main_switch_on_msg), Toast.LENGTH_SHORT).show()
                //Start the VPN service
                requireActivity().startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
                switchInternet.isClickable = true
                h.postDelayed(runnableUI, 1000)//Start thread for status of peers
            } else {//Switch OFF
                Toast.makeText(this.context, context?.resources?.getString(R.string.main_switch_off_msg), Toast.LENGTH_SHORT).show()
                Disconnect()
                //Disable 2nd switch
                switchInternet.isChecked = false
            }}

        val switchInternet = view.findViewById<Switch>(R.id.switchInternet)
        switchInternet?.setOnClickListener {
            val isChecked = switchInternet.isChecked
            if (!switchVpn.isChecked) {
                Toast.makeText(this.context, context?.resources?.getString(R.string.main_switch_enable_msg), Toast.LENGTH_SHORT).show()
                switchInternet.isChecked = false
            } else {
                if (isChecked) {
                    Log.i(LOGTAG, "Internet Switch checked")
                    val ipText: TextView = view.findViewById(R.id.textViewPublicIP)
                    ipText.text = R.string.connecting.toString()
                    Toast.makeText(this.context, context?.resources?.getString(R.string.internet_switch_on_msg), Toast.LENGTH_SHORT).show()
                    //Start connecting thread
                    val executor: ExecutorService = Executors.newSingleThreadExecutor()
                    ConnectingThread.init(view, h, activity)
                    executor.submit(ConnectingThread)
                } else {
                    Log.i(LOGTAG, "Internet Switch unchecked")
                    Toast.makeText(this.context, context?.resources?.getString(R.string.internet_switch_off_msg), Toast.LENGTH_SHORT).show()
                    Disconnect()
                    if (switchVpn.isChecked) {
                        activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
                        h.postDelayed(runnableUI, 1000)
                    }
                }
            }
        }

        val buttonLog = view.findViewById<Button>(R.id.buttonLog)
        buttonLog?.setOnClickListener {
            this.context?.let { it1 -> PostLogs(it1).execute() }
        }

 */
    }

    override fun onResume() { super.onResume() }
/*
    fun stopThreads() {
        h.removeCallbacks(runnableUI)
        h.removeCallbacks(runnableConnection)
    }

    fun Disconnect() {
        CjdnsSocket.Core_stopTun()
        val logText: TextView = requireView().findViewById(R.id.textViewLog)
        logText.text = context?.resources?.getString(R.string.disconnected)
        val ipText: TextView = requireView().findViewById(R.id.textViewPublicIP)
        ipText.text = context?.resources?.getString(R.string.disconnected)
        stopThreads()
        CjdnsSocket.clearRoutes()
        activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
    }

    companion object {
        private const val LOGTAG = "co.anode.anodium"
    }

 */
}
/*


class GetPublicIP(): AsyncTask<TextView, Void, String>() {
    var ipText:TextView? = null

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

object runnableUI: Runnable {
    private var v: View? = null
    private var h:Handler? = null

    fun init(view: View, handler: Handler)  {
        v = view
        h = handler
    }

    var info = 0
    @SuppressLint("SetTextI18n")
    override fun run() {
        val switchVpn: Switch = v!!.findViewById(R.id.switchVpn)
        if (CjdnsSocket.ls.isConnected) {
            info = CjdnsSocket.getNumberofEstablishedPeers()
            if (info > 0) {
                switchVpn.isChecked = true
            }
        } else {
            info = 0
            switchVpn.isChecked = false
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
    private var ipv6address: String? = null

    fun init(view: View, handler: Handler, activity: FragmentActivity?, address:String?)  {
        v = view
        h = handler
        a = activity
        ipv4address = address
    }

    override fun run() {
        val switchInternet: Switch = v!!.findViewById(R.id.switchInternet)
        CjdnsSocket.getCjdnsRoutes()
        val newip4address = CjdnsSocket.ipv4Address
        val newip6address = CjdnsSocket.ipv6Address
        //Update UI
        switchInternet.isChecked = (ipv4address != "") || (ipv6address != "")
        //Reset VPN with new address
        if ((ipv4address != newip4address) || (ipv4address != newip4address)){
            ipv4address = newip4address
            ipv6address = newip6address
            //Restart Service
            CjdnsSocket.Core_stopTun()
            a?.startService(Intent(a, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
            a?.startService(Intent(a, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
        }
        val ipText: TextView = v!!.findViewById(R.id.textViewPublicIP)
        GetPublicIP().execute(ipText)

        h!!.postDelayed(this, 10000) //ms
    }
}

object ConnectingThread: Runnable {
    private var v: View? = null
    private var activity: FragmentActivity? = null
    private var h: Handler? = null

    fun init(view: View, handler:Handler, a:FragmentActivity?) {
        v = view
        h = handler
        activity = a
    }

    @SuppressLint("SetTextI18n")
    override fun run() {
        var iconnected: Boolean = false
        val logText: TextView = v!!.findViewById(R.id.textViewLog)
        val ipText: TextView = v!!.findViewById(R.id.textViewPublicIP)
        //Connect to Internet
        CjdnsSocket.IpTunnel_connectTo("cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
        var tries = 0
        logText.post(Runnable { logText.text = "Connecting..." })
        //Check for ip address given by cjdns try for 20 times, 10secs
        while (!iconnected && (tries < 10)) {
            iconnected = CjdnsSocket.getCjdnsRoutes()
            tries++
            Thread.sleep(2000)
        }
        if (iconnected) {
            //Restart Service
            CjdnsSocket.Core_stopTun()
            activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
            activity?.startService(Intent(activity, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
            //Start Thread for checking connection
            h!!.postDelayed(runnableConnection, 10000)
        } else {
            //Stop UI thread
            h!!.removeCallbacks(runnableUI)
            h!!.removeCallbacks(runnableConnection)
            logText.post(Runnable { logText.text = "Can not connect to VPN. Authorization needed" })
            ipText.text = ""
        }
    }
}

 */