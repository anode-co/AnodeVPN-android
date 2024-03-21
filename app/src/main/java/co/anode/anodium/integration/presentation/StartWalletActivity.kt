package co.anode.anodium.integration.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pkt.core.presentation.start.StartFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class StartWalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Timber.i("StartWalletActivity onCreate")
//        if (savedInstanceState == null) {
//        }
        supportFragmentManager.beginTransaction()
            .replace(
                android.R.id.content,
                StartFragment().apply{}
            )
            .commit()
    }


    companion object {
        fun getIntent(context: Context) = Intent(context, StartFragment::class.java).apply {}
    }
}