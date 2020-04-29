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
        val url = URL(myUrl)
        val conn = url.openConnection() as HttpsURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        val jsonObject = buidJsonObject(type, message)
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
            } ?: false
        }

    @Throws(JSONException::class)
    private fun buidJsonObject(type: String, message: String?): JSONObject {
        val anodeUtil: AnodeUtil = AnodeUtil(null)
        val jsonObject = JSONObject()
        jsonObject.accumulate("pubkey", anodeUtil.getPubKey())
        jsonObject.accumulate("type", type)
        jsonObject.accumulate("message",message)
        jsonObject.accumulate("ip4address", CjdnsSocket.ipv4Address)
        jsonObject.accumulate("ip6address", CjdnsSocket.ipv6Route)
        val logfile = File(anodeUtil.CJDNS_PATH+"/"+ anodeUtil.CJDROUTE_LOG)
        jsonObject.accumulate("cjdroute_log", logfile.readText(Charsets.UTF_8))
        jsonObject.accumulate("local_timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))

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
}