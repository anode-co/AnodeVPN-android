package co.anode.anodium.ui.profile

import android.content.*
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import co.anode.anodium.*
import co.anode.anodium.databinding.FragmentProfileBinding
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.CjdnsSocket
import co.anode.anodium.volley.APIController
import co.anode.anodium.volley.ServiceVolley
import co.anode.anodium.wallet.PasswordPrompt
import co.anode.anodium.wallet.PinPrompt
import co.anode.anodium.wallet.WalletStatsActivity
import org.json.JSONObject
import java.io.File


class ProfileFragment : Fragment() {
    private lateinit var mycontext: Context
    private var _binding: FragmentProfileBinding? = null
    private val LOGTAG = "co.anode.anodium"
    private val binding get() = _binding!!
    private lateinit var apiController: APIController
    private var activeWallet = ""
    private var walletNames: ArrayList<String> = ArrayList<String>()
    private lateinit var walletsSpinner: Spinner
    private lateinit var prefs: SharedPreferences

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

        val idTextview = root.findViewById<TextView>(R.id.user_id)
        val versionTextview = root.findViewById<TextView>(R.id.version_number)
        prefs = mycontext.getSharedPreferences("co.anode.anodium", AppCompatActivity.MODE_PRIVATE)
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
            getWalletSeed(showDialog = true, saveToFile = false)
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
        val checkUpdateButton = root.findViewById<Button>(R.id.button_check_update)
        checkUpdateButton.setOnClickListener {
            AnodeClient.checkNewVersion(true)
        }
        val addWalletButton = root.findViewById<ImageButton>(R.id.button_add_wallet)
        addWalletButton.setOnClickListener {
            //Dialog for wallet name
            walletNameDialog(true)
        }
        val editWalletButton = root.findViewById<ImageButton>(R.id.button_edit_wallet)
        editWalletButton.setOnClickListener {
            //Dialog for renaming wallet
            walletNameDialog(false)
        }

