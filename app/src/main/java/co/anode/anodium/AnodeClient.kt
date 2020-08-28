package co.anode.anodium

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.AsyncTask
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.SocketTimeoutException
import java.net.URL
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import javax.net.ssl.HttpsURLConnection
import kotlin.collections.ArrayList


object AnodeClient {
    lateinit var mycontext: Context
    lateinit var statustv: TextView
    lateinit var connectButton: Button
    private const val API_VERSION = "0.3"
    private const val FILE_BASE_PATH = "file://"
    private const val PROVIDER_PATH = ".provider"
    private const val API_ERROR_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/clients/events/"
    private const val API_REGISTRATION_URL = "https://api.anode.co/api/$API_VERSION/vpn/client/accounts/"
    private const val API_UPDATE_APK = "https://vpn.anode.co/api/$API_VERSION/vpn/clients/versions/android/"
    private const val API_PUBLICKEY_REGISTRATION = "https://vpn.anode.co/api/$API_VERSION/vpn/clients/publickeys/"
    private const val API_AUTH_VPN = "https://vpn.anode.co/api/$API_VERSION/vpn/servers/"
    private const val Auth_TIMEOUT = 1000*60*60 //1 hour in millis
    private var notifyUser = false
    var downloadFails = 0
    val h = Handler()

    fun init(context: Context, textview: TextView, button: Button)  {
        mycontext = context
        statustv = textview
        connectButton = button
    }

