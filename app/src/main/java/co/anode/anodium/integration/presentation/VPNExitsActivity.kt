package co.anode.anodium.integration.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pkt.core.presentation.main.vpn.VpnFragment
import com.pkt.core.presentation.main.vpn.exits.VpnExitsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VPNExitsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    VpnExitsFragment()
                )
                .commit()
        }
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, VpnExitsFragment::class.java).apply {}
    }
}