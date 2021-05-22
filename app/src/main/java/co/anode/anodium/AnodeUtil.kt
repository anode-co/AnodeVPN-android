package co.anode.anodium

import android.content.Context
import android.system.Os
import android.util.Log
import org.json.JSONObject
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


class AnodeUtil(c: Context?) {
    private val LOGTAG = "co.anode.anodium"
    var context: Context? = null
    var CJDNS_PATH = ""
    val CJDROUTE_SOCK = "cjdroute.sock"
    val CJDROUTE_BINFILE = "cjdroute"
    val CJDROUTE_CONFFILE = "cjdroute.conf"
    val CJDROUTE_LOG = "cjdroute.log"
    val PLTD_LOG = "pltd.log"
    val PLTD_BINFILE = "pltd"
    lateinit var pltd_pb: Process
    lateinit var cjdns_pb: Process
    private val CJDROUTE_TEMPCONFFILE = "tempcjdroute.conf"

    init {
        context = c
        if (context != null)
            CJDNS_PATH = context!!.filesDir.toString()
    }

    fun initializeApp() {
        //Create files folder
        Log.i(LOGTAG, "Creating files directory")
        context!!.filesDir.mkdir()
        //Create symbolic link for cjdroute
        val filesdir = "data/data/co.anode.anodium/files"
        val nativelibdir = context!!.applicationInfo.nativeLibraryDir.toString()
        val cjdrouteFile = File("$filesdir/cjdroute")
        cjdrouteFile.delete()
        var oldPath = "$nativelibdir/libcjdroute.so"
        var newPath = "$filesdir/cjdroute"
        Os.symlink(oldPath, newPath)
        //Create symbolic link for pltd
        val pltdfile = File("$filesdir/pltd")
        pltdfile.delete()
        oldPath = "$nativelibdir/libpltd.so"
        newPath = "$filesdir/pltd"
        Os.symlink(oldPath,newPath)


        //Create lnd directory
        val lnddir = File("$CJDNS_PATH/lnd")
        if (!lnddir.exists()) {
            lnddir.mkdir()
        }
        //Copy tls files
        var `in`: InputStream
        var out: OutputStream?
        val am = context!!.assets
        try {
            val buffer = ByteArray(1024)
            var read: Int
            val tlscert = File("$CJDNS_PATH/lnd/tls.cert")
            if (!tlscert.exists()) {
                `in` = am.open("tls.cert")
                out = FileOutputStream("$CJDNS_PATH/lnd/tls.cert")
                while (`in`.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                `in`.close()
                out.close()
            }
            val tlskey = File("$CJDNS_PATH/lnd/tls.key")
            if (!tlskey.exists()) {
                `in` = am.open("tls.key")
                out = FileOutputStream("$CJDNS_PATH/lnd/tls.key")
                while (`in`.read(buffer).also { read = it } != -1) {
                    out.write(buffer, 0, read)
                }
                `in`.close()
                out.close()
            }
        }catch (e: IOException) {
            throw AnodeUtilException("Failed to copy pltd or tls files " + e.message)
        }
    }


    fun launch() {
        val confFile = File("$CJDNS_PATH/$CJDROUTE_CONFFILE")
        if (!confFile.exists()) {
            initializeCjdrouteConfFile()
        }
        launchCJDNS()
        launchpltd()
    }


    fun initializeCjdrouteConfFile() {
        generateConfFile()
        modifyJSONConfFile()
    }

    private fun generateConfFile() {
        Log.i(LOGTAG, "Generating new conf file with cjdroute...")
        val processBuilder = ProcessBuilder()
        try {
            processBuilder.command("$CJDNS_PATH/$CJDROUTE_BINFILE", "--genconf")
                    .redirectOutput(File(CJDNS_PATH, CJDROUTE_TEMPCONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)

            //Clean conf
            Log.i(LOGTAG, "Clean conf file with cjdroute")
            processBuilder.command("$CJDNS_PATH/$CJDROUTE_BINFILE", "--cleanconf")
                    .redirectInput(File(CJDNS_PATH, CJDROUTE_TEMPCONFFILE))
                    .redirectOutput(File(CJDNS_PATH, CJDROUTE_CONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to generate new configuration file " + e.message)
        }
        //Delete temp file
        Log.i(LOGTAG, "Delete temp conf file")
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
            cjdns_pb = processBuilder.start()
            cjdns_pb.waitFor()
            Log.e(LOGTAG, "cjdns exited with " + cjdns_pb.exitValue())
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to execute cjdroute " + e.message)
        }
    }

    private fun launchpltd() {
        try {
            Log.e(LOGTAG, "Launching pltd (file size: " +
                    File("$CJDNS_PATH/$PLTD_BINFILE").length() + ")")
            val processBuilder = ProcessBuilder()

            val pb: ProcessBuilder = processBuilder.command("$CJDNS_PATH/$PLTD_BINFILE","--notls","--no-macaroons","--lnddir=/data/data/co.anode.anodium/files/lnd","--configfile=/data/data/co.anode.anodium/files/config.go","--datadir=/data/data/co.anode.anodium/files/lnddata")
                    .redirectOutput(File(CJDNS_PATH, PLTD_LOG))
                    .redirectErrorStream(true)
            pb.environment()["TMPDIR"] = CJDNS_PATH
            pltd_pb = processBuilder.start()
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to execute pltd " + e.message)
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
            //Add tunfd and tunnel socket
            val router = json.getJSONObject("router")
            val interf = router.getJSONObject("interface")
            interf.put("tunfd", "android")
            val security = json.getJSONArray("security")
            if (security.getJSONObject(3).has("noforks"))
                security.getJSONObject(3).put("noforks", 0)
            //Save file
            val writer = BufferedWriter(FileWriter("$CJDNS_PATH/$CJDROUTE_CONFFILE"))
            val out = json.toString().replace("\\/", "/")
            writer.write(out)
            writer.close()
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to modify cjdroute.conf file " + e.message)
        }
    }

    fun getPubKey(): String {
        var pubkey: String
        val filecontent = readJSONFile("$CJDNS_PATH/$CJDROUTE_CONFFILE")
        val json = JSONObject(filecontent)
        pubkey = json.getString("publicKey")
        return pubkey
    }

    fun logFile() {
        val filename: String = "$CJDNS_PATH/anodium.log"
        if (File("$CJDNS_PATH/last_anodium.log").exists())  Files.delete(Paths.get("$CJDNS_PATH/last_anodium.log"))
        if (File(filename).exists()) Files.move(Paths.get(filename), Paths.get("$CJDNS_PATH/last_anodium.log"))
        val command = "logcat -f $filename -v time co.anode.anodium:V"
        try {
            Runtime.getRuntime().exec(command)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun readMemUsage(): String {
        val runtime = Runtime.getRuntime();
        val usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L;
        val maxHeapSizeInMB=runtime.maxMemory() / 1048576L;
        return (maxHeapSizeInMB - usedMemInMB).toString();
    }

    fun eventLog(message: String) {
        val logFile = File("$CJDNS_PATH/anodium-events.log")
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append(message)
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

class AnodeUtilException(message: String): Exception(message)