package co.anode.anodevpn

import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths

class AnodeUtil {
    private val LOGTAG = "anodeVPNService"
    val CJDNS_PATH = "/data/data/co.anode.anodevpn/files"
    val CJDROUTE_SOCK = "cjdroute.sock"
    val CJDROUTE_BINFILE = "cjdroute"
    val CJDROUTE_CONFFILE = "cjdroute.conf"
    private val CJDROUTE_LOG = "cjdroute.log"
    private val CJDROUTE_TEMPCONFFILE = "tempcjdroute.conf"



    fun launch() {
        val confFile = File("$CJDNS_PATH/$CJDROUTE_CONFFILE")
        if (confFile.exists()) {
            launchCJDNS()
        } else {
            Log.i(LOGTAG, "Trying to create new cjdroute.conf file...")
            generateConfFile()
            modifyJSONConfFile()
        }
    }

    fun initializeCjdrouteConfFile() {
        generateConfFile()
        modifyJSONConfFile()
    }

    private fun generateConfFile() {
        val processBuilder = ProcessBuilder()
        try {
            processBuilder.command("$CJDNS_PATH/$CJDROUTE_BINFILE", "--genconf")
                    .redirectOutput(File(CJDNS_PATH, CJDROUTE_TEMPCONFFILE))
                    .start()
                    .waitFor()
            //Clean conf
            processBuilder.command("$CJDNS_PATH/$CJDROUTE_BINFILE", "--cleanconf")
                    .redirectInput(File(CJDNS_PATH, CJDROUTE_TEMPCONFFILE))
                    .redirectOutput(File(CJDNS_PATH, CJDROUTE_CONFFILE))
                    .start()
                    .waitFor()
        } catch (e: InterruptedException) {
            Log.e(LOGTAG, "Failed to generate new configuration file", e)
            e.printStackTrace()
        } catch (e: IOException) {
            Log.e(LOGTAG, "Failed to generate new configuration file", e)
            e.printStackTrace()
        }

        //Delete temp file
        Files.delete(Paths.get("$CJDNS_PATH/$CJDROUTE_TEMPCONFFILE"))
    }

    private fun launchCJDNS() {
        try {
            Log.e(LOGTAG, "Launching cjdroute (file size: " +
                    File("$CJDNS_PATH/$CJDROUTE_BINFILE").length() + ")")
            val processBuilder = ProcessBuilder()
            //Run cjdroute with existing conf file
            val pb: ProcessBuilder = processBuilder.command("$CJDNS_PATH/$CJDROUTE_BINFILE")
                    .redirectInput(File(CJDNS_PATH, CJDROUTE_CONFFILE))
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
    fun readJSONFile(filename: String): String {
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
            val filecontent = readJSONFile("$CJDNS_PATH/$CJDROUTE_CONFFILE")
            val json = JSONObject(filecontent)
            val interfaces = json.getJSONObject("interfaces")
            val UDPInterface = interfaces.getJSONArray("UDPInterface")
            //Add Peers
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

            //Save file
            val writer = BufferedWriter(FileWriter("$CJDNS_PATH/$CJDROUTE_CONFFILE"))
            val out = json.toString().replace("\\/", "/")
            writer.write(out)
            writer.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun getPubKey(): String {
        var pubkey = ""
        try {
            val filecontent = readJSONFile("$CJDNS_PATH/$CJDROUTE_CONFFILE")
            val json = JSONObject(filecontent)
            pubkey = json.getString("publicKey")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return pubkey
    }
}