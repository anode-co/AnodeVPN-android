package co.anode.anodium.support

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.system.Os
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.anode.anodium.R
import org.json.JSONObject
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.concurrent.thread


object AnodeUtil {
    private val LOGTAG = "co.anode.anodium"
    var context: Context? = null
    var filesDirectory = ""
    val CJDROUTE_SOCK = "cjdroute.sock"
    val CJDROUTE_BINFILE = "cjdroute"
    val CJDROUTE_CONFFILE = "cjdroute.conf"
    val CJDROUTE_LOG = "cjdroute.log"
    val PLD_LOG = "pldlog.txt"
    val PLD_BINFILE = "pld"
    lateinit var pld_pb: Process
    lateinit var cjdns_pb: Process
    private val CJDROUTE_TEMPCONFFILE = "tempcjdroute.conf"
    private val clickInterval = 1000L

    fun init(c: Context) {
        context = c
        if (context != null)
            filesDirectory = context!!.filesDir.toString()
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
        val pktwalletdir = File("$filesDirectory/.pktwallet")
        if (!pktwalletdir.exists()) {
            pktwalletdir.mkdir()
        }
        val pktdir = File("$filesDirectory/.pktwallet/pkt")
        if (!pktdir.exists()) {
            pktdir.mkdir()
        }
        val mainnetdir = File("$filesDirectory/.pktwallet/pkt/mainnet")
        if (!mainnetdir.exists()) {
            mainnetdir.mkdir()
        }
        //Create lnd directory
        val lnddir = File("$filesDirectory/.pktwallet/lnd")
        if (!lnddir.exists()) {
            lnddir.mkdir()
        }
    }


    fun launch() {
        launchPld()
        val confFile = File("$filesDirectory/$CJDROUTE_CONFFILE")
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
            processBuilder.command("$filesDirectory/$CJDROUTE_BINFILE", "--genconf")
                    .redirectOutput(File(filesDirectory, CJDROUTE_TEMPCONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)

            //Clean conf
            Log.i(LOGTAG, "Clean conf file with cjdroute")
            processBuilder.command("$filesDirectory/$CJDROUTE_BINFILE", "--cleanconf")
                    .redirectInput(File(filesDirectory, CJDROUTE_TEMPCONFFILE))
                    .redirectOutput(File(filesDirectory, CJDROUTE_CONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to generate new configuration file " + e.message)
        }
        //Delete temp file
        Log.i(LOGTAG, "Delete temp conf file")
        Files.delete(Paths.get("$filesDirectory/$CJDROUTE_TEMPCONFFILE"))
    }

    private fun launchCJDNS() {
        try {
            Log.i(
                LOGTAG, "Launching cjdroute (file size: " +
                    File("$filesDirectory/$CJDROUTE_BINFILE").length() + ")")
            val processBuilder = ProcessBuilder()
            //Run cjdroute with existing conf file
            val pb: ProcessBuilder = processBuilder.command("$filesDirectory/$CJDROUTE_BINFILE")
                    .redirectInput(File(filesDirectory, CJDROUTE_CONFFILE))
                    .redirectOutput(File(filesDirectory, CJDROUTE_LOG))
                    .redirectErrorStream(true)
            pb.environment()["TMPDIR"] = filesDirectory
            cjdns_pb = processBuilder.start()
            cjdns_pb.waitFor()
            Log.e(LOGTAG, "cjdns exited with " + cjdns_pb.exitValue())
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to execute cjdroute " + e.message)
        }
    }

    fun launchPld() {
        try {
            if (this::pld_pb.isInitialized && pld_pb.isAlive) return
            Log.i(LOGTAG, "Launching pld (file size: " + File("$filesDirectory/$PLD_BINFILE").length() + ")")
            val pldLogFile = File(filesDirectory + "/" + PLD_LOG)
            val processBuilder = ProcessBuilder()
            val pb: ProcessBuilder = processBuilder.command("$filesDirectory/$PLD_BINFILE","--lnddir=/data/data/co.anode.anodium/files/pkt/lnd","--pktdir=/data/data/co.anode.anodium/files/pkt")
                    .redirectOutput(File(filesDirectory, PLD_LOG))
                    .redirectErrorStream(true)
            pb.environment()["TMPDIR"] = filesDirectory
            pld_pb = processBuilder.start()
            Thread( Runnable {
                //If pld fails, send error msg and relaunch it
                pld_pb.waitFor()
                //Get last 100 lines from log and post it
                if (pldLogFile.exists()) {
                    var pldLines = pldLogFile.readLines()
                    //Check if pld is already running
                    if (pldLines.contains("127.0.0.1:8080: bind: address already in use")) {
                       return@Runnable
                    }
                    if (pldLines.size > 100) {
                        pldLines = pldLines.drop(pldLines.size - 100)
                    }
                    AnodeClient.httpPostMessage("lnd", pldLines.toString(), false)
                    //Delete log file
                    pldLogFile.delete()
                }
                //Wait before restarting pld
                Thread.sleep(500)
                launchPld()
            }).start()
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to execute pld " + e.message)
        }
    }

    /**
     *
     */
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
            val filecontent = readJSONFile("$filesDirectory/$CJDROUTE_CONFFILE")
            val json = JSONObject(filecontent)
            //Add tunfd and tunnel socket
            val router = json.getJSONObject("router")
            val interf = router.getJSONObject("interface")
            interf.put("tunfd", "android")
            val security = json.getJSONArray("security")
            if (security.getJSONObject(3).has("noforks"))
                security.getJSONObject(3).put("noforks", 0)
            //Save file
            val writer = BufferedWriter(FileWriter("$filesDirectory/$CJDROUTE_CONFFILE"))
            val out = json.toString().replace("\\/", "/")
            writer.write(out)
            writer.close()
        } catch (e: Exception) {
            throw AnodeUtilException("Failed to modify cjdroute.conf file " + e.message)
        }
    }

