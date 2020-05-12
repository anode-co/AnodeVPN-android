package co.anode.anodevpn

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

class AnodeUtil(private val context: Context?) {
    private val LOGTAG = "co.anode.anodevpn"
    val CJDNS_PATH = "/data/data/co.anode.anodevpn/files"
    val CJDROUTE_SOCK = "cjdroute.sock"
    val CJDROUTE_BINFILE = "cjdroute"
    val CJDROUTE_CONFFILE = "cjdroute.conf"
    val CJDROUTE_LOG = "cjdroute.log"
    private val CJDROUTE_TEMPCONFFILE = "tempcjdroute.conf"

    fun initializeApp() {
        //Create files folder
        Log.i(LOGTAG, "Creating files directory")
        context!!.filesDir.mkdir()
        val cjdrouteFile = File("$CJDNS_PATH/$CJDROUTE_BINFILE")

        //Read architecture
        val arch = System.getProperty("os.arch")
        var `in`: InputStream? = null
        val out: OutputStream?
        try {
            val am = context.assets
            Log.i(LOGTAG, "OS Architecture: $arch")
            if (arch == "x86" || arch!!.contains("i686")) {
                `in` = am.open("i686/16/cjdroute")
            } else if (arch.contains("arm64-v8a") || arch.contains("aarch64")) {
                `in` = am.open("aarch64/21/cjdroute")
            } else if (arch.contains("armeabi") || arch.contains("armv7a")) {
                `in` = am.open("armv7a/16/cjdroute")
            } else if (arch.contains("x86_64")) {
                `in` = am.open("X86_64/21/cjdroute")
            } else { //Unknown architecture
                throw AnodeUtilException("Incompatible CPU architecture")
            }
        } catch (e: IOException) {
            throw AnodeUtilException("Failed to copy cjdroute file "+e.message)
        }

        if (!cjdrouteFile.exists() ||
                arch!!.contains("i686") ||
                arch.contains("x86") ||
                arch.contains("X86_64")){
            //Copy cjdroute
            try {
                if (!cjdrouteFile.exists()) {
                    Log.i(LOGTAG,"cjdroute does not exists")
                }
                Log.i(LOGTAG,"Copying cjdroute")
                out = FileOutputStream(context.filesDir.toString() + "/cjdroute")
                val buffer = ByteArray(1024)
                var read: Int
                while (`in`.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                `in`.close()
                out.close()
                //Set permissions
                Log.i(LOGTAG, "set new cjdroute permissions")
                val file = File(context.filesDir.toString() + "/cjdroute")
                file.setExecutable(true)
            }catch (e: IOException) {
                throw AnodeUtilException("Failed to copy cjdroute file "+e.message)
            }
        }
        //Create and initialize conf file
        if (!File(context.filesDir.toString()+"/"+ CJDROUTE_CONFFILE).exists()) {
            initializeCjdrouteConfFile()
        }
    }


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
        Log.i(LOGTAG,"Generating new conf file with cjdroute...")
        val processBuilder = ProcessBuilder()
        try {
            processBuilder.command("$CJDNS_PATH/$CJDROUTE_BINFILE", "--genconf")
                    .redirectOutput(File(CJDNS_PATH, CJDROUTE_TEMPCONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)

            //Clean conf
            Log.i(LOGTAG,"Clean conf file with cjdroute")
            processBuilder.command("$CJDNS_PATH/$CJDROUTE_BINFILE", "--cleanconf")
                    .redirectInput(File(CJDNS_PATH, CJDROUTE_TEMPCONFFILE))
                    .redirectOutput(File(CJDNS_PATH, CJDROUTE_CONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to generate new configuration file "+e.message)
        }

        //Delete temp file
        Log.i(LOGTAG,"Delete temp conf file")
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
            throw AnodeUtilException("Failed to execute cjdroute "+e.message)
        }
    }

    @Throws(IOException::class)
    fun readJSONFile(filename: String): String {
        Log.i(LOGTAG, "reading $filename")
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
        Log.i(LOGTAG, "modifying conf file")
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
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to modify cjdroute.conf file "+e.message)
        }
    }

    fun getPubKey(): String {
        var pubkey = ""
        val filecontent = readJSONFile("$CJDNS_PATH/$CJDROUTE_CONFFILE")
        val json = JSONObject(filecontent)
        pubkey = json.getString("publicKey")

        return pubkey
    }

    fun logFile() {
        val filename: String = "$CJDNS_PATH/anodevpn.log"
        if (File(filename).exists()) {
            if (File("$CJDNS_PATH/last_anodevpn.log").exists()) {
                Files.delete(Paths.get("$CJDNS_PATH/last_anodevpn.log"))
            }
            Files.move(Paths.get(filename),Paths.get("$CJDNS_PATH/last_anodevpn.log"))
        }
        val command = "logcat -f $filename -v time co.anode.anodevpn:V"
        try {
            Runtime.getRuntime().exec(command)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}





class AnodeUtilException(message:String): Exception(message)