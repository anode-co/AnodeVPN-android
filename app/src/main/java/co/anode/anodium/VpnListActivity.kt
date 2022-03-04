@file:Suppress("DEPRECATION")

package co.anode.anodium

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray


class VpnListActivity : AppCompatActivity() {
    var dataList = ArrayList<HashMap<String, String>>()
    var adapter: VPNListAdapter? = null
    private val apiVersion = "0.3"
    private val apiServersList = "https://vpn.anode.co/api/$apiVersion/vpn/servers/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn_servers_list)
        val vpnListToolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.vpnlist_toolbar)
        setSupportActionBar(vpnListToolbar)
        //actionbar
        val actionbar = supportActionBar
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
        AnodeClient.eventLog("Activity: VPN List created")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    inner class fetchVpnServers : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            return try {
                AnodeClient.statustv = findViewById(R.id.textview_status)
                var url = apiServersList
                if (params.isNotEmpty()) url = params[0].toString()
                //return URL(url).readText(Charsets.UTF_8)
                AnodeClient.APIHttpReq(url,"", "GET", needsAuth = true, isRetry = false)
            } catch (e: Exception) {
                Log.e(LOGTAG, "Error: "+e.message)
                runOnUiThread {
                    Toast.makeText(baseContext, "Error: "+e.message, Toast.LENGTH_LONG).show()
                }
                null
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result.isNullOrBlank()) {
                if (dataList.isNotEmpty()) findViewById<ListView>(R.id.listview_servers).adapter = VPNListAdapter(this@VpnListActivity, supportFragmentManager, dataList)
                return
            }

            val serversArr = JSONArray(result)
            //val prefs = baseContext.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)

            for (i in 0 until serversArr.length()) {
                val serverDetails = serversArr.getJSONObject(i)
                val map = HashMap<String, String>()
                map["publicKey"] = serverDetails.getString("publicKey")
                map["name"] = serverDetails.getString("name")
                map["countryCode"] = serverDetails.getString("countryCode")
                map["averageRating"] = serverDetails.getString("averageRating")
                map["cost"] = serverDetails.getString("cost")
                map["load"] = serverDetails.getInt("load").toString()
                map["quality"] = serverDetails.getInt("quality").toString()
                map["isFavorite"] = serverDetails.getString("isFavorite")
                map["onlineSinceDatetime"] = serverDetails.getString("onlineSinceDatetime")
                map["lastSeenDatetime"] = serverDetails.getString("lastSeenDatetime")
                map["numRatings"] = serverDetails.getInt("numRatings").toString()
                dataList.add(map)
            }
            //Sort list by country
            dataList.sortWith(compareBy { it["countryCode"] })
            adapter = VPNListAdapter(this@VpnListActivity,supportFragmentManager, dataList)
            findViewById<ListView>(R.id.listview_servers).adapter = adapter
        }
    }
}
