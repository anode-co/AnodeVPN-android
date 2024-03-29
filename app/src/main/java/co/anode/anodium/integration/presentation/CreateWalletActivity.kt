package co.anode.anodium.integration.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pkt.core.presentation.createwallet.CreateWalletFragment
import com.pkt.core.presentation.createwallet.CreateWalletMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CreateWalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    CreateWalletFragment.newCreateWalletInstance(intent.extras?.getSerializable(EXTRA_MODE) as? CreateWalletMode)
                )
                .commit()
        }
    }

    companion object {
        private const val EXTRA_MODE = "EXTRA_MODE"
        private const val EXTRA_NAME = "EXTRA_NAME"

        fun getCreateWalletIntent(context: Context, name: String?) =
            Intent(context, CreateWalletActivity::class.java)
                .putExtra(EXTRA_MODE, CreateWalletMode.CREATE)
                .putExtra(EXTRA_NAME, name)

        fun getRecoverWalletIntent(context: Context) =
            Intent(context, CreateWalletActivity::class.java).putExtra(EXTRA_MODE, CreateWalletMode.RECOVER)
    }
}
