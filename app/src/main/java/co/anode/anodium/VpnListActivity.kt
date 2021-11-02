package co.anode.anodium

import android.app.Activity
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray


class VpnListActivity : AppCompatActivity() {
    var dataList = ArrayList<HashMap<String, String>>()
    var adapter: VPNListAdapter? = null
    private val API_VERSION = "0.3"
    private val API_SERVERS_LIST = "https://vpn.anode.co/api/$API_VERSION/vpn/servers/"

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
        AnodeClient.eventLog(baseContext,"Activity: VPN List created")
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            //TODO:???
        }
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
                Log.e(LOGTAG, "Error: "+e.message)
                runOnUiThread {
                    Toast.makeText(baseContext, "Error: "+e.message, Toast.LENGTH_LONG).show()
                }
                return null
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
            dataList.sortWith(compareBy {it.get("countryCode")})
            adapter = VPNListAdapter(this@VpnListActivity,supportFragmentManager, dataList)
            findViewById<ListView>(R.id.listview_servers).adapter = adapter
        }
    }
}
