package com.anode.anode

import android.content.Intent
import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log

class AnodeVpnService : VpnService() {
    var mThread: Thread? = null
    val anodeUtil: AnodeUtil = AnodeUtil()
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        anodeUtil.launch()
        mThread = Thread(VpnThread(), "AnodeVpnService.VpnThread")
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
}

class VpnThread : Runnable {
    private var mInterface: ParcelFileDescriptor? = null
    fun main() {
        val ls = CjdnsSocket.setupSocket()
        val num = CjdnsSocket.exportUdp(ls, 0)
        Log.i(AnodeUtil.LOGTAG, "Socketnum = $num")
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