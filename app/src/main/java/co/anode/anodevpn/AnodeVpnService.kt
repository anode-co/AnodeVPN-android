package co.anode.anodevpn

import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.system.OsConstants.AF_INET
import android.util.Log
import java.io.FileDescriptor


class AnodeVpnService : VpnService() {
    var mThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        mThread = Thread(VpnThread(this), "AnodeVpnService.VpnThread")
        //start the service
        mThread!!.start()
        return START_STICKY
    }

    override fun onDestroy() { // TODO Auto-generated method stub
        if (mThread != null) {
            mThread!!.interrupt()
        }
        super.onDestroy()
    }

    fun builder(): Builder = Builder()
}

class VpnThread(val avpn: AnodeVpnService) : Runnable {
    private var mInterface: ParcelFileDescriptor? = null
    private var myIp6: String = ""

    private fun configVpn() {
        val b = avpn.builder().setSession("AnodeVpnService")
                .addAddress(myIp6, 128)
                .allowFamily(AF_INET)
                .allowBypass()

        if (CjdnsSocket.ipv4Address.isEmpty()) {
            b.addRoute("fc00::",8)
        } else {
            b.addRoute(CjdnsSocket.ipv4Route,CjdnsSocket.ipv4RoutePrefix)
            b.addAddress(CjdnsSocket.ipv4Address, CjdnsSocket.ipv4AddressPrefix)
        }

        mInterface = b.establish()
        Log.i(LOGTAG, "interface vpn")
        val fdNum = CjdnsSocket.Admin_importFd(mInterface!!.fileDescriptor)
        Log.i(LOGTAG, "imported vpn fd $fdNum")
        Log.i(LOGTAG, CjdnsSocket!!.Core_initTunfd(fdNum).toString())
        Log.i(LOGTAG, "vpn launched")
    }

    private fun init() {
        CjdnsSocket.init(AnodeUtil().CJDNS_PATH + "/" + AnodeUtil().CJDROUTE_SOCK)
        val info = CjdnsSocket.Core_nodeInfo()
        myIp6 = info["myIp6"].str()
        Log.i(LOGTAG, info.toString())
        val protectFd = fdGetInt(CjdnsSocket.Admin_exportFd(CjdnsSocket.UDPInterface_getFd(0)))
        Log.i(LOGTAG, "got local fd to protect " + protectFd)
        avpn.protect(protectFd);
    }

    fun main() {
        init()
        configVpn()
        while (true) { Thread.sleep(1000); }
    }

    override fun run() {
        try {
            main()
        } catch (e: Exception) {
            if (mInterface != null) {
                try {
                    mInterface!!.close()
                } catch (ee: Exception) {
                }
                mInterface = null
            }
            e.printStackTrace()
        }
    }
}

private fun fdGetInt(fd: FileDescriptor) =
        FileDescriptor::class.java.getDeclaredMethod("getInt$").invoke(fd) as Int