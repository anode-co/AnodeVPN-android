package co.anode.anodium.integration.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.os.bundleOf
import com.pkt.core.presentation.main.wallet.WalletFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    WalletFragment().apply {
                        arguments = bundleOf(
                            "walletName" to intent.getStringExtra(WALLET_NAME),
                            "PKTtoUSD" to intent.getStringExtra(PKT_TO_USD)
                        )
                    }
                )
                .commit()
        }
    }

    companion object {
        private const val WALLET_NAME = "WALLET_NAME"
        private const val PKT_TO_USD = "PKT_TO_USD"
        fun getIntent(context: Context, walletName: String, PKTtoUSD: String) = Intent(context, WalletFragment::class.java).apply {
            putExtra(WALLET_NAME, walletName)
            putExtra(PKT_TO_USD, PKTtoUSD)
        }
    }
}