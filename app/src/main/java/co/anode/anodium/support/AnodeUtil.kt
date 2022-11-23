package co.anode.anodium.support

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.system.Os
import android.system.Os.socket
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import co.anode.anodium.BuildConfig
import co.anode.anodium.R
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.*
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec


object AnodeUtil {
    private val LOGTAG = BuildConfig.APPLICATION_ID
    var context: Context? = null
    val ApplicationID = BuildConfig.APPLICATION_ID
    val DEFAULT_WALLET_NAME = "wallet"
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
    private var walletBalance = ""
    private var walletTransactions: JSONArray = JSONArray()
    private var walletAddress = ""
    var enablePktCubeConnection = true
    lateinit var apiController: APIController
    var isInternetSharingAvailable = false
    const val CHANNEL_ID = "anodium_channel_01"
    private var doUpdateCheck = false

    fun init(c: Context) {
        context = c
        if (context != null)
            filesDirectory = context!!.filesDir.toString()
        val service = ServiceVolley()
        apiController = APIController(service)
        if (BuildConfig.APPLICATION_ID == "co.anode.anodium") {
            doUpdateCheck = true
        }
    }

    fun isCjdnsAlive(): Boolean {
        if (this::cjdns_pb.isInitialized) return true
        else return false
    }

    fun initializeApp() {
        //Create files folder
        Log.i(BuildConfig.APPLICATION_ID, "Creating files directory")
        context!!.filesDir.mkdir()
        //Create symbolic link for cjdroute
        val filesdir = "data/data/${BuildConfig.APPLICATION_ID}/files"
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
        val pktwalletdir = File("$filesDirectory/pkt")
        if (!pktwalletdir.exists()) {
            pktwalletdir.mkdir()
        }
        //Create lnd directory
        val lnddir = File("$filesDirectory/pkt/lnd")
        if (!lnddir.exists()) {
            lnddir.mkdir()
        }
        //Copy neutrino.db
        //copyAssets()
    }

    fun setUsernameToSharedPrefs(username: String) {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        if (prefs != null) {
            prefs.edit().putString("username", username).apply()
        }
    }

    fun getUsernameFromSharedPrefs(): String {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        if (prefs != null) {
            return prefs.getString("username", "")!!
        }
        return ""
    }

    fun setPreReleaseUpgrade(value: Boolean) {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        if (prefs != null) {
            prefs.edit().putBoolean("preRelease", value).apply()
        }
    }

    fun getPreReleaseUpgrade(): Boolean {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        return prefs!!.getBoolean("preRelease", false)
    }

    fun setUseNewUi(value: Boolean) {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        if (prefs != null) {
            prefs.edit().putBoolean("useNewUI", value).apply()
        }
    }

    fun getUseNewUi(): Boolean {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        return prefs!!.getBoolean("useNewUI", false)
    }

    fun generateUsername(textview:TextView?) {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        //If there is no username stored
        if (prefs?.getString("username", "").isNullOrEmpty()) {
            //Generate a username
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var usernameResponse: String
            executor.execute {
                usernameResponse = AnodeClient.generateUsername()
                handler.post {
                    generateUsernameHandler(usernameResponse,textview)
                }
            }
        }
    }

    private fun generateUsernameHandler(result: String, textview:TextView?) {
        Log.i(BuildConfig.APPLICATION_ID,"Received from API: $result")
        if ((result.isBlank())) {
            return
        } else if (result.contains("400") || result.contains("401")) {
            val json = result.split("-")[1]
            var msg = result
            try {
                val jsonObj = JSONObject(json)
                msg = jsonObj.getString("username")
            } catch (e: JSONException) {
                msg += " Invalid JSON"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        } else if (result.contains("ERROR: ")) {
            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
        } else {
            val jsonObj = JSONObject(result)
            if (jsonObj.has("username")) {
                val username = jsonObj.getString("username")
                if (textview != null) {
                    textview.text = username
                }
                val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE)
                if (prefs != null) {
                    with (prefs.edit()) {
                        putString("username", jsonObj.getString("username"))
                        putBoolean("SignedIn", false)
                        commit()
                    }
                }
            }
        }
    }

