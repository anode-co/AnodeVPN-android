package co.anode.anodium.integration.presentation.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import com.pkt.core.presentation.main.settings.walletinfo.WalletInfoFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    WalletInfoFragment().apply {
                        arguments = bundleOf("address" to intent.getStringExtra(EXTRA_ADDRESS))
                    }
                )
                .commit()
        }
    }

    companion object {
        private const val EXTRA_ADDRESS = "EXTRA_ADDRESS"
        fun getIntent(context: Context, address: String) = Intent(context, WalletInfoActivity::class.java).apply {
            putExtra(EXTRA_ADDRESS, address)
        }
    }
}
