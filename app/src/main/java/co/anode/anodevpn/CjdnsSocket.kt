package co.anode.anodevpn

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import java.io.FileDescriptor
import java.lang.Exception

val LOGTAG = "CjdnsSocket"

object CjdnsSocket {
    val ls: LocalSocket = LocalSocket()
    var ipv4Address: String = ""
    var ipv4AddressPrefix: Int = 0
    var ipv4Route: String = ""
    var ipv4RoutePrefix: Int = 0

    fun init(path:String ) {
        var tries = 0
        while (tries < 10) {
            try {
                Log.i(LOGTAG, "Connecting to socket...")
                ls.connect(LocalSocketAddress(path, LocalSocketAddress.Namespace.FILESYSTEM))
            } catch (e: java.lang.Exception) {
                if (tries > 100) {
                    throw Error("Unable to establish socket to cjdns")
                }
            }
            if (ls.isConnected) {
                ls.sendBufferSize = 1024
                break
            }
            try {
                Thread.sleep(200)
            } catch (e: java.lang.Exception) {
            }
            tries++
        }
    }

    private fun read(): String {
        var tries = 0
        val istr = ls.inputStream
        var av: Int
        do {
            av = istr.available()
            Thread.sleep(50)
            tries++;
        } while ((av < 1) && (tries < 100))
        val b = ByteArray(av)
        istr.read(b)
        return String(b)
    }

    @Synchronized fun call(name: String, args: Benc.Bdict?): Benc.Obj {
        val benc =
                if (args != null) {
                    Benc.dict("q", name, "args", args)
                } else {
                    Benc.dict("q", name)
                }
        ls.outputStream.write(benc.bytes())
        val x = read()
        Log.e(LOGTAG, "$benc-->$x")
        val dec: Benc.Obj
        if (x.isEmpty()) {
            throw Error("Empty reply, call to $name")
        }
        dec = Benc(x).decode()
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
            throw Error("getUdpFd cjdns replied without fd $dec")
        }
        return fd.num().toInt()
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
        val result = call("Admin_importFd", null)["fd"].num().toInt()
        ls.setFileDescriptorsForSend(null)
        return result
    }

    fun Core_nodeInfo(): Benc.Bdict = call("Core_nodeInfo", null) as Benc.Bdict

    fun Core_stopTun(): Benc.Bdict = call("Core_stopTun", null) as Benc.Bdict

    fun Core_initTunfd(fd: Int): Benc.Bdict =
            call("Core_initTunfd", Benc.dict("tunfd", fd)) as Benc.Bdict

    fun InterfaceController_peerStats(): ArrayList<Benc.Bdict> {
        var peerStats: Benc.Obj
        val out:ArrayList<Benc.Bdict> = ArrayList<Benc.Bdict>()
        var totalPeers: Long = 0
        var i = 0
        /*
        peerStats = call("InterfaceController_peerStats", Benc.dict("page", 0))
        out.add(peerStats["peers"][0] as Benc.Bdict)
        */
        while(true) {
            peerStats = call("InterfaceController_peerStats", Benc.dict("page", i))
            if(peerStats["peers"].toString() != "[]")
            {
                out.add(peerStats["peers"][i] as Benc.Bdict)
            } else {
                break
            }
            i++
        }
        return out
    }

    fun getNumberofEstablishedPeers(): Int {
        val peers:ArrayList<Benc.Bdict> = InterfaceController_peerStats()
        var totalestablishedpeers: Int = 0
        for (i in 0 until peers.count()) {
            if (peers[i]["state"].toString() == "\"ESTABLISHED\"") {
                totalestablishedpeers++
            }
        }
        return totalestablishedpeers
    }

    fun IpTunnel_connectTo(node: String) {
        call("IpTunnel_connectTo", Benc.dict("publicKeyOfNodeToConnectTo", node))
    }

    fun RouteGen_getPrefixes(): String {
        //get routes
        val routes: Benc.Obj = call("RouteGen_getPrefixes", null)
        this.ipv4Route = routes["routes"].toString()
        return this.ipv4Route
    }

    fun IpTunnel_showConnection(num: Int): Benc.Obj {
        val conn: Benc.Obj
        conn = call("IpTunnel_showConnection", Benc.dict("connection", num))
        return conn
    }

    fun getCjdnsIpv4Address(): String? {
        var connection = IpTunnel_showConnection(0)

        var address = connection["ip4Address"]
        var addprefix = connection["ip4Prefix"]
        var routeprefix = connection["ip4Alloc"]
        //Authorization missing...
        if (address.toString() == "null") {
            return null
        }
        this.ipv4Address = address.str()
        this.ipv4Route = address.str()
        this.ipv4RoutePrefix = routeprefix.num().toInt()
        this.ipv4AddressPrefix = addprefix.num().toInt()
        return address.str()
    }
}