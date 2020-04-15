package com.anode.anode

import android.content.Intent
import android.net.Network
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import java.io.FileDescriptor
import java.lang.reflect.Method

class AnodeVpnService : VpnService() {
    var mThread: Thread? = null
    val anodeUtil: AnodeUtil = AnodeUtil()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        anodeUtil.launch()
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
    private var cjdns: CjdnsSocket? = null
    private var myIp6: String = ""
    fun configVpn() {
        val b = avpn.builder().setSession("AnodeVpnService")
                .addRoute("fc00::", 8)
                .addAddress(myIp6, 128)
        mInterface = b.establish()
        Log.i(LOGTAG, "interface vpn")
        val fdNum = cjdns!!.Admin_importFd(mInterface!!.fileDescriptor)
        Log.i(LOGTAG, "imported vpn fd $fdNum")
        Log.i(LOGTAG, cjdns!!.Core_initTunfd(fdNum).toString())
        Log.i(LOGTAG, "vpn launched")
    }
    fun init() {
        cjdns = CjdnsSocket(AnodeUtil.CJDNS_PATH + "/" + AnodeUtil.CJDROUTE_SOCK)
        val info = cjdns!!.Core_nodeInfo()
        myIp6 = info["myIp6"].str()
        Log.i(LOGTAG, info.toString())
        val protectFd = fdGetInt(cjdns!!.Admin_exportFd(cjdns!!.UDPInterface_getFd(0)))
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