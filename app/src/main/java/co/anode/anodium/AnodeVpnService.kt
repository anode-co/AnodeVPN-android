package co.anode.anodium

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileDescriptor
import java.lang.Exception


class AnodeVpnService : VpnService() {
    var mThread: Thread? = null
    val actionConnect = "co.anode.anodium.START"
    val actionDisconnect = "co.anode.anodium.STOP"

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null && actionDisconnect == intent.action) {
            this.onDestroy()
            return START_NOT_STICKY;
        } else {
            mThread = Thread(VpnThread(this), "AnodeVpnService.VpnThread")
            //start the service
            mThread!!.start()
            return START_STICKY;
        }
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
    private var mInterface: ParcelFileDescriptor? = null
    private var myIp6: String = ""

    private fun configVpn() {
        val b = avpn.builder().setSession("AnodeVpnService")
                //.addAddress(myIp6, 128)
                //.addRoute("fc00::",8)
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

        mInterface = b.establish()
        Log.i(LOGTAG, "interface vpn")
        val fdNum = CjdnsSocket.Admin_importFd(mInterface!!.fileDescriptor)
        Log.i(LOGTAG, "imported vpn fd $fdNum")
        Log.i(LOGTAG, CjdnsSocket.Core_initTunfd(fdNum).toString())
        Log.i(LOGTAG, "vpn launched")
    }

    private fun init() {
        val info = CjdnsSocket.Core_nodeInfo()
        myIp6 = info["myIp6"].str()
        Log.i(LOGTAG, info.toString())
        val protectFd = fdGetInt(CjdnsSocket.Admin_exportFd(CjdnsSocket.UDPInterface_getFd(0)))
        Log.i(LOGTAG, "got local fd to protect $protectFd")
        avpn.protect(protectFd)
    }

    private fun main() {
        init()
        configVpn()
    }

    private fun stopVPN() {
        mInterface!!.close()
        mInterface = null
    }

    override fun run() {
        try {
            main()
            //TODO: DEBUG for testing notifications
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
            if (mInterface != null) {
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