package co.anode.anodium

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.util.Log
import java.io.FileDescriptor
import java.net.InetAddress
import kotlin.concurrent.thread
import kotlin.experimental.and

val LOGTAG = "co.anode.anodiumvpn"

object CjdnsSocket {
    val ls: LocalSocket = LocalSocket()
    var ipv4Address: String = ""
    var VPNipv4Address: String = ""
    var ipv4AddressPrefix: Int = 0
    var ipv4Route: String = ""
    var ipv4RoutePrefix: Int = 0
    var ipv6Route: String = ""
    var ipv6RoutePrefix: Int = 0
    var ipv6Address: String = ""
    var VPNipv6Address: String = ""
    var ipv6AddressPrefix: Int = 0
    var logpeerStats: String = ""
    var logshowConnections: String = ""

    fun init(path:String ) {
        var tries = 0
        while (tries < 10) {
            try {
                Log.i(LOGTAG, "Connecting to socket...")
                ls.connect(LocalSocketAddress(path, LocalSocketAddress.Namespace.FILESYSTEM))
            } catch (e: java.lang.Exception) {
                if (tries > 100) {
                    throw CjdnsException("Unable to establish socket to cjdns")
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
        Log.i(LOGTAG,benc.toString())
        ls.outputStream.write(benc.bytes())
        val x = read()
        //Log.i(LOGTAG, "$benc-->$x")
        val dec: Benc.Obj
        if (x.isEmpty()) {
            throw CjdnsException("Empty reply, call to $name")
        }
        dec = Benc(x).decode()
        val err = dec["error"]
        if (err.toString() != "null") {
            if (err.str().contains("no tun currently")) {
                //Ignore it
            } else if (err.str().contains("connection not found")) {
                //Ignore it
                return dec
            } else if (err is Benc.Bstr && err.str() != "none") {
                throw CjdnsException("cjdns replied: " + err.str())
            }
        }
        return dec
    }

    fun UDPInterface_getFd(ifNum: Int): Int {
        Log.i(LOGTAG, "getUdpFd")
        val dec = call("UDPInterface_getFd", Benc.dict("interfaceNumber", ifNum))
        val fd = dec["fd"]
        if (fd !is Benc.Bint) {
            throw CjdnsException("getUdpFd cjdns replied without fd $dec")
        }
        return fd.num().toInt()
    }

    fun Admin_exportFd(fd: Int): FileDescriptor {
        Log.i(LOGTAG, "Admin_exportFd $fd")
        call("Admin_exportFd", Benc.dict("fd", fd))
        val fds = ls.ancillaryFileDescriptors
        if (fds == null || fds.isEmpty()) {
            throw CjdnsException("Did not read back file descriptor")
        }
        return fds[0]
    }

    fun Admin_importFd(fd: FileDescriptor): Int {
        Log.i(LOGTAG, "Admin_importFd $fd")
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
        var i = 0
        while(true) {
            peerStats = call("InterfaceController_peerStats", Benc.dict("page", i))
            //logpeerStats = peerStats.str()
            if(peerStats["peers"].toString() != "[]") {
                var x =0
                try {
                    val size = peerStats["peers"].size()
                    while (x < size) {
                        out.add(peerStats["peers"][x] as Benc.Bdict)
                        x++
                    }
                }catch (e: java.lang.Exception) {
                    break
                }
            } else {
                break
            }
            i++
        }
        return out
    }

    fun getIpTunnelConnectionIDs(): MutableList<Int> {
        var connection: Benc.Obj
        val out:MutableList<Int> = mutableListOf()
        var i = 0
        val list= call("IpTunnel_listConnections", null)
        for (i in 0 until list["connections"].size()) {
            try {
                connection = IpTunnel_showConnection(list["connections"][0].num().toInt())
                if (connection.toString().contains("connection not found"))
                    break
                out.add(list["connections"][0].num().toInt())
            }catch (e: java.lang.Exception) {
                break
            }
        }
        return out
    }

    fun IpTunnel_listConnections(): ArrayList<Benc.Bdict> {
        var connection: Benc.Obj
        val out:ArrayList<Benc.Bdict> = ArrayList<Benc.Bdict>()
        var i = 0
        val list= call("IpTunnel_listConnections", null)
        for (i in 0 until list["connections"].size()) {
            try {
                connection = IpTunnel_showConnection(list["connections"][0].num().toInt())
                if (connection.toString().contains("connection not found"))
                    break
                out.add(connection as Benc.Bdict)
            }catch (e: java.lang.Exception) {
                break
            }
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
        Log.i(LOGTAG,"IpTunnel_connectTo: $node")
        call("IpTunnel_connectTo", Benc.dict("publicKeyOfNodeToConnectTo", node))
    }

    fun IpTunnel_removeAllConnections() {
        val connections:MutableList<Int> = getIpTunnelConnectionIDs()
        for (i in 0 until connections.count()) {
            IpTunnel_removeConnection(connections[i])
        }
    }

    fun IpTunnel_removeConnection(num: Int) {
        Log.i(LOGTAG,"IpTunnel_removeConnection: $num")
        call("IpTunnel_removeConnection", Benc.dict("connection", num))
    }

    fun IpTunnel_showConnection(num: Int): Benc.Obj =
            call("IpTunnel_showConnection", Benc.dict("connection", num))

    fun Sign_sign(b64Digest: String): Benc.Obj =
            call("Sign_sign", Benc.dict("msgHash", b64Digest))

    fun getCjdnsRoutes(): Boolean {
        val connectionIDs:MutableList<Int> = getIpTunnelConnectionIDs()
        if (connectionIDs.size > 0) {
            val connection = IpTunnel_showConnection(connectionIDs[0])
            logshowConnections = connection.toString()
            val ip4Address = connection["ip4Address"]
            val ip6Address = connection["ip6Address"]
            //Authorization missing...
            if ((ip4Address.toString() == "null") && (ip6Address.toString() == "null")) {
                return false
            }
            if (connection["ip4Prefix"].toString() != "null") {
                val ip4Prefix = connection["ip4Prefix"]
                ipv4RoutePrefix = ip4Prefix.num().toInt()
                ipv4Route = trimBitsforRoute(ip4Address.str(), ipv4RoutePrefix)
            }
            if (connection["ip6Prefix"].toString() != "null") {
                val ip6Prefix = connection["ip6Prefix"]
                ipv6RoutePrefix = ip6Prefix.num().toInt()
                ipv6Route = trimBitsforRoute(ip6Address.str(), ipv6RoutePrefix)
            }
            if (connection["ip4Alloc"].toString() != "null") {
                val ip4Alloc = connection["ip4Alloc"]
                ipv4AddressPrefix = ip4Alloc.num().toInt()
                ipv4Address = trimBitsforRoute(ip4Address.str(), ipv4AddressPrefix)
            }
            if (connection["ip6Alloc"].toString() != "null") {
                val ip6Alloc = connection["ip6Alloc"]
                ipv6AddressPrefix = ip6Alloc.num().toInt()
                ipv6Address = trimBitsforRoute(ip6Address.str(), ipv6AddressPrefix)
            }
            return true
        } else { return false }
    }

    fun clearRoutes() {
        Log.i(LOGTAG, "clear routes")
        ipv4Address = ""
        ipv4Route = ""
        ipv4RoutePrefix = 0
        ipv4AddressPrefix = 0
        ipv6Route = ""
        ipv6RoutePrefix = 0
        ipv6Address = ""
        ipv6AddressPrefix = 0
    }

    fun trimBitsforRoute(addr: String, prefix: Int): String {
        //Log.i(LOGTAG, "trimBitsforRoute $addr with $prefix")
        var a = InetAddress.getByName(addr)
        val bytes = a.address
        thread {
            if ((prefix shr 3) >= bytes.size) {
                return@thread
            }
            bytes[prefix shr 3] = bytes[prefix shr 3].and( ((0xff shl 8 - prefix % 8).toByte()) )
            for (i in (prefix shr 3) + 1 until bytes.size) {
                bytes[i] = 0
            }
            return@thread
        }.run()
        return InetAddress.getByAddress(bytes).hostAddress
    }

    fun isCjdnsConnected(): Boolean {
        val conn = getNumberofEstablishedPeers()
        return conn > 0
    }

    fun UDPInterface_beginConnection(publickey:String, ip:String, port:Int, password:String,login:String) {
        var peerName = ""
        val address = "$ip:$port"
        call("UDPInterface_beginConnection", Benc.dict("publicKey",publickey,"address", address,"peerName", peerName, "password",password,"login", login, "interfaceNumber",0))
        //call("UDPInterface_beginConnection", Benc.dict("publicKey","9syly12vuwr1jh5qpktmjc817y38bc9ytsvs8d5qwcnvn6c2lwq0.k","address", "94.23.31.145:17102","peerName", peerName, "password","wwbn34yhxhtubtghq6y2pksyt7c9mm8","login", "cjd-snode", "interfaceNumber",0))
        //call("UDPInterface_beginConnection", Benc.dict("publicKey","cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k","address", "198.167.222.70:54673","peerName", peerName, "password","use_more_bandwidth","login", "ipredator.se/cjdns_public_node", "interfaceNumber",0))
        //call("UDPInterface_beginConnection", Benc.dict("publicKey","znrj10d4s005qyg2wg2sx54k4yrqjxyvkc1pz4vqfzxdv5sxvds0.k","address", "188.165.200.79:17102","peerName", peerName, "password","gxv7zc4nyrcqc10wn5yn8pzsl7cs413","login", "ratty.cjdns.fr", "interfaceNumber",0))
    }

    fun SessionManager_sessionStatsByIP(address:String) {
        val result = call("SessionManager_sessionStatsByIP", Benc.dict("ip6",address))
        //get path
        val addr = result["addr"].toString().split(".")
        val path = addr[1]+"."+addr[2]+"."+addr[3]+"."+addr[4]
        SwitchPinger_ping(path)
    }

    fun SwitchPinger_ping(path:String) {
        Log.i(LOGTAG, "SwitchPinger_ping for path: "+path)
        val result = call("SwitchPinger_ping", Benc.dict("path",path))
    }
}

class CjdnsException(message:String): Exception(message)