    fun stackString(e: Throwable): String {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    fun httpPostError(dir: File): String {
        try {
            if (!dir.exists()) { return "No log files to be submitted" }
            val files = dir.listFiles { file -> file.name.startsWith("error-uploadme-") }
            if (files.isEmpty()) { return "No log files to be submitted" }
            val file = files.random()

            val resp = APIHttpReq(API_ERROR_URL,file.readText(), "POST", false, false)

            try {
                val json = JSONObject(resp)
                if (json.has("status") and (json.getString("status") == "success")) {
                    // ok, it looks like everything worked, we can delete the file now
                    Log.e(LOGTAG, "Log submitted successfully ${file.name}")
                    file.delete()
                    return "Log submitted successfully"
                }
                else if (json.has("status") and (json.getString("status") != "success")) {
                    Log.e(LOGTAG, "Error invalid status posting ${file.name}: $resp")
                    return "Error invalid status posting ${file.name}: $resp"
                } else {
                    return "Error posting ${file.name} to server: $resp"
                }
            } catch (e:Exception) {
                Log.e(LOGTAG, "Error posting ${file.name}: $resp")
                return "Error posting ${file.name}: $resp"
            }
        } catch (e: Exception) {
            Log.e(LOGTAG, "Error reporting error: ${e.message}\n${stackString(e)}")
            return "Error reporting error: ${e.message}\n${stackString(e)}"
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

    fun storeError(ctx: Context, type: String, e: Throwable) {
        val err = errorJsonObj(ctx, type, e).toString(1)
        val fname = "error-uploadme-" + Instant.now().toEpochMilli().toString() + ".json"
        File(ctx.filesDir,fname).appendText(err)
        Log.e(LOGTAG, "Logged error [${e.message}] to file $fname")
    }

    fun errorCount(ctx: Context): Int {
        return ctx.filesDir.listFiles { file ->
            file.name.startsWith("error-uploadme-")
        }.size
    }

    fun ignoreErr(l: ()->Unit) = try { l() } catch (t: Throwable) { }

    @Throws(JSONException::class)
    private fun errorJsonObj(ctx: Context, type: String, err: Throwable): JSONObject {
        val anodeUtil: AnodeUtil = AnodeUtil(ctx)
        val jsonObject = JSONObject()
        var pubkey = ""
        ignoreErr{ pubkey = anodeUtil.getPubKey() }
        if (pubkey == "") pubkey = "unknown"
        jsonObject.accumulate("publicKey", pubkey)
        jsonObject.accumulate("error", type)
        jsonObject.accumulate("clientSoftwareVersion", BuildConfig.VERSION_CODE)
        jsonObject.accumulate("clientOs", "Android")
        jsonObject.accumulate("clientOsVersion", android.os.Build.VERSION.RELEASE)
        ignoreErr{ jsonObject.accumulate("localTimestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now())) }
        ignoreErr{ jsonObject.accumulate("ip4Address", CjdnsSocket.ipv4Address) }
        ignoreErr{ jsonObject.accumulate("ip6Address", CjdnsSocket.ipv6Route) }
        ignoreErr{ jsonObject.accumulate("cpuUtilizationPercent", anodeUtil.readCPUUsage().toString()) }
        ignoreErr{ jsonObject.accumulate("availableMemoryBytes", anodeUtil.readMemUsage()) }
        val prefs = mycontext.getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
        val username = prefs!!.getString("username","")
        ignoreErr{ jsonObject.accumulate("username", username) }
        jsonObject.accumulate("message", err.message)
        val cjdroutelogfile = File(anodeUtil.CJDNS_PATH+"/"+ anodeUtil.CJDROUTE_LOG)
        val lastlogfile = File(anodeUtil.CJDNS_PATH+"/last_anodevpn.log")
        val currlogfile = File(anodeUtil.CJDNS_PATH+"/anodium.log")
        var debugmsg = "";
        ignoreErr {
            debugmsg += "Error stack: " + stackString(err) + "\n";
        }
        ignoreErr {
            debugmsg += "peerStats: " + CjdnsSocket.logpeerStats + "\nshowConnections: " + CjdnsSocket.logshowConnections
        }
        if (err.message!! == "Submit logs")
        {
            ignoreErr {
                if (currlogfile.exists()) jsonObject.accumulate("newAndroidLog", currlogfile.readText(Charsets.UTF_8))
            }
            ignoreErr {
                if (lastlogfile.exists()) jsonObject.accumulate("previousAndroidLog", lastlogfile.readText(Charsets.UTF_8))
            }
            ignoreErr {
                if (cjdroutelogfile.exists()) debugmsg += "\n\nCDJROUTE LOG:" + cjdroutelogfile.readText(Charsets.UTF_8)
            }
        } else {
            ignoreErr {
                if (currlogfile.exists()) jsonObject.accumulate("newAndroidLog", currlogfile.readText(Charsets.UTF_8))
            }
            ignoreErr {
                if (cjdroutelogfile.exists()) debugmsg += "\n\nCDJROUTE LOG:" + cjdroutelogfile.readText(Charsets.UTF_8)
            }
        }
        jsonObject.accumulate("debuggingMessages", debugmsg)
        return jsonObject
    }




    fun deleteFiles(folder: String, ext: String) {
        val dir = File(folder)
        if (!dir.exists()) return
        val files: Array<File> = dir.listFiles()
        for (file in files) {
            if ((!file.isDirectory) and (file.endsWith(ext))) {
                file.delete()
            }
        }
    }


    fun checkNewVersion(notify: Boolean): Boolean {
        notifyUser = notify
        Log.i(LOGTAG, "Checking for latest APK")
        getLatestAPK().execute()
        return false
    }

    fun downloadFile(uri: Uri, version: String, filesize: Long): Long {
        Log.i(LOGTAG, "download file from $uri")
        val filename = "anodium-$version.apk"
        var downloadReference: Long = 0
        val downloadManager = mycontext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        var destination = mycontext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).toString() + "/"
        destination += filename
        val destinationuri = Uri.parse("$FILE_BASE_PATH$destination")
        var flag = true
        try {
            val file = File(destination)
            if (!file.exists() or (file.length() < filesize)) {
                val request = DownloadManager.Request(uri)
                //Setting title of request
                request.setTitle(filename)
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
                var query: DownloadManager.Query? = null
                query = DownloadManager.Query()
                var c: Cursor? = null
                query.setFilterByStatus(DownloadManager.STATUS_FAILED or DownloadManager.STATUS_PAUSED or DownloadManager.STATUS_SUCCESSFUL or DownloadManager.STATUS_RUNNING or DownloadManager.STATUS_PENDING)
                var downloading = true
                while (downloading) {
                    c = downloadManager.query(query)
                    if (c.moveToFirst()) {
                        var status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))
                        if (status == DownloadManager.STATUS_FAILED) {
                            flag = false
                            downloading = false
                            break
                        } else if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false
                            flag = true
                            break
                        }
                    }
                }
            }
            if (flag) {
                downloadFails = 0
                showInstallOption(destination, uri)
            } else {
                downloadFails++
                Toast.makeText(mycontext,"ERROR downloading", Toast.LENGTH_LONG).show()
            }
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
                    val filesize = json.get("fileSizeBytes")
                    return json.getString("binaryDownloadUrl")+"|"+json.get("majorNumber").toString()+"_"+json.get("minorNumber").toString()+"_"+json.get("revisionNumber").toString()+"|$filesize"
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
                    Toast.makeText(mycontext, R.string.downloading_update, Toast.LENGTH_LONG).show()
                    val url = result.split("|")[0]
                    val version = result.split("|")[1]
                    val filesize = result.split("|")[2].toLong()
                    downloadFile(Uri.parse(url), version, filesize)
                } else if (result.contains("error")) {
                    //TODO: submit it? show to user?
                    Log.i(LOGTAG, "ERROR updating APK from $result")
                } else if (result == "none"){
                    if (notifyUser) Toast.makeText(mycontext,"Application already at latest version", Toast.LENGTH_LONG).show()
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

    class AuthorizeVPN() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val pubkey = params[0]
            val jsonObject = JSONObject()
            jsonObject.accumulate("date", System.currentTimeMillis())
            val resp = APIHttpReq( "$API_AUTH_VPN$pubkey/authorize/",jsonObject.toString(), "POST", true , false)
            Log.i(LOGTAG, resp)
            return resp
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_AUTH_VPN: $result")
            //if 200 or 201 then connect to VPN
            if (result.isNullOrBlank()) {
                statustv.post(Runnable {
                    statustv.text  = "VPN Authorization failed with unknown reason"
                    statustv.setBackgroundColor(0x00000000)
                } )
            } else {
                val jsonObj = JSONObject(result)
                if (jsonObj.has("status")) {
                    if (jsonObj.getString("status") == "success") {
                        statustv.post(Runnable {
                            statustv.text  = "VPN Authorization success"
                            statustv.setBackgroundColor(0x00000000)
                        } )
                        //do not try to reconnect while re-authorization
                        val node = "cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k"
                        if (!isVpnActive()) {
                            cjdnsConnectVPN(node)
                        }
                        val prefs = mycontext.getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
                        with (prefs.edit()) {
                            putLong("LastAuthorized", System.currentTimeMillis())
                            putString("ServerPublicKey",node)
                            commit()
                        }
                    } else {
                        statustv.post(Runnable {
                            statustv.text  = "VPN Authorization failed: ${jsonObj.getString("message")}"
                            statustv.setBackgroundColor(0x00000000)
                        } )
                        val prefs = mycontext.getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
                        with (prefs.edit()) {
                            putLong("LastAuthorized", 0)
                            commit()
                        }
                        connectButton.text = "CONNECT"
                        CjdnsSocket.Core_stopTun()
                        CjdnsSocket.clearRoutes()
                        mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                    }
                }
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
            CjdnsSocket.Core_stopTun()
            mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
            mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
            statustv.post(Runnable {
                statustv.text  = "VPN Connected"
                statustv.setBackgroundColor(0xFF00FF00.toInt())
            } )
            //Start Thread for checking connection
            h.postDelayed(runnableConnection, 10000)
        } else {
            statustv.post(Runnable {
                statustv.text  = "VPN Authorization required"
                statustv.setBackgroundColor(0xFFFF0000.toInt())
            } )
            //Stop UI thread
            h.removeCallbacks(runnableConnection)
        }
    }

    fun APIHttpReq(address: String, body: String, method: String, needsAuth: Boolean,isRetry: Boolean): String {
        var useHttps = true
        Log.i(LOGTAG,"HttpReq at $address with $body and Auth:$needsAuth")
        val cjdnsServerAddress = "h.vpn.anode.co" //"[fc58:2fa:fbb9:b9ee:a4e5:e7c4:3db3:44f8]"
        var result = ""
        var url: URL

        url = URL(address)
        //if connection failed and we are connected to cjdns try with ipv6 address
        if(isRetry and isVpnActive()) {
            var cjdnsurl = address.replace("vpn.anode.co",cjdnsServerAddress)
            cjdnsurl = cjdnsurl.replace("https:","http:")
            useHttps = false
            url = URL(cjdnsurl)
        }
        val conn: HttpURLConnection
        if (useHttps)
            conn = url.openConnection() as HttpsURLConnection
        else
            conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        conn.requestMethod = method

        val bytes = body.toByteArray()
        if (needsAuth) {
            val md = MessageDigest.getInstance("SHA-256")
            val digest: ByteArray = md.digest(bytes)
            val digestStr = Base64.getEncoder().encodeToString(digest)
            val res = CjdnsSocket.Sign_sign(digestStr)
            val sig = res["signature"].str()
            conn.setRequestProperty("Authorization", "cjdns $sig")
        }
        try {
            conn.connect()
        } catch (e: SocketTimeoutException) {
            if (url.toString().contains(cjdnsServerAddress)) {
                APIHttpReq(address,body,method,needsAuth,true)
                return ""
            }
        }
        //Send body
        if (conn.requestMethod == "POST") conn.outputStream.write(bytes)

        try {
            result = conn.inputStream.bufferedReader().readText()
        } catch (e: Exception) {
            if ((conn.responseCode == 400) or (conn.responseCode == 403)){
                result = conn.responseCode.toString()+"|"+ conn.errorStream.reader().readText()
            } else {
                result = conn.responseCode.toString() + ": " + conn.responseMessage
            }
        }
        return result
    }


    object runnableConnection: Runnable {
        private var h: Handler? = null
        private var ipv4address: String? = null
        private var ipv6address: String? = null

        fun init(handler: Handler)  {
            h = handler
        }

        override fun run() {
            if (!CjdnsSocket.getCjdnsRoutes()) {
                //Disconnect
                stopThreads()
                statustv.post(Runnable {
                    statustv.text  = "VPN disconnected"
                    statustv.setBackgroundColor(0xFFFF0000.toInt())
                } )
                CjdnsSocket.Core_stopTun()
                CjdnsSocket.clearRoutes()
                mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                connectButton.text = "CONNECT"
            }
            val newip4address = CjdnsSocket.ipv4Address
            val newip6address = CjdnsSocket.ipv6Address
            //Reset VPN with new address
            if ((ipv4address != newip4address) || (ipv4address != newip4address)){
                statustv.post(Runnable {
                    statustv.text  = "VPN Reconnecting..."
                    statustv.setBackgroundColor(0xFF00FF00.toInt())
                } )
                ipv4address = newip4address
                ipv6address = newip6address
                //Restart Service
                CjdnsSocket.Core_stopTun()
                mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_DISCONNECT))
                mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction(AnodeVpnService().ACTION_CONNECT))
            } else if (ipv6address != "") {
                statustv.post(Runnable {
                    statustv.text  = "VPN Connected"
                    statustv.setBackgroundColor(0xFF00FF00.toInt())
                } )
            }
            //Check for needed authorization call
            val prefs = mycontext.getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
            val Authtimestamp = prefs.getLong("LastAuthorized",0)
            if ((System.currentTimeMillis() - Authtimestamp) > Auth_TIMEOUT) {
                AuthorizeVPN().execute("cmnkylz1dx8mx3bdxku80yw20gqmg0s9nsrusdv0psnxnfhqfmu0.k")
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

    class PostLogs() : AsyncTask<Any?, Any?, String>() {
        override fun doInBackground(objects: Array<Any?>): String? {
            if (checkNetworkConnection())
                return httpPostError( mycontext.filesDir)
            return "no connection"
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Toast.makeText(mycontext, result, Toast.LENGTH_SHORT).show()
        }
    }

    fun isVpnActive(): Boolean {
        val networkList:ArrayList<String> = ArrayList()
        try {
            for (networkInterface in Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (networkInterface.isUp()) networkList.add(networkInterface.getName())
            }
        } catch (ex: java.lang.Exception) {
            return false
        }

        return networkList.contains("tun0")
    }
}