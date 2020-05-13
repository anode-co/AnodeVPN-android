package co.anode.anodevpn

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.net.ssl.HttpsURLConnection


object AnodeClient {
    lateinit var mycontext: Context

    fun init(context: Context)  {
        mycontext= context
    }

    @Throws(IOException::class, JSONException::class)
    fun httpPost(myUrl: String, type: String, message: String?): String {
        val apiKey = "hthiP3Gx.TLJRcZGpsHh8ImjtBCjpB7soD87qsaDb"
        val url = URL(myUrl)
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        //conn.setRequestProperty("Authorization", "Api-Key $apiKey")
        val jsonObject = errorJsonObj(type, message)
        setPostRequestContent(conn, jsonObject)
        conn.connect()
        return conn.responseMessage + ""
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
        jsonObject.accumulate("public_key", anodeUtil.getPubKey())
        jsonObject.accumulate("error", type)
        jsonObject.accumulate("client_software_version", BuildConfig.VERSION_CODE)
        jsonObject.accumulate("client_os", "Android")
        jsonObject.accumulate("client_os_version", android.os.Build.VERSION.RELEASE)
        jsonObject.accumulate("local_timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
        jsonObject.accumulate("ip4_address", CjdnsSocket.ipv4Address)
        jsonObject.accumulate("ip6_address", CjdnsSocket.ipv6Route)
        jsonObject.accumulate("message", message)
        val cjdroutelogfile = File(anodeUtil.CJDNS_PATH+"/"+ anodeUtil.CJDROUTE_LOG)
        val lastlogfile = File(anodeUtil.CJDNS_PATH+"/last_anodevpn.log")
        val currlogfile = File(anodeUtil.CJDNS_PATH+"/anodevpn.log")
        var debugmsg = "peerStats: "+CjdnsSocket.logpeerStats+"\nshowConnections: "+CjdnsSocket.logshowConnections
        if (message!! == "Submit logs")
        {
            if(currlogfile.exists()) {
                jsonObject.accumulate("new_android_log", currlogfile.readText(Charsets.UTF_8))
            }
            if(lastlogfile.exists()) {
                jsonObject.accumulate("previous_android_log", lastlogfile.readText(Charsets.UTF_8))
            }
            if(cjdroutelogfile.exists()) {
                debugmsg += "\n\nCDJROUTE LOG:"+cjdroutelogfile.readText(Charsets.UTF_8)
            }
        } else {
            if(currlogfile.exists()) {
                jsonObject.accumulate("new_android_log", currlogfile.readText(Charsets.UTF_8))
            }
            if(cjdroutelogfile.exists()) {
                debugmsg += "\n\nCDJROUTE LOG:"+cjdroutelogfile.readText(Charsets.UTF_8)
            }
        }
        jsonObject.accumulate("debugging_messages", debugmsg)
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

    fun getLatestAPK() {
        //TODO: send device arch to receive latest APK URL
    }

    fun getListofServers() {

    }
}