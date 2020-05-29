package co.anode.anodevpn

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
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.net.ssl.HttpsURLConnection


object AnodeClient {
    lateinit var mycontext: Context
    private const val FILE_NAME = "anodevpn-latest.apk"
    private const val FILE_BASE_PATH = "file://"
    private const val MIME_TYPE = "application/vnd.android.package-archive"
    private const val PROVIDER_PATH = ".provider"
    private const val API_ERROR_URL = "https://vpn.anode.co/api/0.2/vpn/clients/events/"
    private const val API_REGISTRATION_URL = "https://api.anode.co/api/0.3/vpn/client/accounts/"
    private const val API_UPDATE_APK = "https://vpn.anode.co/api/0.2/vpn/clients/versions/android/"
    fun init(context: Context)  {
        mycontext= context
    }

    @Throws(IOException::class, JSONException::class)
    fun httpPostError(type: String, message: String?): String {
        try {
            val apiKey = "hthiP3Gx.TLJRcZGpsHh8ImjtBCjpB7soD87qsaDb"
            val url = URL(API_ERROR_URL)
            val conn = url.openConnection() as HttpsURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            //conn.setRequestProperty("Authorization", "Api-Key $apiKey")
            val jsonObject = errorJsonObj(type, message)
            setPostRequestContent(conn, jsonObject)
            conn.connect()
            return conn.responseMessage + ""
        }catch (e:Exception) {
            return "Error: $e"
        }
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

    @Throws(JSONException::class)
    private fun requpdateJsonObj(type: String, message: String?): JSONObject {
        val jsonObject = JSONObject()
        //jsonObject.accumulate("public_key", anodeUtil.getPubKey())
        return jsonObject
    }

    @Throws(IOException::class)
    private fun setPostRequestContent(conn: HttpURLConnection, jsonObject: JSONObject) {
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
}