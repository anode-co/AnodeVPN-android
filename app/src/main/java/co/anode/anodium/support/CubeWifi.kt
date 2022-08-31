package co.anode.anodium.support

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.*
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.util.Log
import android.widget.TextView
import androidx.annotation.RequiresApi
import java.io.IOException
import java.net.*
import java.util.*
import java.util.concurrent.Executors


@SuppressLint("StaticFieldLeak")
object CubeWifi {
    lateinit var context: Context
    lateinit var statusbar: TextView
    val LOGTAG = "co.anode.anodiumvpn"

    private val wifiSSID = "Pkt.cube"
    val mServiceName = "OpenWrt"
    val mServiceType = "_udpdummy._udp."
    private lateinit var nsdManager: NsdManager
    var mService: NsdServiceInfo = NsdServiceInfo()
    lateinit var mServiceHost: InetAddress
    var mServicePort = 0
    lateinit var udpSocket: DatagramSocket
    lateinit var pktNetwork: Network
    private var nsdListenerActive = false
    private var usemDNS = false

    private lateinit var connManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    fun init(c: Context) {
        context = c
        nsdManager = context.getSystemService(NSD_SERVICE) as NsdManager
    }

    fun disconnect() {
        statusbar.post{ statusbar.text = "Disconnect $wifiSSID" }
        if (this::connManager.isInitialized) {
            connManager.unregisterNetworkCallback(networkCallback)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun connectPasspoint() {
        val suggestion4 = WifiNetworkSuggestion.Builder()
            .setSsid(wifiSSID)
            .setIsAppInteractionRequired(false)
            .build();

        val suggestionsList = listOf(suggestion4);

        val wifiManager = context.getSystemService(WIFI_SERVICE) as WifiManager;

        val status = wifiManager.addNetworkSuggestions(suggestionsList);
        if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
            // do error handling here
        }

// Optional (Wait for post connection broadcast to one of your suggestions)
        val intentFilter = IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (!intent.action.equals(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                    return;
                }
                // do post connect processing here
            }
        };
        context.registerReceiver(broadcastReceiver, intentFilter);

    }
    fun connect() {
        //Get cjdns filedescriptor
        if (CjdnsSocket.cjdnsFd == null) {
            CjdnsSocket.cjdnsFd = CjdnsSocket.Admin_exportFd(CjdnsSocket.UDPInterface_getFd(0))
        }
        //get peers
        CjdnsSocket.InterfaceController_peerStats()
        //remove All peers
        AnodeClient.removeCjdnsPeers()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Thread({
                connManager = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                val wifi = WifiNetworkSpecifier.Builder()
                    .setSsid(wifiSSID)
                    .build()

                val networkReq = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .setNetworkSpecifier(wifi)
                    .build()

                networkCallback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        network.bindSocket(CjdnsSocket.cjdnsFd)
                        udpSocket = DatagramSocket()
                        pktNetwork = network
                        discoverService()
                        //AnodeUtil.addCjdnsPeers()
                        statusbar.post { statusbar.text = "Connected to $wifiSSID" }
                    }

                    override fun onUnavailable() {
                        super.onUnavailable()
                        statusbar.post { statusbar.text = "$wifiSSID unavailable" }
                        Thread.sleep(10000)
                        connect()
                    }

                    override fun onLost(network: Network) {
                        super.onLost(network)
                        statusbar.post { statusbar.text = "Lost $wifiSSID" }
                        Thread.sleep(10000)
                        connect()
                    }
                }

                connManager.requestNetwork(networkReq, networkCallback)
            }, "CubeWifi.Connect").start()
        }
    }

    fun bindSocket(socket:DatagramSocket) {
        if (this::pktNetwork.isInitialized)
            pktNetwork.bindSocket(socket)
    }

    fun isConnected() :Boolean {
        return this::connManager.isInitialized && this::pktNetwork.isInitialized
    }

    fun discoverService() {
        if (nsdListenerActive) {
            nsdManager.stopServiceDiscovery(discoveryListener)
        }
        while (nsdListenerActive) {
            Thread.sleep(100)
        }
        try {
            nsdManager.discoverServices(mServiceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e:Exception) {
            val error = e.message
        }
    }

    fun sendmessagetoservice(){
        connManager.bindProcessToNetwork(pktNetwork)
        val sendExecutor = Executors.newFixedThreadPool(1)
        val receiveExecutor = Executors.newFixedThreadPool(1)

        udpSocket.soTimeout = 5000
        val timertask = object : TimerTask() {
            override fun run() {
                sendExecutor.execute {
                    sendUdp("Hello Pkt.Cube")
                }
                receiveExecutor.execute {
                    receiveudp()
                }
            }
        }
        val timer = Timer()
        timer.schedule(timertask, 0, 1000)
    }

    private fun sendUdp(msg: String) {
        val buf = msg.toByteArray()
        if (this::mServiceHost.isInitialized) {

            val packet = DatagramPacket(buf, buf.size, mServiceHost,mServicePort)
            udpSocket.send(packet)
            statusbar.post { statusbar.text = "Message $msg send." }
        } else {
            mServiceHost = InetAddress.getByName("172.31.242.254")
            mServicePort = 5555
            val packet = DatagramPacket(buf, buf.size, mServiceHost,mServicePort)
            udpSocket.send(packet)
            statusbar.post { statusbar.text = "Message $msg send to default host." }
        }
    }

    fun receiveudp() {
        val message = ByteArray(1024)
        connManager.bindProcessToNetwork(pktNetwork)
        val packet = DatagramPacket(message, message.size)
        try {
            udpSocket.receive(packet)
            val text = String(message, 0, packet.length)
            statusbar.post { statusbar.text = "Received: $text" }
        } catch (e: IOException) {
            statusbar.post { statusbar.text = "Received: ${e.message}" }
        } catch (e: SocketTimeoutException) {
            statusbar.post { statusbar.text = "Received: ${e.message}" }
        } catch (e: SocketException) {
            statusbar.post { statusbar.text = "Received: ${e.message}" }
        }
    }

    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onDiscoveryStarted(regType: String) {
            nsdListenerActive = true
            Log.d(LOGTAG, "Service discovery started")
            statusbar.post { statusbar.text = "Service discovery started"
            }
        }

        override fun onServiceFound(service: NsdServiceInfo) {
            Log.d(LOGTAG, "Service discovery success$service")
            nsdListenerActive = true
            when {
                ((service.serviceType == mServiceType) &&
                        (service.serviceName == mServiceName)) -> nsdManager.resolveService(service, resolveListener)
            }
        }

        override fun onServiceLost(service: NsdServiceInfo) {
            nsdListenerActive = true
            Log.e(LOGTAG, "service lost: $service")
            statusbar.post { statusbar.text = "service lost: $service" }
        }

        override fun onDiscoveryStopped(serviceType: String) {
            nsdListenerActive = false
            Log.i(LOGTAG, "Discovery stopped: $serviceType")
            statusbar.post { statusbar.text = "Discovery stopped"
            }
        }

        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(LOGTAG, "Discovery failed: Error code:$errorCode")
            nsdListenerActive = false
            nsdManager.stopServiceDiscovery(this)
            statusbar.post { statusbar.text = "Discovery failed"
            }
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(LOGTAG, "Discovery failed: Error code:$errorCode")
            nsdListenerActive = true
            nsdManager.stopServiceDiscovery(this)
            statusbar.post { statusbar.text = "Discovery failed"
            }
        }
    }
    private val resolveListener = object : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            // Called when the resolve fails. Use the error code to debug.
            Log.e(LOGTAG, "Resolve failed: $errorCode")
            statusbar.post { statusbar.text = "Service resolve failed!"
            }
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.e(LOGTAG, "Resolve Succeeded. $serviceInfo")

            mService = serviceInfo
            mServicePort = serviceInfo.port
            mServiceHost = serviceInfo.host
            statusbar.post { statusbar.text = "${mService.serviceType} resolved!" }
            //sendmessagetoservice()
        }
    }
}