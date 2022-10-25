package co.anode.anodium.integration.presentation

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pkt.core.presentation.start.StartFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StartWalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    StartFragment().apply{}
                )
                .commit()
        }
    }
}