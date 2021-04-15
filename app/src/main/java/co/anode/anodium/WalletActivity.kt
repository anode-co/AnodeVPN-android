package co.anode.anodium

import android.content.Context
import android.os.Bundle
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit

class WalletActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallet)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = getString(R.string.wallet_activity_title)
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)

        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val createfragment = WalletFragmentSetup()
        val mainfragment = WalletFragmentMain()

        //Show setup or main fragment according to wallet existing or not
        if (prefs.getBoolean("lndwallet", false)) {
          supportFragmentManager.beginTransaction()
                  .show(mainfragment)
                  .commit()
            supportFragmentManager.beginTransaction()
                    .hide(createfragment)
                    .commit()
        } else {
            supportFragmentManager.beginTransaction()
                    .show(createfragment)
                    .commit()
            supportFragmentManager.beginTransaction()
                    .hide(mainfragment)
                    .commit()
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}