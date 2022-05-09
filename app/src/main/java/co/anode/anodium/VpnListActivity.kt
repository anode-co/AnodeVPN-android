@file:Suppress("DEPRECATION")

package co.anode.anodium

import android.os.*
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import co.anode.anodium.support.AnodeClient
import org.json.JSONArray
import java.util.concurrent.Executors


class VpnListActivity : AppCompatActivity() {
    var dataList = ArrayList<HashMap<String, String>>()
    var adapter: VPNListAdapter? = null
    private val apiVersion = "0.3"
    private val apiServersList = "https://vpn.anode.co/api/$apiVersion/vpn/servers/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vpn_servers_list)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.title_activity_vpn_servers_list)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        AnodeClient.eventLog("Activity: VPN List created")
        val loading = findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        val loading = findViewById<ProgressBar>(R.id.loadingAnimation)
        loading.visibility = View.VISIBLE
        //Retrieve Servers list
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        val statusBar = findViewById<TextView>(R.id.textview_status)
        executor.execute {
            runOnUiThread {
                statusBar.text = "Retrieving VPN Servers..."
            }

            val result = AnodeClient.APIHttpReq(apiServersList,"", "GET", needsAuth = false, isRetry = false)
            handler.post {
                loading.visibility = View.GONE
                statusBar.text = ""
                if (result.isBlank()) {
                    if (dataList.isNotEmpty()) findViewById<ListView>(R.id.listview_servers).adapter = VPNListAdapter(this@VpnListActivity, supportFragmentManager, dataList)
                    return@post
                }
                try {
                    val serversArr = JSONArray(result)
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
                    adapter = VPNListAdapter(this@VpnListActivity, supportFragmentManager, dataList)
                    findViewById<ListView>(R.id.listview_servers).adapter = adapter
                } catch (e: Exception) {
                    Toast.makeText(this, "Error in getting VPN list servers", Toast.LENGTH_LONG).show()
                }
            }
        }

        val searchView = findViewById<SearchView>(R.id.searchText)
        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                adapter?.setFilter(newText)
                return false
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
