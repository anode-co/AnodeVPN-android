package co.anode.anodium

import android.R.attr.process
import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.system.Os
import android.util.Log
import android.view.View
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONObject
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.TimeUnit


object AnodeUtil {
    private val LOGTAG = "co.anode.anodium"
    var context: Context? = null
    var CJDNS_PATH = ""
    val CJDROUTE_SOCK = "cjdroute.sock"
    val CJDROUTE_BINFILE = "cjdroute"
    val CJDROUTE_CONFFILE = "cjdroute.conf"
    val CJDROUTE_LOG = "cjdroute.log"
    val PLD_LOG = "pld.log"
    val PLD_BINFILE = "pld"
    var isPldRunning = false
    lateinit var pld_pb: Process
    lateinit var cjdns_pb: Process
    private val CJDROUTE_TEMPCONFFILE = "tempcjdroute.conf"
    private val clickInterval = 1000L

    fun init(c: Context) {
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
        //Create symbolic link for pld
        val pldfile = File("$filesdir/pld")
        pldfile.delete()
        oldPath = "$nativelibdir/libpld.so"
        newPath = "$filesdir/pld"
        Os.symlink(oldPath,newPath)
        //Create needed directories
        val pktwalletdir = File("$CJDNS_PATH/.pktwallet")
        if (!pktwalletdir.exists()) {
            pktwalletdir.mkdir()
        }
        val pktdir = File("$CJDNS_PATH/.pktwallet/pkt")
        if (!pktdir.exists()) {
            pktdir.mkdir()
        }
        val mainnetdir = File("$CJDNS_PATH/.pktwallet/pkt/mainnet")
        if (!mainnetdir.exists()) {
            mainnetdir.mkdir()
        }
        //Create lnd directory
        val lnddir = File("$CJDNS_PATH/.pktwallet/lnd")
        if (!lnddir.exists()) {
            lnddir.mkdir()
        }
    }


    fun launch() {
        launchPld()
        val confFile = File("$CJDNS_PATH/$CJDROUTE_CONFFILE")
        if (!confFile.exists()) {
            initializeCjdrouteConfFile()
        }
        launchCJDNS()
    }

    fun stopPld() {
        pld_pb.destroy()
    }

    fun deleteNeutrino() {
        val neutrinoDB = File("data/data/co.anode.anodium/files/pkt/neutrino.db")
        if (neutrinoDB.exists()) {
            neutrinoDB.delete()
        }
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

    private fun launchPld() {
        try {
            Log.e(LOGTAG, "Launching pld (file size: " +
                    File("$CJDNS_PATH/$PLD_BINFILE").length() + ")")
            val processBuilder = ProcessBuilder()
            val pb: ProcessBuilder = processBuilder.command("$CJDNS_PATH/$PLD_BINFILE","--no-macaroons","--lnddir=/data/data/co.anode.anodium/files/pkt/lnd","--pktdir=/data/data/co.anode.anodium/files/pkt")
                    .redirectOutput(File(CJDNS_PATH, PLD_LOG))
                    .redirectErrorStream(true)
            pb.environment()["TMPDIR"] = CJDNS_PATH
            pld_pb = processBuilder.start()
            isPldRunning = true
            Thread( Runnable {
                //If pld fails, send error msg and relaunch it
                pld_pb.waitFor()
                isPldRunning = false
                //Get last 100 lines from log and post it
                var pldLines = File(CJDNS_PATH+"/"+PLD_LOG).readLines()
                if (pldLines.size > 100) {
                    pldLines = pldLines.drop(pldLines.size - 100)
                }
                AnodeClient.httpPostMessage("lnd", pldLines.toString())
                launchPld()
            }).start()
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to execute pld " + e.message)
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
        val pubkey: String
        val fileContent = readJSONFile("$CJDNS_PATH/$CJDROUTE_CONFFILE")
        val json = JSONObject(fileContent)
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
        val runtime = Runtime.getRuntime()
        val usedMemInMB=(runtime.totalMemory() - runtime.freeMemory()) / 1048576L
        val maxHeapSizeInMB=runtime.maxMemory() / 1048576L
        return (maxHeapSizeInMB - usedMemInMB).toString()
    }

    fun eventLog(message: String) {
        val logFile = File("$CJDNS_PATH/anodium-events.log")
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
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

    fun preventTwoClick(view: View) {
        view.setEnabled(false)
        view.postDelayed(
            { view.setEnabled(true) },
            clickInterval
        )
    }

    /**
     * Converts satoshis to PKT divisions
     * PKT, mPKT, uPKT and nPKT and return it as string
     *
     * @param Long
     * @return String
     */
    fun satoshisToPKT(satoshis: Long):String {
        var amount = satoshis.toFloat()
        val onePKT = 1073741824
        val mPKT = 1073741.824F
        val uPKT  = 1073.741824F
        val nPKT  = 1.073741824F
        if (amount > 1000000000) {
            amount /= onePKT
            return "%.2f PKT".format(amount)
        } else if (amount > 1000000) {
            amount /= mPKT
            return "%.2f mPKT".format(amount)
        } else if (amount > 1000) {
            amount /= uPKT
            return "%.2f μPKT".format(amount)
        } else if (amount < 1000000000) {
            amount /= onePKT
            return "%.2f PKT".format(amount)
        } else if (amount < 1000000) {
            amount /= mPKT
            return "%.2f mPKT".format(amount)
        } else if (amount < 1000) {
            amount /= uPKT
            return "%.2f μPKT".format(amount)
        } else {
            amount /= nPKT
            return "%.2f nPKT".format(amount)
        }
    }

    /**
     * Stores a password to
     * ecnrypted shared preferences
     *
     * @param password: String
     */
    fun storePassword(password: String) {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        val masterKey = MasterKey.Builder(context!!)
            .setKeyGenParameterSpec(spec)
            .build()

        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context!!,
                "co.anode.anodium-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        encSharedPreferences.edit().putString("wallet_password", password).apply()
    }

    /**
     * Get the stored password from
     * ecnrypted shared preferences
     *
     * @return password: String
     */
    fun getPasswordFromEncSharedPreferences(): String {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        val masterKey = MasterKey.Builder(context!!)
            .setKeyGenParameterSpec(spec)
            .build()
        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context!!,
                "co.anode.anodium-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        val password : String? = encSharedPreferences.getString("wallet_password", "")
        if (password.isNullOrEmpty()) {
            return ""
        } else {
            return password
        }
    }
}

class AnodeUtilException(message: String): Exception(message)