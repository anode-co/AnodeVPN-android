package co.anode.anodevpn

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.security.*
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.net.ssl.HttpsURLConnection


object AnodeClient {
    lateinit var mycontext: Context
    lateinit var status: TextView
    private const val API_VERSION = "0.3"
    private const val FILE_NAME = "anodevpn-latest.apk"
    private const val FILE_BASE_PATH = "file://"
    private const val PROVIDER_PATH = ".provider"
    private const val API_ERROR_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/clients/events/"
    private const val API_REGISTRATION_URL = "https://api.anode.co/api/$API_VERSION/vpn/client/accounts/"
    private const val API_UPDATE_APK = "https://vpn.anode.co/api/$API_VERSION/vpn/clients/versions/android/"
    private const val API_PUBLICKEY_REGISTRATION = "https://vpn.anode.co/api/$API_VERSION/vpn/clients/publickeys/"
    private const val API_AUTH_VPN = "https://vpn.anode.co/api/$API_VERSION/vpn/servers/"
    val h = Handler()

    fun init(context: Context, textview: TextView)  {
        mycontext = context
        status = textview
    }

    // Returns true if there was some kind of error posting
    fun httpPostError(dir: File): Boolean {
        try {
            if (!dir.exists()) { return false; }
            val files = dir.listFiles { file ->
                file.name.startsWith("error-uploadme-")
            }
            if (files.isEmpty()) { return false; }
            val file = files[0]
            val conn = URL(API_ERROR_URL).openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            file.inputStream().copyTo(conn.outputStream, 4096)
            if (conn.responseCode != 200) {
                Log.e(LOGTAG, "Unable to post error ${file.name}: ${conn.responseMessage}")
                return true
            }
            val resp = conn.inputStream.bufferedReader().readText()
            try {
                val json = JSONObject(resp)
                if (json.getString("status") != "success") {
                    Log.e(LOGTAG, "Invalid status posting ${file.name}: $resp")
                    return true
                }
                // ok, it looks like everything worked, we can delete the file now
                file.delete()
                return false
            } catch (e:Exception) {
                Log.e(LOGTAG, "Error posting ${file.name}: $resp")
            }
        } catch (e:Exception) {
            Log.e(LOGTAG, "Error reporting error: ${e.message}")
        }
        return true
    }

    @SuppressLint("CommitPrefEdits")
    @Throws(IOException::class, JSONException::class)
    fun httpPostPubKeyRegistration(): String {
        try {
            val prefs = mycontext.getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
            val pubkeyId = prefs!!.getString("publicKeyID","")
            //Check existing pubkeyID
            if (pubkeyId.isNullOrEmpty()) {
                val keypair = generateKeys()
                val encoder = Base64.getEncoder()
                val strpubkey = encoder.encodeToString(keypair?.public?.encoded)
                val strprikey = encoder.encodeToString(keypair?.private?.encoded)
                //Store public, private keys
                with (prefs.edit()) {
                    putString("publicKey",strpubkey)
                    putString("privateKey",strprikey)
                    commit()
                }
                //Get public key ID from API
                fetchpublicKeyID().execute(strpubkey)
            }
        }catch (e:Exception) {
            return "Error: $e"
        }
        return ""
    }

    @Throws(IOException::class, JSONException::class)
    fun httpPostRegistration(email: String): String {
        try {
            val url = URL(API_REGISTRATION_URL)
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            val jsonObject = JSONObject()
            jsonObject.accumulate("email", email)
            setPostRequestContent(conn, jsonObject)
            conn.connect()
            return conn.responseMessage + ""
        }catch (e:Exception) {
            return "Error: $e"
        }
    }

    fun checkNetworkConnection() =
        with(mycontext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager) {
            getNetworkCapabilities(activeNetwork)?.run {
                hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                        || hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                        || hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                        || hasTransport(NetworkCapabilities.TRANSPORT_VPN)
            } ?: false
        }

    fun storeError(dir: File, type: String, message: String?) {
        var err = errorJsonObj(type, message).toString(1)
        val fname = "error-uploadme-" + Instant.now().toEpochMilli().toString() + ".json"
        File(dir,fname).appendText(err)
    }

    fun hasErrors(): Boolean {
        val anodeUtil: AnodeUtil = AnodeUtil(null)
        val f = File(anodeUtil.CJDNS_PATH)
        return f.exists() && f.listFiles { file ->
            Log.i(LOGTAG, "File $file")
            file.name.startsWith("error-uploadme-")
        }.isNotEmpty()
    }

