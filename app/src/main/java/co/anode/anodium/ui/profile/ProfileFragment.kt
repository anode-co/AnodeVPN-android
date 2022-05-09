package co.anode.anodium.ui.profile

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.anode.anodium.BuildConfig
import co.anode.anodium.CjdnsStatsActivity
import co.anode.anodium.R
import co.anode.anodium.databinding.FragmentProfileBinding
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.wallet.PasswordPrompt
import co.anode.anodium.wallet.PinPrompt
import co.anode.anodium.wallet.WalletStatsActivity

class ProfileFragment : Fragment() {
    private lateinit var mycontext: Context
    private var _binding: FragmentProfileBinding? = null
    private val LOGTAG = "co.anode.anodium"
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val notificationsViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)
        mycontext = requireContext()
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root

        getWallets(root)
        val idTextview = root.findViewById<TextView>(R.id.user_id)
        val versionTextview = root.findViewById<TextView>(R.id.version_number)
        val prefs = mycontext.getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
        //If there is no username stored
        if (prefs.getString("username", "").isNullOrEmpty()) {
            AnodeUtil.generateUsername(idTextview)
        } else {
            idTextview.text = prefs.getString("username","")
        }
        versionTextview.text = "Version: "+ BuildConfig.VERSION_NAME

        val changePasswordButton = root.findViewById<Button>(R.id.button_change_password)
        changePasswordButton.setOnClickListener {
            Log.i(LOGTAG, "Open PasswordPrompt activity for changing password")
            val promptPasswordActivity = Intent(mycontext, PasswordPrompt::class.java)
            promptPasswordActivity.putExtra("changepassphrase", true)
            startActivity(promptPasswordActivity)
        }
        val showSeedButton = root.findViewById<Button>(R.id.button_show_seed)
        showSeedButton.setOnClickListener {
            //TODO: use a dialog with a copy and share button?
        }
        val cjdnsStatsButton = root.findViewById<Button>(R.id.button_cjdns_stats)
        cjdnsStatsButton.setOnClickListener {
            Log.i(LOGTAG, "Start cjdns stats (debug) activity")
            startActivity(Intent(mycontext, CjdnsStatsActivity::class.java))
        }
        val walletStatsButton = root.findViewById<Button>(R.id.button_wallet_stats)
        walletStatsButton.setOnClickListener {
            Log.i(LOGTAG, "Start wallet stats activity")
            startActivity(Intent(mycontext, WalletStatsActivity::class.java))
        }
        val setPinButton = root.findViewById<Button>(R.id.button_set_pin)
        setPinButton.setOnClickListener {
            Log.i(LOGTAG, "Open PasswordPin activity for setting PIN")
            val promptPinActivity = Intent(mycontext, PinPrompt::class.java)
            promptPinActivity.putExtra("changepassphrase", true)
            promptPinActivity.putExtra("noNext", true)
            startActivity(promptPinActivity)
        }
        return root
    }

    private fun getWallets(v: View) {
        val dropdown = v.findViewById<Spinner>(R.id.wallet_selector)
        dropdown.prompt = "select wallet..."
        val walletNames = AnodeUtil.getWalletFiles()
        val dataAdapter = ArrayAdapter(mycontext,android.R.layout.simple_spinner_dropdown_item, walletNames)
        dropdown.adapter = dataAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}