    fun getPubKey(): String {
        val pubkey: String
        val fileContent = readJSONFile("$filesDirectory/$CJDROUTE_CONFFILE")
        val json = JSONObject(fileContent)
        pubkey = json.getString("publicKey")
        return pubkey
    }

    fun logFile() {
        val filename: String = "$filesDirectory/anodium.log"
        if (File("$filesDirectory/last_anodium.log").exists())  Files.delete(Paths.get("$filesDirectory/last_anodium.log"))
        if (File(filename).exists()) Files.move(Paths.get(filename), Paths.get("$filesDirectory/last_anodium.log"))
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
        val logFile = File("$filesDirectory/anodium-events.log")
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

    fun pushNotification(title: String, text: String) {
        val CHANNEL_ID = "anodium_channel_01"
        if (context != null) {
            var builder = NotificationCompat.Builder(context!!, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_logo_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            with(NotificationManagerCompat.from(context!!)) {
                // notificationId is a unique int for each notification that you must define
                val notificationId = 1
                notify(notificationId, builder.build())
            }
        }
    }

    fun getMasterKey(): MasterKey {
        val spec = KeyGenParameterSpec.Builder(
            MasterKey.DEFAULT_MASTER_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        return MasterKey.Builder(context!!)
            .setKeyGenParameterSpec(spec)
            .build()
    }

    /**
     * Stores a password to
     * ecnrypted shared preferences
     *
     * @param password: String
     */
    fun storePassword(password: String) {
        val masterKey = getMasterKey()
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
     * Get the stored vale for a key from
     * ecnrypted shared preferences
     * @param key:String
     * @return password:String
     */
    fun getKeyFromEncSharedPreferences(key:String): String {
        val masterKey = getMasterKey()
        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context!!,
                "co.anode.anodium-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        val value : String? = encSharedPreferences.getString(key, "")
        if (value.isNullOrEmpty()) {
            return ""
        } else {
            return value
        }
    }

    /**
     * Get key from AndroidKeyStore with alias=AnodeVPN
     *
     * @return SecretKey
     */
    fun getKey(): SecretKey {
        val keyAlias = "AnodeVPNKey"
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        if (!keyStore.containsAlias(keyAlias)) {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            keyGenerator.init(
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .setRandomizedEncryptionRequired(false)
                    .build()
            )
            return keyGenerator.generateKey()
        } else {
            val keyEntry = keyStore.getEntry(keyAlias, null) as KeyStore.SecretKeyEntry
            return keyEntry.secretKey
        }
    }

    /**
     * Will generate a password encrypted using a safe key
     *
     * @param pin:String
     * @return password:String
     */
    fun getTrustedPassword(pin:String): String {
        val key = getKey()
        //get PIN
        val pinBytes:ByteArray = pin.toByteArray(Charsets.UTF_8)
        //encrypt iv+PIN using key
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val iv = byteArrayOf(85, 125, 30, 127, -79, 106, -90, 99, 19, -17, -49, 30, -99, 94, -123, -42)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val cipherText = cipher.doFinal(iv+pinBytes)
        //get password
        var cipherString = ""
        for (b in cipherText) {
            cipherString += String.format("%02X", b)
        }
        return cipherString
    }
}

class AnodeUtilException(message: String): Exception(message)