    @Throws(JSONException::class)
    private fun errorJsonObj(type: String, message: String?): JSONObject {
        val anodeUtil: AnodeUtil = AnodeUtil(null)
        val jsonObject = JSONObject()
        var pubkey = anodeUtil.getPubKey()
        if (pubkey == "") pubkey = "unknown"
        jsonObject.accumulate("publicKey", pubkey)
        jsonObject.accumulate("error", type)
        jsonObject.accumulate("clientSoftwareVersion", BuildConfig.VERSION_CODE)
        jsonObject.accumulate("clientOs", "Android")
        jsonObject.accumulate("clientOsVersion", android.os.Build.VERSION.RELEASE)
        jsonObject.accumulate("localTimestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        jsonObject.accumulate("ip4Address", CjdnsSocket.ipv4Address)
        jsonObject.accumulate("ip6Address", CjdnsSocket.ipv6Route)
        jsonObject.accumulate("cpuUtilizationPercent", anodeUtil.readCPUUsage().toString())
        jsonObject.accumulate("availableMemoryBytes", anodeUtil.readMemUsage())
        jsonObject.accumulate("message", message)
        val cjdroutelogfile = File(anodeUtil.CJDNS_PATH+"/"+ anodeUtil.CJDROUTE_LOG)
        val lastlogfile = File(anodeUtil.CJDNS_PATH+"/last_anodevpn.log")
        val currlogfile = File(anodeUtil.CJDNS_PATH+"/anodevpn.log")
        var debugmsg = "peerStats: "+CjdnsSocket.logpeerStats+"\nshowConnections: "+CjdnsSocket.logshowConnections
        if (message!! == "Submit logs")
        {
            if(currlogfile.exists()) jsonObject.accumulate("new_android_log", currlogfile.readText(Charsets.UTF_8))
            if(lastlogfile.exists()) jsonObject.accumulate("previous_android_log", lastlogfile.readText(Charsets.UTF_8))
            if(cjdroutelogfile.exists()) debugmsg += "\n\nCDJROUTE LOG:"+cjdroutelogfile.readText(Charsets.UTF_8)
        } else {
            if(currlogfile.exists()) jsonObject.accumulate("new_android_log", currlogfile.readText(Charsets.UTF_8))
            if(cjdroutelogfile.exists()) debugmsg += "\n\nCDJROUTE LOG:"+cjdroutelogfile.readText(Charsets.UTF_8)
        }
        jsonObject.accumulate("debuggingMessages", debugmsg)
        return jsonObject
    }

    @Throws(IOException::class)
    fun setPostRequestContent(conn: HttpURLConnection, jsonObject: JSONObject) {
        val os = conn.outputStream
        val writer = BufferedWriter(OutputStreamWriter(os, "UTF-8"))
        writer.write(jsonObject.toString())
        Log.i(MainActivity::class.java.toString(), jsonObject.toString())
        writer.flush()
        writer.close()
        os.close()
    }

    fun checkNewVersion(): Boolean {
        Log.i(LOGTAG, "Checking for latest APK")
        getLatestAPK().execute()
        return false
    }

    fun downloadFile(uri: Uri): Long {
        Log.i(LOGTAG, "download file from $uri")
        var downloadReference: Long = 0
        val downloadManager = mycontext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var destination = mycontext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += FILE_NAME
        val destinationuri = Uri.parse("${FILE_BASE_PATH}$destination")
        try {
            val file = File(destination)
            if (file.exists()) file.delete()
            val request = DownloadManager.Request(uri)
            //Setting title of request
            request.setTitle(FILE_NAME)
            request.setMimeType("application/vnd.android.package-archive")
            //Setting description of request
            request.setDescription("Your file is downloading")
            //set notification when download completed
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            //Set the local destination for the downloaded file to a path within the application's external files directory
            //request.setDestinationInExternalPublicDir(destination, FILE_NAME) //WORKS
            request.setDestinationUri(destinationuri)
            //Enqueue download and save the referenceId
            downloadReference = downloadManager.enqueue(request)

            showInstallOption(destination, uri)
        } catch (e: IllegalArgumentException) {
            Log.e(LOGTAG, "ERROR downloading file ${e.message}")
            Toast.makeText(mycontext,"ERROR downloading file ${e.message}", Toast.LENGTH_LONG).show()
        }
        return downloadReference
    }

    fun showInstallOption(
            destination: String,
            uri: Uri
    ) {
        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(
                    context: Context,
                    intent: Intent
            ) {
                val contentUri = FileProvider.getUriForFile(
                        context,
                        context.applicationContext.packageName + PROVIDER_PATH,
                        File(destination)
                )
                Log.i(LOGTAG, "Installing new apk")
                val install = Intent(Intent.ACTION_VIEW)
                install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                install.data = contentUri
                context.startActivity(install)
                context.unregisterReceiver(this)
            }
        }
        mycontext.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    class getLatestAPK(): AsyncTask<Any?, Any?, String>() {
        override fun doInBackground(objects: Array<Any?>): String? {
            Log.i(LOGTAG,"Checking for latest APK")
            var result = ""
            val versionCode = BuildConfig.VERSION_CODE
            val versionName = BuildConfig.VERSION_NAME
            val major_number = versionName.split(".")[0].toInt()
            val minor_number = versionName.split(".")[1].toInt()
            val revision_number = versionName.split(".")[2].toInt()
            try {
                val json = JSONObject(URL(API_UPDATE_APK).readText(Charsets.UTF_8))
                if (json.get("clientOs") != "android") {
                    result = "wrong os returned"
                    return result
                }
                //TODO: check for client_cpu_architecture
                if ((json.get("majorNumber").toString().toInt() > major_number) ||
                        ((json.get("majorNumber").toString().toInt() == major_number) &&
                                (json.get("minorNumber").toString().toInt() > minor_number)) ||
                        ((json.get("majorNumber").toString().toInt() == major_number) &&
                                (json.get("minorNumber").toString().toInt() == minor_number) &&
                                (json.get("revisionNumber").toString().toInt() > revision_number))
                ){
                    return json.getString("binaryDownloadUrl")
                } else {
                    Log.i(LOGTAG,"NO update needed")
                    return "none"
                }
            } catch (e: Exception) {
                result = "error in getting update information"
                Log.i(LOGTAG, result)
            }
            return result
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result != null) {
                if (result.contains("http")) {
                    Log.i(LOGTAG, "Updating APK from $result")
                    Toast.makeText(mycontext,R.string.downloading_update, Toast.LENGTH_LONG).show()
                    downloadFile(Uri.parse(result))
                } else if (result.contains("error")) {
                    //TODO: submit it? show to user?
                    Log.i(LOGTAG, "ERROR updating APK from $result")
                } else {
                    //DO NOTHING
                }
            }
        }
    }

