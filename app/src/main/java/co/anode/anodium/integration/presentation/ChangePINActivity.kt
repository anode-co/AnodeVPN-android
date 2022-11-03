package co.anode.anodium.integration.presentation

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.pkt.core.presentation.main.settings.changepassword.ChangePasswordFragment
import com.pkt.core.presentation.main.settings.changepin.ChangePinFragment
import com.pkt.core.presentation.main.vpn.VpnFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChangePINActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(
                    android.R.id.content,
                    ChangePinFragment()
                )
                .commit()
        }
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, ChangePinFragment::class.java).apply {}
    }
}