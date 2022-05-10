package co.anode.anodium

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import co.anode.anodium.databinding.ActivityMainBinding
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private val LOGTAG = "co.anode.anodium"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        initializeApp()
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)
    }

    private fun initializeApp() {
        AnodeClient.init(applicationContext, this)
        AnodeUtil.init(applicationContext)
        AnodeUtil.initializeApp()
        AnodeUtil.launchPld()
        val prefs = getSharedPreferences("co.anode.anodium", MODE_PRIVATE)
        //If there is no username stored
        if (prefs.getString("username", "").isNullOrEmpty()) {
            //Generate a username
            val executor = Executors.newSingleThreadExecutor()
            val handler = Handler(Looper.getMainLooper())
            var usernameResponse: String
            executor.execute {
                usernameResponse = AnodeClient.generateUsername()
                handler.post {
                    generateUsernameHandler(usernameResponse)
                }
            }
        }
    }

    private fun generateUsernameHandler(result: String) {
        Log.i(co.anode.anodium.support.LOGTAG,"Received from API: $result")
        if ((result.isBlank())) {
            return
        } else if (result.contains("400") || result.contains("401")) {
            val json = result.split("-")[1]
            var msg = result
            try {
                val jsonObj = JSONObject(json)
                msg = jsonObj.getString("username")
            } catch (e: JSONException) {
                msg += " Invalid JSON"
            }
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        } else if (result.contains("ERROR: ")) {
            Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
        } else {
            val jsonObj = JSONObject(result)
            if (jsonObj.has("username")) {
                val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                with (prefs.edit()) {
                    putString("username", jsonObj.getString("username"))
                    putBoolean("SignedIn", false)
                    commit()
                }
            }
        }
    }
}