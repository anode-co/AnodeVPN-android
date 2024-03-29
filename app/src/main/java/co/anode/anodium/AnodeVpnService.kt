package co.anode.anodium

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import co.anode.anodium.support.CjdnsSocket
import timber.log.Timber
import java.io.FileDescriptor
import java.lang.Exception

class AnodeVpnService : VpnService() {
    var mInterface: ParcelFileDescriptor? = null
    var mThread: Thread? = null
    val actionConnect = "co.anode.anodium.START"
    val actionDisconnect = "co.anode.anodium.DISCONNECT"
    val actionStop = "co.anode.anodium.STOP"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                actionConnect -> {
                    mThread = Thread(VpnThread(this), "AnodeVpnService.VpnThread")
                    mThread!!.start()
                }
                actionDisconnect -> {
                    this.onDestroy()
                }
                actionStop -> {
                    stopSelf()
                    stopForeground(true)
                    mInterface?.close()
                    mInterface = null
                }
            }
        } else {
            mThread = Thread(VpnThread(this), "AnodeVpnService.VpnThread")
            mThread!!.start()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (mThread != null) {
            mThread!!.interrupt()
        }
        super.onDestroy()
    }

    override fun stopService(name: Intent?): Boolean {
        if (mThread != null) {
            mThread!!.interrupt()
        }
        return super.stopService(name)
    }

    override fun onRevoke() {
        if (mThread != null) {
            mThread!!.interrupt()
        }
        super.onRevoke()
    }

    fun builder(): Builder = Builder()
}

class VpnThread(private val avpn: AnodeVpnService) : Runnable {
    private var myIp6: String = "fc00::1"

    private fun configVpn() {
        /* Remove check because we want to allow creating the VPN tun with empty addresses
         * for when trying to connect cjdns to Pkt.cube */

        if (CjdnsSocket.ipv4Address.isEmpty() && CjdnsSocket.ipv6Address.isEmpty()) {
            Timber.i("AnodeVPNService: At least one address must be specified")
            return
        }

        val b = avpn.builder().setSession("AnodeVpnService")
            /* Uncomment to allow creating VPN tun with empty addresses
            * for cjdns connecting to Pkt.cube (wifi sharing)*/
            /*.addAddress("fc00::", 128)
            .addRoute("fc00::", 8)*/
            .addDnsServer("1.1.1.1")
            //.allowFamily(AF_INET)

        if (CjdnsSocket.ipv4Address.isNotEmpty() && CjdnsSocket.ipv4Address != "") {
            CjdnsSocket.VPNipv4Address = CjdnsSocket.ipv4Address
            b.addRoute(CjdnsSocket.ipv4Route, CjdnsSocket.ipv4RoutePrefix) //0
            b.addAddress(CjdnsSocket.ipv4Address, CjdnsSocket.ipv4AddressPrefix) //32
        }
        if (CjdnsSocket.ipv6Address.isNotEmpty() && CjdnsSocket.ipv6Address != "") {
            CjdnsSocket.VPNipv6Address = CjdnsSocket.ipv6Address
            b.addRoute(CjdnsSocket.ipv6Route, CjdnsSocket.ipv6RoutePrefix) //0
            b.addAddress(CjdnsSocket.ipv6Address, CjdnsSocket.ipv6AddressPrefix) //64
        }
        val nodeInfo = CjdnsSocket.Core_nodeInfo()
        if (nodeInfo["myIp6"].str().isNotEmpty()) {
            b.addAddress(nodeInfo["myIp6"].str(), 8)
        }

        avpn.mInterface = b.establish()
        Timber.i("interface vpn")
        if (avpn.mInterface != null) {
            val fdNum = CjdnsSocket.Admin_importFd(avpn.mInterface!!.fileDescriptor)
            Timber.i("imported vpn fd $fdNum")
            val response = CjdnsSocket.Core_initTunfd(fdNum)
            Timber.i("initTunfd response: $response[0]")
        }
    }

    private fun init() {
        val info = CjdnsSocket.Core_nodeInfo()
        myIp6 = info["myIp6"].str()
        Timber.i( "AndeVpnService init with IP6: $info")
        CjdnsSocket.cjdnsFd = CjdnsSocket.Admin_exportFd(CjdnsSocket.UDPInterface_getFd(0))
        val protectFd = fdGetInt(CjdnsSocket.cjdnsFd!!)
        Timber.i( "got local fd to protect $protectFd")
        avpn.protect(protectFd)
    }

    private fun main() {
        init()
        configVpn()
    }

    private fun stopVPN() {
        avpn.mInterface!!.close()
        avpn.mInterface = null
    }

    override fun run() {
        try {
            main()
            //DEBUG: for testing notifications
            /*
            var time = System.currentTimeMillis()
            val interval: Long = 10 * 60 * 1000 //10min
            while (true) {
                if(System.currentTimeMillis() > (time + interval)) {
                    time = System.currentTimeMillis()
                    val CHANNEL_ID = "anodium_channel_01"
                    var builder = NotificationCompat.Builder(avpn.applicationContext, CHANNEL_ID)
                            .setSmallIcon(R.mipmap.ic_logo_foreground)
                            .setContentTitle("Test notification")
                            .setContentText("with some short description...")
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    with(NotificationManagerCompat.from(avpn.applicationContext)) {
                        // notificationId is a unique int for each notification that you must define
                        val notificationId = 1
                        notify(notificationId, builder.build())
                    }
                }
                Thread.sleep(interval)
            }*/
        } catch (e: InterruptedException) {
            if (avpn.mInterface != null) {
                stopVPN()
            }
            e.printStackTrace()
        } catch (e: Exception) {
            throw AnodeVPNException("VPNService error "+e.message)
        }
    }
}

class AnodeVPNException(message:String): Exception(message)

private fun fdGetInt(fd: FileDescriptor) =
        FileDescriptor::class.java.getDeclaredMethod("getInt$").invoke(fd) as Int