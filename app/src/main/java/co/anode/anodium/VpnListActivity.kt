package co.anode.anodium

//import kotlin.collections.ArrayList
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_vpn_servers_list.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL


class VpnListActivity : AppCompatActivity() {
    var dataList = ArrayList<HashMap<String, String>>()
    var adapter: VPNListAdapter? = null
    private val API_VERSION = "0.3"
    private val API_SERVERS_LIST = "https://vpn.anode.co/api/$API_VERSION/vpn/servers/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn_servers_list)
        setSupportActionBar(vpnlist_toolbar)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        //actionbar!!.title = "New Activity"
        //set back button
        actionbar?.setDisplayHomeAsUpEnabled(true)
        //Retrieve Servers list
        fetchVpnServers().execute()
        val searchView = findViewById<SearchView>(R.id.searchText)

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter!!.setFilter(newText)
                return false
            }
        })
        AnodeClient.eventLog(baseContext,"Activity: VPN List created")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    inner class fetchVpnServers() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            try {
                AnodeClient.statustv = findViewById(R.id.textview_status)
                var url = API_SERVERS_LIST
                if (params.isNotEmpty()) url = params[0].toString()
                //return URL(url).readText(Charsets.UTF_8)
                return AnodeClient.APIHttpReq(url,"", "GET", true, false)
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

            val serversArr = JSONArray(result)
            //val prefs = baseContext.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)

            for (i in 0 until serversArr.length()) {
                val serverdetails = serversArr.getJSONObject(i)
                val map = HashMap<String, String>()
                //if (!serverdetails.getBoolean("isFake")) {
                    map["name"] = serverdetails.getString("name")

                    map["publicKey"] = serverdetails.getString("publicKey")
                    map["bandwidthBps"] = serverdetails.getString("bandwidthBps")
                    map["region"] = serverdetails.getString("region")
                    map["countryCode"] = serverdetails.getString("countryCode")
                    map["onlineSinceDatetime"] = serverdetails.getString("onlineSinceDatetime")
                    map["lastSeenDatetime"] = serverdetails.getString("lastSeenDatetime")
                    map["averageRating"] = serverdetails.getString("averageRating")
                    map["isFavorite"] = serverdetails.getString("isFavorite")
                    map["speed"] = "100"
                    val networkdetails = serverdetails.getJSONObject("networkSettings")
                    map["network_usesNat"] = networkdetails.getString("usesNat")

                    dataList.add(map)
                //}
            }
            //Sort list by country
            dataList.sortWith(compareBy {it.get("countryCode")})
            adapter = VPNListAdapter(this@VpnListActivity, dataList)
            findViewById<ListView>(R.id.listview_servers).adapter = adapter
        }
    }
}
