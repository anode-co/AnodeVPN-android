package co.anode.anodium.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.anode.anodium.BuildConfig
import co.anode.anodium.CjdnsStatsActivity
import co.anode.anodium.R
import co.anode.anodium.databinding.FragmentProfileBinding
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import co.anode.anodium.wallet.PasswordPrompt
import co.anode.anodium.wallet.PinPrompt
import co.anode.anodium.wallet.WalletStatsActivity
import org.json.JSONObject

class ProfileFragment : Fragment() {
    private lateinit var mycontext: Context
    private var _binding: FragmentProfileBinding? = null
    private val LOGTAG = "co.anode.anodium"
    private val binding get() = _binding!!
    private lateinit var apiController: APIController

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
        val service = ServiceVolley()
        apiController = APIController(service)
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
            getSeed()
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

    private fun getSeed() {
        apiController.get(apiController.getSeedURL) { response ->
            if ((response != null) && (response.has("seed") && !response.isNull("seed"))) {
                Log.i(LOGTAG, "wallet seed rerieved")
                //Get seed
                val seedArray = response.getJSONArray("seed")
                var seedString = ""
                for (i in 0 until seedArray.length()) {
                    seedString += seedArray.getString(i) + " "
                }
                seedDialog(seedString)
            } else {
                Log.e(co.anode.anodium.support.LOGTAG, "Error in generating wallet seed")
                Toast.makeText(requireContext(), "Failed to generate wallet seed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun seedDialog(seed: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("This is your wallet seed phrase")
        builder.setMessage(seed)
        builder.setNegativeButton("Copy") { dialog, _ ->
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Seed", seed)
            clipboard.setPrimaryClip(clip)
            dialog.dismiss()
        }
        builder.setPositiveButton("Close") { dialog, _ ->
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }
}