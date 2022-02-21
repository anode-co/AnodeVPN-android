package co.anode.anodium

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.TextView
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley

class WalletInfoActivity : AppCompatActivity() {
    lateinit var apiController: APIController
    lateinit var h: Handler
    private val refreshValuesInterval: Long = 10000
    lateinit var infotext:TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet_info)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "Wallet Info"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        h = Handler(Looper.getMainLooper())
        infotext = findViewById(R.id.walletinfotext)
        //Initialize handlers
        val service = ServiceVolley()
        apiController = APIController(service)
    }

    private fun getInfo() {
        apiController.get(apiController.getInfoURL) { response ->
            if (response != null) {
                val jsonString = response.toString()
                infotext.text = jsonString.replace(",",",\n")
            }
            h.postDelayed(getPldInfo, refreshValuesInterval)
        }
    }

    private val getPldInfo = Runnable { getInfo() }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        h.postDelayed(getPldInfo, 0)
    }

    override fun onPause() {
        super.onPause()
        h.removeCallbacks(getPldInfo)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}