    fun generateKeys(): KeyPair? {
        Log.i(LOGTAG,"Generating key pair")
        try {
            // creating the object of KeyPairGenerator
            val kpg = KeyPairGenerator.getInstance("RSA")
            // initializing with 1024
            kpg.initialize(1024)
            // getting key pairs
            return kpg.generateKeyPair()
        } catch (e: NoSuchAlgorithmException) {
            Log.e(LOGTAG,"ERROR - Failed to generate key pair: $e")
            return null
        }
    }

    class fetchpublicKeyID() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            return try {
                var result = StringBuilder()
                val url = URL(API_PUBLICKEY_REGISTRATION)
                val conn = url.openConnection() as HttpsURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                val jsonObject = JSONObject()
                jsonObject.accumulate("publicKey", "-----BEGIN PUBLIC KEY-----\n"+params[0]+"\n-----END PUBLIC KEY-----")
                jsonObject.accumulate("algorithm", "rsa-sha256")
                setPostRequestContent(conn, jsonObject)
                conn.connect()
                val `in`: InputStream = BufferedInputStream(conn.getInputStream())
                val reader = BufferedReader(InputStreamReader(`in`))

                var line = reader.readLine()
                while (!line.isNullOrEmpty()) {
                    result.append(line);
                    line = reader.readLine()
                }

