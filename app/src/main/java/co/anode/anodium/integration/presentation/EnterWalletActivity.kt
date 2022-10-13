package co.anode.anodium.integration.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.os.bundleOf
import com.pkt.core.presentation.enterwallet.EnterWalletFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class EnterWalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    EnterWalletFragment().apply {
                        arguments = bundleOf(
                            "walletName" to intent.getStringExtra(WALLET_NAME)
                        )
                    }
                )
                .commit()
        }
    }

    companion object {
        private const val WALLET_NAME = "WALLET_NAME"
        fun getIntent(context: Context, walletName: String) = Intent(context, EnterWalletFragment::class.java).apply {
            putExtra(WALLET_NAME, walletName)
        }
    }
}