    fun stopPld() {
        pld_pb.destroy()
    }

    fun stopCjdns() {
        cjdns_pb.destroy()
    }

    fun deleteNeutrino() {
        val neutrinoDB = File("data/data/${BuildConfig.APPLICATION_ID}/files/pkt/neutrino.db")
        if (neutrinoDB.exists()) {
            neutrinoDB.delete()
        }
    }

    fun initializeCjdrouteConfFile(secret: String) {
        generateCjdnsConfFile(secret)
        modifyJSONConfFile()
    }

    private fun generateCjdnsConfFile(secret: String) {
        Log.i(BuildConfig.APPLICATION_ID, "Generating new conf file with cjdroute...")
        val processBuilder = ProcessBuilder()
        try {
            if (secret.isNotEmpty()) {
                //Create secret file
                val filename = "$filesDirectory/secret.txt"
                File(filename).writeText(secret)
                processBuilder.command("$filesDirectory/$CJDROUTE_BINFILE", "--genconf-seed")
                    .redirectInput(File("$filesDirectory/secret.txt"))
                    .redirectOutput(File(filesDirectory, CJDROUTE_TEMPCONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)
                //delete secret file
                File(filename).delete()
            } else {
                processBuilder.command("$filesDirectory/$CJDROUTE_BINFILE", "--genconf")
                    .redirectOutput(File(filesDirectory, CJDROUTE_TEMPCONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)
            }
            //Clean conf
            Log.i(BuildConfig.APPLICATION_ID, "Clean conf file with cjdroute")
            processBuilder.command("$filesDirectory/$CJDROUTE_BINFILE", "--cleanconf")
                    .redirectInput(File(filesDirectory, CJDROUTE_TEMPCONFFILE))
                    .redirectOutput(File(filesDirectory, CJDROUTE_CONFFILE))
                    .start()
                    .waitFor(2, TimeUnit.SECONDS)

        } catch (e: Exception) {
            throw AnodeUtilException("Failed to generate new configuration file " + e.message)
        }
        //Delete temp file
        Log.i(BuildConfig.APPLICATION_ID, "Delete temp conf file")
        Files.delete(Paths.get("$filesDirectory/$CJDROUTE_TEMPCONFFILE"))
    }

    fun launchCJDNS() {
        val confFile = File("$filesDirectory/$CJDROUTE_CONFFILE")
        if (!confFile.exists()) {
            val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
            initializeCjdrouteConfFile(prefs?.getString("wallet_secret","").toString())
        }
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
            CjdnsSocket.init(filesDirectory + "/" + CJDROUTE_SOCK)
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
            val pb: ProcessBuilder = processBuilder.command("$filesDirectory/$PLD_BINFILE","--lnddir=/data/data/${BuildConfig.APPLICATION_ID}/files/pkt/lnd","--pktdir=/data/data/${BuildConfig.APPLICATION_ID}/files/pkt")
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
                    AnodeClient.PostMessage().execute("lnd", pldLines.toString(), "false")
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
        val command = "logcat -f $filename -v time ${BuildConfig.APPLICATION_ID}:V"
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

    fun restartApp() {
        if (context != null) {
            val packageManager: PackageManager = context!!.packageManager
            val intent: Intent? = packageManager.getLaunchIntentForPackage(context!!.packageName)
            val componentName = intent?.component
            val mainIntent = Intent.makeRestartActivityTask(componentName)
            context!!.startActivity(mainIntent)
            Runtime.getRuntime().exit(0)
        }
    }

    fun pushNotification(title: String, text: String) {
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
    fun storeWalletPassword(password: String, walletName: String) {
        var wallet = walletName
        if (wallet.isEmpty()) wallet = DEFAULT_WALLET_NAME
        val masterKey = getMasterKey()
        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context!!,
                "${BuildConfig.APPLICATION_ID}-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        val prefKey = wallet+"_password"
        encSharedPreferences.edit().putString(prefKey, password).apply()
    }

    fun storeWalletPin(pin: String, walletName: String) {
        var wallet = walletName
        if (wallet.isEmpty()) wallet = DEFAULT_WALLET_NAME
        val masterKey = getMasterKey()
        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context!!,
                "${BuildConfig.APPLICATION_ID}-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        val prefKey = wallet+"_pin"
        encSharedPreferences.edit().putString(prefKey, pin).apply()
    }

    fun renameEncryptedWalletPreferences(currentWalletName: String, newWalletName:String) {
        //Store Password under new wallet name
        val password = getWalletPassword(currentWalletName)
        storeWalletPassword(password, newWalletName)
        //Store PIN under new wallet name
        val pin = getWalletPin(currentWalletName)
        storeWalletPin(pin, newWalletName)
        //Remove old entries
        removeKeyFromEncSharedPreferences(currentWalletName)
    }

    fun removeEncryptedWalletPreferences(walletName: String) {
        var wallet = walletName
        if (wallet.isEmpty()) wallet = DEFAULT_WALLET_NAME
        val prefKeyPassword = wallet+"_password"
        removeKeyFromEncSharedPreferences(prefKeyPassword)
        val prefKeyPin = wallet+"_pin"
        removeKeyFromEncSharedPreferences(prefKeyPin)
    }

    fun getWalletPin(walletName:String):String {
        var wallet = walletName
        if (wallet.isEmpty()) wallet = DEFAULT_WALLET_NAME
        val prefKey = wallet+"_pin"
        return getValueFromEncSharedPreferences(prefKey)
    }

    fun getWalletPassword(walletName:String):String {
        var wallet = walletName
        if (wallet.isEmpty()) wallet = DEFAULT_WALLET_NAME
        val prefKey = wallet+"_password"
        return getValueFromEncSharedPreferences(prefKey)
    }
    /**
     * Get the stored vale for a key from
     * ecnrypted shared preferences
     * @param key:String
     * @return password:String
     */
    private fun getValueFromEncSharedPreferences(key:String): String {
        val masterKey = getMasterKey()
        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context!!,
                "${BuildConfig.APPLICATION_ID}-encrypted-sharedPreferences",
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

    private fun removeKeyFromEncSharedPreferences(key:String) {
        val masterKey = getMasterKey()
        val encSharedPreferences: SharedPreferences =
            EncryptedSharedPreferences.create(
                context!!,
                "${BuildConfig.APPLICATION_ID}-encrypted-sharedPreferences",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        encSharedPreferences.edit().remove(key).apply()
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
                KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
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

    fun md5(s: String): String? {
        val MD5 = "MD5"
        try {
            // Create MD5 Hash
            val digest: MessageDigest = MessageDigest
                .getInstance(MD5)
            digest.update(s.toByteArray())
            val messageDigest: ByteArray = digest.digest()

            // Create Hex String
            val hexString = StringBuilder()
            for (aMessageDigest in messageDigest) {
                var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
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
        val pinBytes: ByteArray = pin.toByteArray(Charsets.UTF_8)
        //encrypt iv+PIN using key
        val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
        val iv = byteArrayOf(85, 125, 30, 127, -79, 106, -90, 99, 19, -17, -49, 30, -99, 94, -123, -42)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val cipherText = cipher.doFinal(iv + pinBytes)
        //get password
        var cipherString = ""
        for (b in cipherText) {
            cipherString += String.format("%02X", b)
        }
        return cipherString
    }

    fun generateKey(pin:String): SecretKey {
        val md5Pin = md5(pin)
        val key = SecretKeySpec(md5Pin?.toByteArray(Charsets.UTF_8),"AES")
        return key
    }

    fun encrypt(value:String, pin: String): String {
        val key = generateKey(pin)
        val cipher = Cipher.getInstance("AES")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.DEFAULT).replace("\n","")
    }

    fun decrypt(value:String, pin:String): String? {
        try {
            val key = generateKey(pin)
            val cipher = Cipher.getInstance("AES")
            cipher.init(Cipher.DECRYPT_MODE, key)
            var decodedValue = Base64.decode(value, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedValue)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            //Can not decrypt password, we will deal by switching from PIN to password
            return null
        }
    }

    fun serviceThreads() {
        //Check for event log files daily
        Thread({
            val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
            Log.i(LOGTAG, "AnodeUtil.UploadEventsThread startup")
            while (true) {
                AnodeClient.ignoreErr {
                    //Check if 24 hours have passed since last log file submitted
                    if ((System.currentTimeMillis() - prefs!!.getLong("LastEventLogFileSubmitted", 0) > 86400000) or
                        (System.currentTimeMillis() - prefs.getLong("LastRatingSubmitted", 0) > 86400000)
                    ) {
                        val bEvents = File("$filesDirectory/anodium-events.log").exists()
                        val bRatings = File("$filesDirectory/anodium-rating.json").exists()
                        var timetosleep: Long = 60000
                        if ((!bEvents) or (!bRatings)) {
                            Log.d(LOGTAG, "No events or ratings to report, sleeping")
                            Thread.sleep((60 * 60000).toLong())
                        } else if (!AnodeClient.checkNetworkConnection()) {
                            // try again in 10 seconds, waiting for internet
                            Log.i(LOGTAG, "Waiting for internet connection to report events")
                            Thread.sleep(10000)
                        } else {
                            if (bEvents) {
                                Log.i(LOGTAG, "Reporting an events log file")
                                if (AnodeClient.httpPostEvent(File(filesDirectory)).contains("Error")) {
                                    timetosleep = 60000
                                } else {
                                    //Log posted, sleep for a day
                                    timetosleep = (60000 * 60 * 24).toLong()
                                }
                            }
                            if (bRatings) {
                                Log.i(LOGTAG, "Reporting ratings")
                                if (AnodeClient.httpPostRating().contains("Error")) {
                                    timetosleep = 60000
                                } else {
                                    with(prefs.edit()) {
                                        putLong("LastRatingSubmitted", System.currentTimeMillis())
                                        commit()
                                    }
                                    timetosleep = (60000 * 60 * 24).toLong()
                                }
                            }
                            Thread.sleep(timetosleep)
                        }
                    }
                    Thread.sleep((60 * 60000).toLong())
                }
            }
        }, "AnodeUtil.UploadEventsThread").start()
        //Check for uploading Errors
        Thread({
            Log.i(LOGTAG, "AnodeUtil.UploadErrorsThread startup")
            val arch = System.getProperty("os.arch")
            while (!(arch!!.contains("x86") || arch.contains("i686"))) {
                AnodeClient.ignoreErr {
                    val erCount = context?.let { AnodeClient.errorCount(it) }
                    if (erCount == 0) {
                        // Wait for errors for 30 seconds
                        Log.d(LOGTAG, "No errors to report, sleeping")
                        Thread.sleep(30000)
                    } else if (!AnodeClient.checkNetworkConnection()) {
                        // try again in a second, waiting for internet
                        Log.i(LOGTAG, "Waiting for internet connection to report $erCount errors")
                        Thread.sleep(1000)
                    } else {
                        Log.i(LOGTAG, "Reporting a random error out of $erCount")
                        if (AnodeClient.httpPostError(File(filesDirectory)).contains("Error")) {
                            // There was an error posting, lets wait 1 minute so as not to generate
                            // tons of crap
                            Log.i(LOGTAG, "Error reporting error, sleep for 60 seconds")
                        }
                        Thread.sleep(60000)
                    }
                }
            }
        }, "AnodeUtil.UploadErrorsThread").start()
        //Check for updates every 5min
        Thread({
            Log.i(LOGTAG, "AnodeUtil.CheckUpdates")
            while (doUpdateCheck) {
                AnodeClient.checkNewVersion(false)
                if (AnodeClient.downloadFails > 1) {
                    //In case of >1 failure delete old apk files and retry after 20min
                    AnodeClient.deleteFiles(context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString(), ".apk")
                    Thread.sleep((20 * 60000).toLong())
                } else if (AnodeClient.downloadingUpdate) {
                    Thread.sleep((20 * 60000).toLong())
                } else {
                    //check for new version every 1h
                    Thread.sleep((60 * 60000).toLong())
                }
            }
        }, "AnodeUtil.CheckUpdates").start()
    }

    fun deleteWallet(walletName: String) {
        val walletFile = File("$filesDirectory/pkt/$walletName.db")
        if (walletFile.exists()) {
            walletFile.delete()
        }
    }

    fun getWalletFiles():ArrayList<String> {
        val files = File("$filesDirectory/pkt").listFiles()
        var result = ArrayList<String>()
        for (file in files!!) {
            if (file.extension.equals("db") && !file.name.equals("neutrino.db")) {
                result.add(file.nameWithoutExtension)
            }
        }
        return result
    }

    fun walletExists():Boolean {
        return getWalletFiles().size > 0
    }

    fun setCacheWalletTxns(txns: JSONArray) {
        walletTransactions = txns
    }

    fun getCachedWalletTxns(): JSONArray {
        return walletTransactions
    }

    fun setCacheWalletAddress(address: String) {
        walletAddress = address
    }

    fun getCachedWalletAddress(): String {
        return walletAddress
    }

    fun getCachedWalletBalance(): String {
        return walletBalance
    }

    fun setCacheWalletBalance(balance: String) {
        walletBalance = balance
    }

    fun clearWalletCache() {
        walletTransactions = JSONArray()
        walletBalance = ""
        walletAddress = ""
    }

    fun deleteWalletChainBackupFile() {
        if (File("$filesDirectory/pkt/lnd/data/chain/pkt/mainnet/channel.backup").exists()) {
            File("$filesDirectory/pkt/lnd/data/chain/pkt/mainnet/channel.backup").delete()
        }
    }

    fun copyAssets() {
        val neutrinofile = File("$filesDirectory/pkt/neutrino.db")
        if (neutrinofile.exists()) {
            return
        }
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            val `is` = context?.assets?.open("neutrino.db")
            val os = FileOutputStream(neutrinofile)
            val buffer = ByteArray(1024)
            if (`is` != null) {
                while (`is`.read(buffer) > 0) {
                    os.write(buffer)
                    Log.d("#DB", "writing>>")
                }
            }

            os.flush()
            os.close()
            if (`is` != null) {
                `is`.close()
            }
        }
    }

    fun addCjdnsPeers() {
        //Get list of peering lines and add them as peers
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        var response: String
        executor.execute {
            response = AnodeClient.getPeeringLines()
            handler.post {
                AnodeClient.getPeeringLinesHandler(response)
            }
        }
    }

    fun isCjdnsAlreadyRunning():Boolean {
        val sendExecutor = Executors.newFixedThreadPool(1)
        val receiveExecutor = Executors.newFixedThreadPool(1)
        val socket = DatagramSocket()
        socket.soTimeout = 1000

        sendExecutor.execute {
            sendUdp(socket, "d1:q4:pinge")
        }
        val result:Future<Boolean> = receiveExecutor.submit(Callable {
            val message = ByteArray(1024)
            val packet = DatagramPacket(message, message.size)
            try {
                socket.receive(packet)
                val text = String(message, 0, packet.length)
                if (text.contains("ponge")) {
                    Log.i(LOGTAG, "Cjdns is already running")
                    return@Callable true
                } else {
                    return@Callable false
                }
            } catch (e: IOException) {
                return@Callable false
            } catch (e: SocketTimeoutException) {
                return@Callable false
            } catch (e: SocketException) {
                return@Callable false
            }
        })
        val isRunning = result.get()
        return isRunning
    }

    private fun sendUdp(udpSocket: DatagramSocket, msg: String) {
        val buf = msg.toByteArray()
        val serviceHost = InetAddress.getByName("127.0.0.1")
        val servicePort = 11234
        val packet = DatagramPacket(buf, buf.size, serviceHost, servicePort)
        udpSocket.send(packet)
    }

    fun getUsername(): String {
        val prefs = context?.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        if (prefs != null) {
            var tries = 0
            while (prefs.getString("username", "").isNullOrEmpty() && (tries < 5)) {
                AnodeUtil.generateUsername(null)
                tries++
                Thread.sleep(50)
            }

        }
        return prefs?.getString("username", "") ?: ""
    }
}

class AnodeUtilException(message: String): Exception(message)