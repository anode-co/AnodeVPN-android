package co.anode.anodium.ui.profile

import android.content.*
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import co.anode.anodium.*
import co.anode.anodium.databinding.FragmentProfileBinding
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import co.anode.anodium.support.CjdnsSocket
import co.anode.anodium.support.CubeWifi
import co.anode.anodium.wallet.PasswordPrompt
import co.anode.anodium.wallet.PinPrompt
import co.anode.anodium.wallet.WalletStatsActivity
import org.json.JSONObject
import timber.log.Timber
import java.io.File
import java.net.NetworkInterface


class ProfileFragment : Fragment() {
    private lateinit var mycontext: Context
    private var _binding: FragmentProfileBinding? = null
    private val LOGTAG = BuildConfig.APPLICATION_ID
    private val binding get() = _binding!!
    private var activeWallet = ""
    private var walletNames: ArrayList<String> = ArrayList()
    private lateinit var walletsSpinner: Spinner
    private lateinit var prefs: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
/*        val notificationsViewModel =
            ViewModelProvider(this).get(ProfileViewModel::class.java)*/
        mycontext = requireContext()
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val idTextview = root.findViewById<TextView>(R.id.user_id)
        val versionTextview = root.findViewById<TextView>(R.id.version_number)
        prefs = mycontext.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)


        //If there is no username stored
        if (prefs.getString("username", "").isNullOrEmpty()) {
            AnodeUtil.generateUsername(idTextview)
        } else {
            idTextview.text = prefs.getString("username","")
        }
        versionTextview.text = "Version: "+ BuildConfig.VERSION_NAME

        val changePasswordButton = root.findViewById<LinearLayout>(R.id.button_change_password)
        changePasswordButton.setOnClickListener {
            Log.i(LOGTAG, "Open PasswordPrompt activity for changing password")
            val promptPasswordActivity = Intent(mycontext, PasswordPrompt::class.java)
            promptPasswordActivity.putExtra("changepassphrase", true)
            promptPasswordActivity.putExtra("walletName", activeWallet)
            startActivity(promptPasswordActivity)
        }
        val showSeedButton = root.findViewById<LinearLayout>(R.id.button_show_seed)
        showSeedButton.setOnClickListener {
            getWalletSeed(showDialog = true)
        }
        val cjdnsStatsButton = root.findViewById<LinearLayout>(R.id.button_cjdns_stats)
        cjdnsStatsButton.setOnClickListener {
            Log.i(LOGTAG, "Start cjdns stats (debug) activity")
            startActivity(Intent(mycontext, CjdnsStatsActivity::class.java))
        }
        val walletStatsButton = root.findViewById<LinearLayout>(R.id.button_wallet_stats)
        walletStatsButton.setOnClickListener {
            Timber.tag(LOGTAG).i("Start wallet stats activity")

            val statsActivity = Intent(mycontext, WalletStatsActivity::class.java)
            statsActivity.putExtra("walletName", activeWallet)
            startActivity(statsActivity)
        }
        val setPinButton = root.findViewById<LinearLayout>(R.id.button_set_pin)
        setPinButton.setOnClickListener {
            Timber.tag(LOGTAG).i("Open PasswordPin activity for setting PIN")
            val promptPinActivity = Intent(mycontext, PinPrompt::class.java)
            promptPinActivity.putExtra("changepassphrase", true)
            promptPinActivity.putExtra("noNext", true)
            promptPinActivity.putExtra("walletName", activeWallet)
            startActivity(promptPinActivity)
        }
        val checkUpdateButton = root.findViewById<LinearLayout>(R.id.button_check_update)
        //Disable update option for other builds
        if (BuildConfig.APPLICATION_ID != "co.anode.anodium") {
            checkUpdateButton.visibility = View.GONE
        } else {
            checkUpdateButton.visibility = View.VISIBLE
            checkUpdateButton.setOnClickListener {
                AnodeClient.checkNewVersion(true)
            }
        }
        val dataConsentButton = root.findViewById<LinearLayout>(R.id.button_data_collection)
        dataConsentButton.setOnClickListener {
            AboutDialog.show(requireActivity())
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
        val exportWalletButton = root.findViewById<ImageButton>(R.id.button_export_wallet)
        exportWalletButton.setOnClickListener {
            val walletUri = FileProvider.getUriForFile(requireContext(),BuildConfig.APPLICATION_ID +".provider", File("${AnodeUtil.filesDirectory}/pkt/$activeWallet.db"))
            val sendIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, walletUri)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, null)
            startActivity(shareIntent)
        }

        walletsSpinner = root.findViewById(R.id.wallet_selector)
        walletNames = AnodeUtil.getWalletFiles()
        val dataAdapter = ArrayAdapter(mycontext, android.R.layout.simple_spinner_dropdown_item, walletNames)

        walletsSpinner.adapter = dataAdapter
        activeWallet = prefs.getString("activeWallet", AnodeUtil.DEFAULT_WALLET_NAME).toString()
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
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (activeWallet != walletNames[position]) {
                    activeWallet = walletNames[position]
                    prefs.edit().putString("activeWallet", activeWallet).apply()
                    //disconnect VPN
                    disconnectVPN()
                    //close cjdns
                    AnodeUtil.stopCjdns()
                    AnodeUtil.clearWalletCache()
                    AnodeUtil.deleteWalletChainBackupFile()
                    //restart pld
                    AnodeUtil.stopPld()
                    //unlock selected wallet->getSecret
                    walletPasswordPrompt()
                    //connect back to VPN
                    notifyUserDialog()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

            }
        }

        val upgradeCheckbox = root.findViewById<CheckBox>(R.id.upgrade_checkbox)
        upgradeCheckbox.isChecked = prefs.getBoolean("preRelease", false)
        upgradeCheckbox.setOnClickListener {
            prefs.edit().putBoolean("preRelease", upgradeCheckbox.isChecked).apply()
        }
        val wifiCheckbox = root.findViewById<LinearLayout>(R.id.button_connectPktcube)
        //Initialize cube object
        CubeWifi.init(requireContext())
        CubeWifi.statusbar = root.findViewById(R.id.textview_status)
        val textConnect = root.findViewById<TextView>(R.id.text_connectPktcube)
        wifiCheckbox.setOnClickListener {
            if (textConnect.text.equals(getString(R.string.button_connect_pktcube))) {
                CubeWifi.connect()
                //CubeWifi.connectPasspoint()
                textConnect.text = getString(R.string.button_disconnect_pktcube)
            } else {
                textConnect.text = getString(R.string.button_connect_pktcube)
                CubeWifi.disconnect()
            }
        }

        val newUiCheckBox = root.findViewById<CheckBox>(R.id.use_newui_checkbox)
        newUiCheckBox.isChecked = !prefs.getBoolean("useNewUI", true)
        newUiCheckBox.setOnClickListener {
            prefs.edit().putBoolean("useNewUI", newUiCheckBox.isChecked).apply()
            Thread.sleep(500)
            //Restart app
            AnodeUtil.restartApp()
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            wifiCheckbox.visibility = View.GONE
        }
        return root
    }

    private fun getLocalIpAddress(): String {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        val list = interfaces.toList()
        for (element in list) {
            if (element.name.equals("wlan0")) {
                val inetAddresses = element.inetAddresses.toList()
                for (inetAddress in inetAddresses) {
                    //return ipv4 - wlan0
                    if (inetAddress.hostAddress.indexOf(":") < 0) {
                        return inetAddress.hostAddress
                    }
                }
            }
        }
        return ""
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
        activeWallet = prefs.getString("activeWallet", AnodeUtil.DEFAULT_WALLET_NAME).toString()
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

    private fun getSecret(resetCjdns: Boolean) {
        val jsonRequest = JSONObject()
        AnodeUtil.apiController.post(AnodeUtil.apiController.getSecretURL,jsonRequest) { response ->
            if ((response != null) && (response.has("secret") && !response.isNull("secret"))) {
                Log.i(LOGTAG, "wallet secret retrieved")
                val secret = response.getString("secret")
                val prefs = mycontext.getSharedPreferences(BuildConfig.APPLICATION_ID, AppCompatActivity.MODE_PRIVATE)
                prefs.edit().putString("wallet_secret", secret).apply()
                if (resetCjdns) {
                    //cjdns seed genconf
                    AnodeUtil.initializeCjdrouteConfFile(secret)
                    //launch cjdns
                    AnodeUtil.launchCJDNS()
                }
            }
        }
    }

    private fun getWalletSeed(showDialog:Boolean) {
        AnodeUtil.apiController.get(AnodeUtil.apiController.getSeedURL) { response ->
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
            } else if ((response != null) &&
                    response.has("error") &&
                    response.getString("error").contains("[LightningServer] is not yet ready")) {
                Log.e(BuildConfig.APPLICATION_ID, "Wallet is not unlocked")
                Toast.makeText(requireContext(), "Wallet is not unlocked.", Toast.LENGTH_LONG).show()
            } else {
                Log.e(BuildConfig.APPLICATION_ID, "Error in generating wallet seed")
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
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext(), R.style.EditWalletAlertDialog)
        builder.setTitle("Wallet name")
        if (addWallet) {
            builder.setMessage("New wallet")
        } else {
            builder.setMessage("Edit $activeWallet")
        }
        val existingWallets = AnodeUtil.getWalletFiles()
        val input = EditText(requireContext())
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        input.inputType = InputType.TYPE_TEXT_VARIATION_FILTER
        input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //nothing
            }

            override fun afterTextChanged(s: Editable?) {
                val regex = "([a-zA-Z0-9]*)".toRegex()
                if (!regex.matches(s.toString())) {
                    input.error = "Name should only contain alphanumeric."
                } else if (existingWallets.contains(s.toString())) {
                    input.error = "Name already exists"
                } else {
                    input.error = null
                }
            }
        })
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
                AnodeUtil.renameEncryptedWalletPreferences(activeWallet,walletName)
                renameWallet(walletName)
                //restart pld
                AnodeUtil.stopPld()
                //update dropdown
                updateWallets()
            }
            dialog.dismiss()
        }
        if (!addWallet) {
            builder.setNeutralButton("Delete") { dialog, _ ->
                //2nd confirmation before deleting wallet
                deleteWalletDialog()
            }
        }
        val alert: AlertDialog = builder.create()
        alert.show()
    }

    private fun deleteWalletDialog() {
        val builder: AlertDialog.Builder = AlertDialog.Builder(requireContext(), R.style.EditWalletAlertDialog)
        builder.setTitle("Delete wallet")
        builder.setMessage("Are you sure you want to permanently delete $activeWallet from this device? If yes enter the name of the wallet.")
        val input = EditText(requireContext())
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                //nothing
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                //nothing
            }

            override fun afterTextChanged(s: Editable?) {
                val regex = "([a-zA-Z0-9]*)".toRegex()
                if (!regex.matches(s.toString())) {
                    input.error = "Name contains only alphanumeric."
                } else {
                    input.error = null
                }
            }
        })
        builder.setPositiveButton("Cancel") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setNeutralButton("Delete") { _, _ ->
            var inputWallet = input.text.toString()
            inputWallet = inputWallet.removeSuffix(".db")
            if (inputWallet == activeWallet) {
                //Delete saved pin/password
                AnodeUtil.removeEncryptedWalletPreferences(inputWallet)
                //Delete Wallet
                AnodeUtil.deleteWallet(inputWallet)
                Toast.makeText(requireContext(), "Wallet $activeWallet has been deleted.", Toast.LENGTH_LONG).show()
                //change active wallet
                updateWallets()
            } else {
                Toast.makeText(requireContext(), "Wallet names do not match.", Toast.LENGTH_LONG).show()
            }
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
        AnodeUtil.apiController.post(AnodeUtil.apiController.unlockWalletURL,jsonRequest) { response ->
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
                getSecret(true)
            }
        }
    }

    private fun walletPasswordPrompt() {
        val builder = AlertDialog.Builder(mycontext)
        val input = EditText(mycontext)
        val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
        input.layoutParams = lp
        builder.setView(input)
        input.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        builder.setMessage("Please enter password for $activeWallet")
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
        alert.setCanceledOnTouchOutside(false)
        alert.show()
    }
}