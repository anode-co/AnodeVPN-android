package com.anode.anode

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import java.io.File
import java.io.FileDescriptor

val LOGTAG = "CjdnsSocket"

object CjdnsSocket {
    fun read(ls: LocalSocket): String {
        val istr = ls.inputStream
        var av: Int
        do {
            av = istr.available()
            Thread.sleep(50)
        } while (av < 1)
        val b = ByteArray(av)
        istr.read(b)
        return String(b)
    }

    fun getUdpFd(ls: LocalSocket, ifNum: Int): Int {
        Log.i(LOGTAG, "getUdpFd")
        ls.outputStream.write(Benc.dict(
                "q", "UDPInterface_getFd",
                "args", Benc.dict("interfaceNumber", ifNum)
        ).bytes())
        val s = read(ls)
        val dec = Benc(s).decode()
        val err = dec["error"]
        if (err is Benc.Bstr && err.str() != "none") {
            throw Error("getUdpFd cjdns replied: " + err.str())
        }
        val fd = dec["fd"]
        if (fd !is Benc.Bint) {
            throw Error("getUdpFd cjdns replied without fd " + dec.toString())
        }
        return fd.num()
    }

    fun exportFd(ls: LocalSocket, fd: Int): FileDescriptor {
        ls.outputStream.write(Benc.dict(
                "q", "Admin_exportFd",
                "args", Benc.dict("fd", fd)
        ).bytes())
        val reply = read(ls)
        Log.i(LOGTAG, "Raw    : " + reply)
        Log.i(LOGTAG, "Decoded: " + Benc(reply).decode())
        val fds = ls.ancillaryFileDescriptors
        if (fds == null || fds.isEmpty()) {
            throw Error("Did not read back file descriptor")
        }
        return fds[0]
    }

    fun exportUdp(ls: LocalSocket, ifNum: Int) = exportFd(ls, getUdpFd(ls, ifNum))

    fun setupSocket(): LocalSocket {
        val ls = LocalSocket()
        var tries = 0
        while (true) {
            val socketName = AnodeUtil.CJDNS_PATH + "/" + AnodeUtil.CJDROUTE_SOCK
            try {
                Log.i(AnodeUtil.LOGTAG, "Connecting to socket...")
                ls.connect(LocalSocketAddress(socketName, LocalSocketAddress.Namespace.FILESYSTEM))
            } catch (e: java.lang.Exception) {
                if (tries > 100) {
                    throw Error("Unable to establish socket to cjdns")
                }
            }
            if (ls.isConnected()) {
                ls.sendBufferSize = 1024
                break
            }
            try {
                Thread.sleep(200)
            } catch (e: java.lang.Exception) {
            }
            tries++
        }
        return ls
    }
}