        walletsSpinner = root.findViewById(R.id.wallet_selector)
        walletNames = AnodeUtil.getWalletFiles()
        val dataAdapter = ArrayAdapter(mycontext,android.R.layout.simple_spinner_dropdown_item, walletNames)
        walletsSpinner.adapter = dataAdapter
        activeWallet = prefs.getString("activeWallet", "wallet").toString()
        var activeWalletId = 0
        var i = 0
        for (name in walletNames) {
            if (name == activeWallet) {
                activeWalletId = i
                break;
            }
            i++
        }
        walletsSpinner.setSelection(activeWalletId)
        walletsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (activeWallet != walletNames[position]) {
                    activeWallet = walletNames[position]
                    prefs.edit().putString("activeWallet", activeWallet).apply()
                    //disconnect VPN
                    disconnectVPN()
                    //close cjdns
                    AnodeUtil.stopCjdns()
                    AnodeUtil.cleatWalletCache()
                    AnodeUtil.deleteWalletChainBackupFile()
                    //restart pld
                    AnodeUtil.stopPld()
                    //unlock selected wallet->getWalletSeed to file
                    walletPasswordPrompt()
                    //connect back to VPN
                    notifyUserDialog()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }
        return root
    }

    private fun disconnectVPN() {
        AnodeClient.AuthorizeVPN().cancel(true)
        AnodeClient.stopThreads()
        CjdnsSocket.IpTunnel_removeAllConnections()
        CjdnsSocket.Core_stopTun()
        CjdnsSocket.clearRoutes()
        mycontext.startService(Intent(mycontext, AnodeVpnService::class.java).setAction("co.anode.anodium.STOP"))
    }

    private fun updateWallets() {
        walletNames = AnodeUtil.getWalletFiles()
        val dataAdapter = ArrayAdapter(mycontext,android.R.layout.simple_spinner_dropdown_item, walletNames)
        walletsSpinner.adapter = dataAdapter
        activeWallet = prefs.getString("activeWallet", "wallet").toString()
        var activeWalletId = 0
        var i = 0
        for (name in walletNames) {
            if (name == activeWallet) {
                activeWalletId = i
                break;
            }
            i++
        }
        walletsSpinner.setSelection(activeWalletId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getWalletSeed(showDialog:Boolean, saveToFile:Boolean) {
        apiController.get(apiController.getSeedURL) { response ->
            if ((response != null) && (response.has("seed") && !response.isNull("seed"))) {
                Log.i(LOGTAG, "wallet seed retrieved")
                //Get seed
                val seedArray = response.getJSONArray("seed")
                var seedString = ""
                for (i in 0 until seedArray.length()) {
                    seedString += seedArray.getString(i) + " "
                }
                if(showDialog) {
                    seedDialog(seedString)
                }
                if (saveToFile) {
                    val filename = mycontext.filesDir.toString()+"/seed.txt"
                    File(filename).writeText(seedString)
                    //cjdns seed genconf
                    AnodeUtil.initializeCjdrouteConfFile()
                    //launch cjdns
                    AnodeUtil.launchCJDNS()
                }
            } else {
                Log.e(co.anode.anodium.support.LOGTAG, "Error in generating wallet seed")
                Toast.makeText(requireContext(), "Failed to get wallet seed.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun notifyUserDialog() {
//        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
//        builder.setTitle("Changed selected PKT wallet")
//        builder.setMessage("Your VPN connection has been closed and your PKT wallet restarted.")
//        builder.setPositiveButton("OK") { dialog, _ ->
//            dialog.dismiss()
//        }
//        val alert: AlertDialog = builder.create()
//        alert.show()
        Toast.makeText(mycontext,"Your VPN connection has been closed and your PKT wallet restarted.", Toast.LENGTH_LONG).show()
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

    private fun walletNameDialog(addWallet: Boolean) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Wallet name")
        if (addWallet) {
            builder.setMessage("New wallet")
        } else {
            builder.setMessage("Rename $activeWallet")
        }

        val input = EditText(requireContext())
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setPositiveButton("Next") { dialog, _ ->
            val walletName = input.text.toString()
            if (addWallet) {
                //restart pld
                AnodeUtil.stopPld()
                val passwordActivity = Intent(context, PasswordPrompt::class.java)
                passwordActivity.putExtra("walletName", walletName)
                startActivity(passwordActivity)
            } else {
                renameWallet(walletName)
                //restart pld
                AnodeUtil.stopPld()
                //update dropdown
                updateWallets()
            }
            dialog.dismiss()
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun renameWallet(walletName: String) {
        val walletFile = File("${AnodeUtil.filesDirectory}/pkt/$activeWallet.db")
        walletFile.renameTo(File("${AnodeUtil.filesDirectory}/pkt/$walletName.db"))
        activeWallet = walletName
        prefs.edit().putString("activeWallet", activeWallet).apply()
    }

    private fun unlockWallet(password:String) {
        Log.i(LOGTAG, "Trying to unlock wallet")
        val jsonRequest = JSONObject()
        jsonRequest.put("wallet_passphrase", password)
        jsonRequest.put("wallet_name", "$activeWallet.db")
        apiController.post(apiController.unlockWalletURL,jsonRequest) { response ->
            if (response == null) {
                Log.i(LOGTAG, "unknown status for wallet")
            } else if ((response.has("error")) &&
                response.getString("error").contains("ErrWrongPassphrase")) {
                Log.d(LOGTAG, "Error unlocking wallet, wrong password")
                walletPasswordPrompt()
            } else if (response.has("error")) {
                Log.d(LOGTAG, "Error: "+response.getString("error").toString())
            } else if (response.length() == 0) {
                //empty response is success
                Log.i(LOGTAG, "Wallet unlocked")
                //getseed
                getWalletSeed(showDialog = false,saveToFile = true)
            }
        }
    }

    private fun walletPasswordPrompt() {
        val builder = AlertDialog.Builder(mycontext)
        val input = EditText(mycontext)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setMessage("Please enter your password")
        input.transformationMethod = PasswordTransformationMethod.getInstance()
        builder.setPositiveButton("OK"
        ) { dialog, _ ->
            val inputPassword = input.text.toString()
            dialog.dismiss()
            unlockWallet(inputPassword)
        }

        builder.setNegativeButton("Cancel"
        ) { dialog, _ ->
            dialog.dismiss()
        }

        val alert = builder.create()
        alert.show()
    }
}