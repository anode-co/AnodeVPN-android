package co.anode.anodium.integration.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.os.bundleOf
import com.pkt.core.presentation.createwallet.CreateWalletFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateWalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    CreateWalletFragment().apply{}
                )
                .commit()
        }
    }

    companion object {
        fun getIntent(context: Context, walletName: String, PKTtoUSD: String) = Intent(context, CreateWalletFragment::class.java).apply {}
    }
}