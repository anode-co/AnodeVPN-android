package com.anode.anode

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import java.io.FileDescriptor
import java.io.InputStream

val LOGTAG = "CjdnsSocket"

private fun setupSocket(socketName: String): LocalSocket {
    val ls = LocalSocket()
    var tries = 0
    while (true) {
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

class CjdnsSocket(val ls: LocalSocket) {
    constructor (path: String) : this(setupSocket(path))

    fun read(): String {
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

    fun call(name: String, args: Benc.Bdict?): Benc.Obj {
        val benc =
                if (args != null) {
                    Benc.dict("q", name, "args", args)
                } else {
                    Benc.dict("q", name)
                }
        ls.outputStream.write(benc.bytes())
        val dec = Benc(read()).decode()
        val err = dec["error"]
        if (err is Benc.Bstr && err.str() != "none") {
            throw Error("cjdns replied: " + err.str())
        }
        return dec
    }

    fun UDPInterface_getFd(ifNum: Int): Int {
        Log.i(LOGTAG, "getUdpFd")
        val dec = call("UDPInterface_getFd", Benc.dict("interfaceNumber", ifNum))
        val fd = dec["fd"]
        if (fd !is Benc.Bint) {
            throw Error("getUdpFd cjdns replied without fd " + dec.toString())
        }
        return fd.num()
    }

    fun Admin_exportFd(fd: Int): FileDescriptor {
        call("Admin_exportFd", Benc.dict("fd", fd))
        val fds = ls.ancillaryFileDescriptors
        if (fds == null || fds.isEmpty()) {
            throw Error("Did not read back file descriptor")
        }
        return fds[0]
    }

    fun Admin_importFd(fd: FileDescriptor): Int {
        ls.setFileDescriptorsForSend(arrayOf(fd))
        return call("Admin_importFd", null)["fd"].num()
    }

    fun Core_nodeInfo(): Benc.Bdict = call("Core_nodeInfo", null) as Benc.Bdict

    fun Core_initTunfd(fd: Int): Benc.Bdict =
            call("Core_initTunfd", Benc.dict("tunfd", fd)) as Benc.Bdict
}