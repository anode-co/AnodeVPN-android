package com.anode.anode

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.*

class AnodeUtil {
    private val LOGTAG = "anodeVPNService"
    private val CJDNS_PATH = "/data/data/com.anode.anode/files"
    private val CJDROUTE_LOG = "cjdroute.log"
    private val cjdrouteBinFile = "cjdroute"
    private val cjdrouteTmpConfFile = "tempcjdroute.conf"
    private val cjdrouteConfFile = "cjdroute.conf"

    fun launch() {
        var cjdnsIPv6Address = ""
        val confFile = File(CJDNS_PATH + "/" + cjdrouteConfFile)
        if (confFile.exists()) {
            cjdnsIPv6Address = getIPv6Address()
        } else {
            Log.i(LOGTAG, "Trying to create new cjdroute.conf file...")
            generateConfFile()
            modifyJSONConfFile()
            cjdnsIPv6Address = getIPv6Address()
            if (cjdnsIPv6Address === "") { //TODO:handle error creating conf file...
                throw RuntimeException("Failed to create conf file")
            }
        }
        Log.i(LOGTAG, "Got ipv6 address: $cjdnsIPv6Address")
        //Launch cjdroute with configuration file
        launchCJDNS()
    }

    private fun generateConfFile() {
        val processBuilder = ProcessBuilder()
        try {
            processBuilder.command("$CJDNS_PATH/$cjdrouteBinFile", "--genconf")
                    .redirectOutput(File(CJDNS_PATH, cjdrouteTmpConfFile))
                    .start()
                    .waitFor()
            //Clean conf
            processBuilder.command("$CJDNS_PATH/$cjdrouteBinFile", "--cleanconf")
                    .redirectInput(File(CJDNS_PATH, cjdrouteTmpConfFile))
                    .redirectOutput(File(CJDNS_PATH, cjdrouteConfFile))
                    .start()
                    .waitFor()
        } catch (e: InterruptedException) {
            Log.e(LOGTAG, "Failed to generate new configuration file", e)
            e.printStackTrace()
        } catch (e: IOException) {
            Log.e(LOGTAG, "Failed to generate new configuration file", e)
            e.printStackTrace()
        }
        try {
            Thread.sleep(300)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        //TODO: Delete cjdrouteTmpConfFile
    }

    private fun launchCJDNS() {
        val os: DataOutputStream? = null
        try {
            Log.e(LOGTAG, "Launching cjdroute (file size: " +
                    File("$CJDNS_PATH/$cjdrouteBinFile").length() + ")")
            val processBuilder = ProcessBuilder()
            //Run cjdroute with existing conf file
            val pb: ProcessBuilder = processBuilder.command("$CJDNS_PATH/$cjdrouteBinFile")
                    .redirectInput(File(CJDNS_PATH, cjdrouteConfFile))
                    .redirectOutput(File(CJDNS_PATH, CJDROUTE_LOG))
                    .redirectErrorStream(true)
            pb.environment()["TMPDIR"] = CJDNS_PATH
            val p = processBuilder.start()
            p.waitFor()
            Log.e(LOGTAG, "cjdns exited with " + p.exitValue())
        } catch (e: Exception) {
            Log.e(LOGTAG, "Failed to execute cjdroute", e)
        } finally {
        }
    }

    @Throws(IOException::class)
    fun readJSONFile(filename: String?): String {
        val confFile = FileInputStream(filename)
        val fileContent = StringBuffer("")
        val buffer = ByteArray(1024)
        var size: Int
        while (confFile.read(buffer).also { size = it } != -1) {
            fileContent.append(String(buffer, 0, size))
        }
        return fileContent.toString()
    }

    private fun modifyJSONConfFile() {
        try {
            val filecontent = readJSONFile("$CJDNS_PATH/$cjdrouteConfFile")
            val json = JSONObject(filecontent)
            val interfaces = json.getJSONObject("interfaces")
            val UDPInterface = interfaces.getJSONArray("UDPInterface")
            //Add Peers
//JSONObject connectTo = UDPInterface.getJSONObject(0).getJSONObject("connectTo");
            val peer = JSONObject()
            val peervalues = JSONObject()
            peervalues.put("login", "cjd-snode")
            peervalues.put("password", "wwbn34yhxhtubtghq6y2pksyt7c9mm8")
            peervalues.put("publicKey", "9syly12vuwr1jh5qpktmjc817y38bc9ytsvs8d5qwcnvn6c2lwq0.k")
            peer.put("94.23.31.145:17102", peervalues)
            peervalues.put("login", "ipredator.se/cjdns_public_node")
            peervalues.put("password", "use_more_bandwidth")
            peervalues.put("publicKey", "cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
            peer.put("198.167.222.70:54673", peervalues)
            UDPInterface.getJSONObject(0).put("connectTo", peer)
            //Add tunfd and tunnel socket
            val router = json.getJSONObject("router")
            val interf = router.getJSONObject("interface")
            interf.put("tunfd", "android")
            //interf.put("tunDevice",CJDNS_PATH+"/"+CJDROUTE_SOCK);
//Set security user to 0
            val security = json.getJSONArray("security")
            security.getJSONObject(0).put("setuser", 0)
            security.getJSONObject(0).remove("keepNetAdmin")
            //json.put("pipe",CJDNS_PATH);
//Save file
            val writer = BufferedWriter(FileWriter("$CJDNS_PATH/$cjdrouteConfFile"))
            val out = json.toString().replace("\\/", "/")
            writer.write(out)
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun getIPv6Address(): String {
        Log.i(LOGTAG, "Trying to read IPv6 address from cjdroute.conf file...")
        var address = ""
        //Read cjdroute conf file to extract the address
        try {
            val filecontent = readJSONFile("$CJDNS_PATH/$cjdrouteConfFile")
            val json = JSONObject(filecontent)
            address = json.getString("ipv6")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return address
    }
}