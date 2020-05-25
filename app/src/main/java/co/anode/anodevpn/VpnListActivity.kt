package co.anode.anodevpn

import android.annotation.SuppressLint
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ListView
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_vpn_servers_list.*
import org.json.JSONObject
import java.net.URL

class VpnListActivity : AppCompatActivity() {
    var dataList = ArrayList<HashMap<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn_servers_list)
        setSupportActionBar(toolbar)
        //Retrieve Servers list
        fetchVpnServers().execute()
    }

    inner class fetchVpnServers() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            try {
                var url = "https://vpn.anode.co/api/0.2/vpn/servers/"
                if (params.isNotEmpty()) url = params[0].toString()
                return URL(url).readText(Charsets.UTF_8)
            } catch (e: Exception) {
                Toast.makeText(baseContext, "Error: "+e.message, Toast.LENGTH_LONG).show()
                return null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result.isNullOrBlank()) {
                if (dataList.isNotEmpty()) findViewById<ListView>(R.id.listview_servers).adapter = VPNListAdapter(this@VpnListActivity, dataList)
                return
            }
            //findViewById<ProgressBar>(R.id.loader).visibility = View.GONE

            val jsonObj = JSONObject(result)
            val nextUrl = jsonObj.getString("next")
            val serversArr = jsonObj.getJSONArray("results")
            for (i in 0 until serversArr.length()) {
                val serverdetails = serversArr.getJSONObject(i)
                val map = HashMap<String, String>()
                if (!serverdetails.getBoolean("isFake")) {
                    map["name"] = serverdetails.getString("name")
                    map["countryCode"] = serverdetails.getString("countryCode")
                    map["speed"] = "100"
                    dataList.add(map)
                }
            }
            if (nextUrl == "null") findViewById<ListView>(R.id.listview_servers).adapter = VPNListAdapter(this@VpnListActivity, dataList)
            else fetchVpnServers().execute(nextUrl)
        }
    }
}
