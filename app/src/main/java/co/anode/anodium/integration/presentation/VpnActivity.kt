package co.anode.anodium.integration.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pkt.core.presentation.main.vpn.VpnFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class VpnActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    VpnFragment()
                )
                .commit()
        }
    }

    companion object {

    }
}