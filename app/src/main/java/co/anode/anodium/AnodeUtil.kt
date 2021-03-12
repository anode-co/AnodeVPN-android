package co.anode.anodium

import android.content.Context
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
    val PLTD_BINFILE = "pltd"
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
        val cjdrouteFile = File("$CJDNS_PATH/$CJDROUTE_BINFILE")

        //Read architecture
        val arch = System.getProperty("os.arch")
        var `in`: InputStream
        var out: OutputStream?
        try {
            val am = context!!.assets
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
            throw AnodeUtilException("Failed to copy cjdroute file " + e.message)
        }
        /*Copy the file everytime */
        /*
        if (!cjdrouteFile.exists() ||
                arch!!.contains("i686") ||
                arch.contains("x86") ||
                arch.contains("X86_64")){
         */
            //Copy cjdroute
        try {
            if (!cjdrouteFile.exists()) {
                Log.i(LOGTAG, "cjdroute does not exists")
            } else {
                // You can't overwrite a file while the program is running,
                // but delete and replace works
                File("$CJDNS_PATH/$CJDROUTE_BINFILE").delete()
            }
            Log.i(LOGTAG, "Copying cjdroute")
            out = FileOutputStream("$CJDNS_PATH/$CJDROUTE_BINFILE")
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            out.close()
            //Set permissions
            Log.i(LOGTAG, "set new cjdroute permissions")
            val file = File("$CJDNS_PATH/$CJDROUTE_BINFILE")
            file.setExecutable(true)
        }catch (e: IOException) {
            throw AnodeUtilException("Failed to copy cjdroute file " + e.message)
        }
        //}
        //Create and initialize conf file
        if (!File(context!!.filesDir.toString() + "/" + CJDROUTE_CONFFILE).exists()) {
            initializeCjdrouteConfFile()
        }

        //Copy pltd
        val am = context!!.assets
        if (arch == "x86" || arch.contains("i686")) {
            `in` = am.open("pltd_emulator")
        } else if (arch.contains("arm64-v8a") || arch.contains("aarch64")) {
            `in` = am.open("pltd_aarch64")
        }
        out = FileOutputStream("$CJDNS_PATH/$PLTD_BINFILE")
        val buffer = ByteArray(1024)
        var read: Int
        while (`in`.read(buffer).also { read = it } != -1) {
            out.write(buffer, 0, read)
        }
        `in`.close()
        out.close()
        //Set permissions
        Log.i(LOGTAG, "set new pltd permissions")
        val file = File("$CJDNS_PATH/$PLTD_BINFILE")
        file.setExecutable(true)
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
            val p = processBuilder.start()
            p.waitFor()
            Log.e(LOGTAG, "cjdns exited with " + p.exitValue())
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to execute cjdroute " + e.message)
        }
    }

    private fun launchpltd() {
        try {
            Log.e(LOGTAG, "Launching pltd (file size: " +
                    File("$CJDNS_PATH/$PLTD_BINFILE").length() + ")")
            val processBuilder = ProcessBuilder()

            val pb: ProcessBuilder = processBuilder.command("$CJDNS_PATH/$PLTD_BINFILE")
            pb.environment()["TMPDIR"] = CJDNS_PATH
            val p = processBuilder.start()
            //p.waitFor()
            //Log.e(LOGTAG, "pltd exited with " + p.exitValue())
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

    fun readCPUUsage(): Float {
        try {
            val reader = RandomAccessFile("/proc/stat", "r")
            var load = reader.readLine()
            var toks = load.split(" +").toTypedArray() // Split on one or more spaces
            val idle1 = toks[4].toLong()
            val cpu1 = toks[2].toLong() + toks[3].toLong() + toks[5].toLong() + toks[6].toLong() + toks[7].toLong() + toks[8].toLong()
            try {
                Thread.sleep(360)
            } catch (e: java.lang.Exception) {}
            reader.seek(0)
            load = reader.readLine()
            reader.close()
            toks = load.split(" +").toTypedArray()
            val idle2 = toks[4].toLong()
            val cpu2 = toks[2].toLong() + toks[3].toLong() + toks[5].toLong() + toks[6].toLong() + toks[7].toLong() + toks[8].toLong()
            return (cpu2 - cpu1).toFloat() / (cpu2 + idle2 - (cpu1 + idle1))
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        return 0f
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