                return result.toString()
            } catch (e: Exception) {
                Log.i(LOGTAG,"Failed to get publick key ID from API $e")
                null
            }
        }

        @SuppressLint("CommitPrefEdits")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result.isNullOrBlank()) {
                return
            }
            val jsonObj = JSONObject(result)
            val pubkeyID = jsonObj.getString("publicKeyId")
            val prefs = mycontext.getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
            //Store pubkeyID
            with (prefs.edit()) {
                putString("publicKeyID",pubkeyID)
                commit()
            }

            //sendAuthTest()
        }
    }

    class AuthorizeVPN() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val pubkey = params[0]
            val jsonObject = JSONObject()
            //jsonObject.accumulate("clientPublicKey", "lbqr0rzyc2tuysw3w8gfr95u68kujzlq7zht5hyf452u8yshr120.k")
            jsonObject.accumulate("date", System.currentTimeMillis())
            val resp = AuthVPNHttpReq(pubkey!!, jsonObject.toString())
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_AUTH_VPN: $result")
            //if 200 or 201 then connect to VPN
            if (result!!.contains("200:") || result!!.contains("201:")) {
                cjdnsConnectVPN("cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
            } else if (result.isNullOrBlank() || result.contains(":")) {
                Toast.makeText(mycontext, result, Toast.LENGTH_LONG).show()
            } else {
                try {
                    val jsonObj = JSONObject(result)
                    val status = jsonObj.getString("status")
                    val expiresAt = jsonObj.getString("expiresAt")
                }catch (e: java.lang.Exception) {}
            }
        }
    }

    fun cjdnsConnectVPN(node: String) {
        var iconnected: Boolean = false
        runnableConnection.init(h)
        //Connect to Internet
        CjdnsSocket.IpTunnel_connectTo(node)
        var tries = 0
        //Check for ip address given by cjdns try for 20 times, 10secs
        while (!iconnected && (tries < 10)) {
            iconnected = CjdnsSocket.getCjdnsRoutes()
            tries++
            Thread.sleep(2000)
        }
        if (iconnected) {
            //Restart Service
            //CjdnsSocket.Core_stopTun()
            mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
            mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
            status.post(Runnable {
                status.text  = "VPN Connected"
                status.setBackgroundColor(0xFF00FF00.toInt())
            } )
            //Start Thread for checking connection
            h.postDelayed(runnableConnection, 10000)
        } else {
            status.post(Runnable {
                status.text  = "VPN Authorization required"
                status.setBackgroundColor(0xFFFF0000.toInt())
            } )
            //Stop UI thread
            h.removeCallbacks(runnableConnection)
        }
    }

    fun httpAuthReq(urladdr: String, str: String, method: String): String {
        val url = URL(urladdr) // TODO: set to (url)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = str.toByteArray()
        val digest: ByteArray = md.digest(bytes)
        val digestStr = Base64.getEncoder().encodeToString(digest)
        val res = CjdnsSocket.Sign_sign(digestStr)
        var sig = res["signature"].str()
        conn.setRequestProperty("Authorization", "cjdns $sig")
        conn.connect()
        if (method == "POST") conn.outputStream.write(bytes)

        if (conn.responseCode == 400) {
            //email exists
            return "exists"
        } else if ((conn.responseCode == 201) || (conn.responseCode == 200)) {
            return conn.inputStream.bufferedReader().readText()
        }
        return ""
    }

    //fun AuthVPNHttpReq(ServerPubkey: String): String {
    fun AuthVPNHttpReq(ServerPubkey: String, body: String): String {
        //val str = ""
        val url = URL("$API_AUTH_VPN$ServerPubkey/authorize/")
        //val url = URL(url)
        val conn = url.openConnection() as HttpURLConnection
        //conn.requestMethod = "GET"
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = body.toByteArray()
        val digest: ByteArray = md.digest(bytes)
        val digestStr = Base64.getEncoder().encodeToString(digest)
        val res = CjdnsSocket.Sign_sign(digestStr)
        val sig = res["signature"].str()
        conn.setRequestProperty("Authorization", "cjdns $sig")
        conn.connect()
        conn.outputStream.write(bytes)
        if (conn.responseCode == 201) {
            //Created
            return conn.responseMessage
        } else if (conn.responseCode == 200) {
            //address renewed
            return "200:Renewed"
        } else if (conn.responseCode == 408) {
            //timed out
            return "408:Timed out"
        } else if (conn.responseCode == 404) {
            //not registered with the API
            return "404:Not registered"
        } else if ((conn.responseCode == 403) || ((conn.responseCode == 401))){
            //client not authorized
            return "403:Not Authorized"
        } else if (conn.responseCode == 503) {
            //VPN server out of available addresses
            return "503:Not available addresses"
        }
        return ""
    }

    object runnableConnection: Runnable {
        private var h: Handler? = null
        private var ipv4address: String? = null
        private var ipv6address: String? = null

        fun init(handler: Handler)  {
            h = handler
        }

        override fun run() {
            CjdnsSocket.getCjdnsRoutes()
            val newip4address = CjdnsSocket.ipv4Address
            val newip6address = CjdnsSocket.ipv6Address
            //Reset VPN with new address
            if ((ipv4address != newip4address) || (ipv4address != newip4address)){
                status.post(Runnable {
                    status.text  = "VPN Reconnecting..."
                    status.setBackgroundColor(0xFF00FF00.toInt())
                } )
                ipv4address = newip4address
                ipv6address = newip6address
                //Restart Service
                CjdnsSocket.Core_stopTun()
                mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
            }
            //GetPublicIP().execute(status)
            h!!.postDelayed(this, 10000) //ms
        }
    }

    class GetPublicIP(): AsyncTask<TextView, Void, String>() {
        var text:TextView? = null
        override fun doInBackground(vararg params: TextView?): String {
            text = params[0]
            return try {
                URL("https://api.ipify.org").readText(Charsets.UTF_8)
            } catch (e: Exception) {
                "error in getting public ip"
            }
        }

        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            text?.post(Runnable { text?.text  = "Public IP: $result" } )
        }
    }

    fun stopThreads() {
        h.removeCallbacks(runnableConnection)
    }
}