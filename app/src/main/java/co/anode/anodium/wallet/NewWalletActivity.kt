package co.anode.anodium.wallet

import android.content.Intent
import android.net.VpnService
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import co.anode.anodium.AboutDialog
import co.anode.anodium.BuildConfig
import co.anode.anodium.R
import co.anode.anodium.integration.presentation.CreateWalletActivity
import co.anode.anodium.integration.presentation.EnterWalletActivity

class NewWalletActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_wallet)


        val newWallet = findViewById<Button>(R.id.buttonCreateNewWallet)
        newWallet.setOnClickListener {
            val passwordActivity = Intent(this, PasswordPrompt::class.java)
            passwordActivity.putExtra("noWallet", true)
            startActivity(passwordActivity)
        }
        val recoverWallet = findViewById<Button>(R.id.buttonLoadFromSeed)
        recoverWallet.setOnClickListener {
            val passwordActivity = Intent(this, PasswordPrompt::class.java)
            passwordActivity.putExtra("recoverWallet", true)
            startActivity(passwordActivity)
        }
        val prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
        var useNewUI = prefs.getBoolean("useNewUI", false)
        useNewUI = true //DEBUG
        if (useNewUI) {
            val createWalletActivity = Intent(applicationContext, CreateWalletActivity::class.java)
            createWalletActivity.putExtra("WALLET_NAME", "wallet")//Default wallet name
            startActivity(createWalletActivity)
        }
        //On first run show data consent
        if (prefs.getBoolean("FirstRun", true)) {
            AboutDialog.show(this)
            with(prefs.edit()) {
                putBoolean("FirstRun", false)
                commit()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val vpnPrepare = VpnService.prepare(this)
        if (vpnPrepare != null) {
            startActivityForResult(vpnPrepare, 0);
        }
    }
}