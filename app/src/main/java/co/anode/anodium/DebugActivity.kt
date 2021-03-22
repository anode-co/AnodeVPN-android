package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import co.anode.anodium.connection.lndConnection.LndConnection
import co.anode.anodium.connection.lndConnection.LndSSLSocketFactory
import co.anode.anodium.connection.lndConnection.MacaroonCallCredential
import co.anode.anodium.connection.manageWalletConfigs.WalletConfig
import co.anode.anodium.connection.manageWalletConfigs.WalletConfigsManager
import co.anode.anodium.lnd.LndLightningService
import co.anode.anodium.lnd.RemoteLndLightningService
import co.anode.anodium.util.Wallet
import com.github.lightningnetwork.lnd.lnrpc.*
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import org.apache.commons.codec.binary.Hex
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import javax.net.ssl.HostnameVerifier

class DebugActivity : AppCompatActivity() {
    private lateinit var mSecureChannel: ManagedChannel
    private lateinit var mLndLightningService: LndLightningService
    private lateinit var mMacaroon: MacaroonCallCredential
    private lateinit var mConnectionConfig: WalletConfig
    private val CERT_PATH = "/Users/user/Library/Application Support/Lnd/tls.cert"
    private val MACAROON_PATH = "/data/data/co.anode.anodium/files/admin.macaroon"
    private val HOST = "localhost"
    private val PORT = 10009
    private lateinit var stub: WalletUnlockerGrpc.WalletUnlockerBlockingStub

    private fun createWalletConfig(){
        val cert = null

        val macaroon: String = Hex.encodeHexString(
                Files.readAllBytes(Paths.get(MACAROON_PATH))
        )
        WalletConfigsManager.getInstance().addWalletConfig("mywallet", "local", "localhost", 10009, cert, macaroon)
    }
    private fun readSavedConnectionInfo() {
        // Load current wallet connection config
        mConnectionConfig = WalletConfigsManager.getInstance().currentWalletConfig
        // Generate Macaroon
        mMacaroon = MacaroonCallCredential(mConnectionConfig.macaroon)
    }
    private fun createConnection() {
        val hostnameVerifier: HostnameVerifier? = null
        mSecureChannel = OkHttpChannelBuilder
                .forAddress("localhost", 10009)
                .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                //.sslSocketFactory(LndSSLSocketFactory.create(mConnectionConfig)) // null = default SSLSocketFactory
                .sslSocketFactory(LndSSLSocketFactory.create(null))
                .build()
    }

    fun createWallet() {
        //TODO: find a way to generate macaroon for lnd connection
        //createWalletConfig()
        //readSavedConnectionInfo()
        // Channels are expensive to create. We want to create it once and then reuse it on all our requests.
        createConnection()
        val stub: WalletUnlockerGrpc.WalletUnlockerBlockingStub? = WalletUnlockerGrpc.newBlockingStub(mSecureChannel)

        // First start:
        val gsrq: GenSeedRequest = GenSeedRequest.newBuilder().build()
        //val gsr: GenSeedResponse? = stub?.genSeed(gsrq)
        val password: ByteString = ByteString.copyFrom("password", Charsets.UTF_8)
        val bldr = InitWalletRequest.newBuilder().setWalletPassword(password).build()

        /*if (gsr != null) {
            for (i in 0 until gsr.getCipherSeedMnemonicCount()) {
                //print(gsr.getCipherSeedMnemonic(i))
                bldr.setCipherSeedMnemonic(i, gsr.getCipherSeedMnemonic(i))
            }
        }*/
        //val response: InitWalletResponse = stub!!.initWallet(bldr)
        //mLndLightningService = RemoteLndLightningService(mSecureChannel, mMacaroon)
    }

    fun openwallet() {
        // Start lnd connection
        if (WalletConfigsManager.getInstance().hasAnyConfigs()) {
            //TimeOutUtil.getInstance().setCanBeRestarted(true)
            LndConnection.getInstance().openConnection()
            Wallet.getInstance().testLndConnectionAndLoadWallet()
        }
    }


    fun createSecurechannel() {
        val hostnameVerifier: HostnameVerifier? = null
        mSecureChannel = OkHttpChannelBuilder
                .forAddress("localhost", 10009)
                .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                .usePlaintext()
                //.sslSocketFactory(LndSSLSocketFactory.create(mConnectionConfig)) // null = default SSLSocketFactory
                //.sslSocketFactory(LndSSLSocketFactory.create(null))
                .build()
    }

    fun createLocalWallet() {
        val gsr = stub.genSeed(GenSeedRequest.newBuilder().build())
        val password: ByteString = ByteString.copyFrom("password", Charsets.UTF_8)
        val bldr = InitWalletRequest.newBuilder().setWalletPassword(password)
        for (i in 0 until gsr.cipherSeedMnemonicCount) {
            Log.i(LOGTAG, gsr.getCipherSeedMnemonic(i))
            bldr.addCipherSeedMnemonic(gsr.getCipherSeedMnemonic(i))
        }
        val walletresponse = stub.initWallet(bldr.build())
        Log.i(LOGTAG, walletresponse.adminMacaroon.toStringUtf8())
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        with(prefs!!.edit()) {
            putString("admin_macaroon", walletresponse.adminMacaroon.toStringUtf8())
            commit()
        }
    }

    fun unlockLocalWallet() {
        val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        stub = WalletUnlockerGrpc.newBlockingStub(mSecureChannel)
        if (prefs.getString("admin_macaroon", "") == "") {
            createLocalWallet()
        }
        val password: ByteString = ByteString.copyFrom("password", Charsets.UTF_8)
        val unlockwalletreq = UnlockWalletRequest.newBuilder().setWalletPassword(password).build()
        //val unlockwalletresponse = stub.unlockWallet(unlockwalletreq)
        //TODO: get macaroon from unlock wallet response?!
        mMacaroon = MacaroonCallCredential(prefs.getString("admin_macaroon", ""))
    }

    fun getPubKey() {
        //val lndstub = LightningGrpc.newBlockingStub(mSecureChannel).withCallCredentials(mMacaroon)
        mLndLightningService = RemoteLndLightningService(mSecureChannel, mMacaroon)

        val response = mLndLightningService.getInfo(GetInfoRequest.newBuilder().build())
        response.blockingGet().identityPubkey
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_debug)
        //actionbar
        val actionbar = supportActionBar
        //set actionbar title
        actionbar!!.title = "View Logs"
        //set back button
        actionbar.setDisplayHomeAsUpEnabled(true)
        val buttonSubmitLogs = findViewById<Button>(R.id.button_SubmitLogs)
        buttonSubmitLogs.setOnClickListener {

            /*Toast.makeText(baseContext, "Sending log files to Anode server...", Toast.LENGTH_LONG).show()
            AnodeClient.mycontext = baseContext
            AnodeClient.storeError(baseContext, "other", Throwable("User submitted logs"))
            AnodeClient.PostLogs()

             */
            /*
            createWallet()
            //openwallet()
            LndConnection.getInstance().openConnection()
            Wallet.getInstance().unlockWallet("password")
            val lndVersionString = "lnd version: " + Wallet.getInstance().lndVersionString.split(" commit")[0]

             */
            createSecurechannel()
            unlockLocalWallet()
            getPubKey()

        }
        val buttonDeleteAccount = findViewById<Button>(R.id.button_Deleteaccount)
        buttonDeleteAccount.setOnClickListener {
            Log.i(LOGTAG, "Delete account")
            AnodeClient.eventLog(baseContext, "Delete account selected")
            AnodeClient.DeleteAccount().execute()
        }

        Thread(Runnable {
            Log.i(LOGTAG, "DebugActivity.RefreshValues startup")
            val anodeUtil = AnodeUtil(application)
            while (true) {
                this.runOnUiThread(Runnable {
                    val appversion = findViewById<TextView>(R.id.text_AppVersion)
                    appversion.text = "App Version: " + BuildConfig.VERSION_NAME
                    val ipv4 = findViewById<TextView>(R.id.text_ipv4)
                    ipv4.text = CjdnsSocket.ipv4Address
                    val cjdnsipv6 = findViewById<TextView>(R.id.text_cjdnsipv6)
                    val nodeinfo = CjdnsSocket.Core_nodeInfo()
                    cjdnsipv6.text = nodeinfo["myIp6"].str()
                    val internetipv6 = findViewById<TextView>(R.id.text_internetipv6)
                    internetipv6.text = CjdnsSocket.ipv6Address
                    val pubkey = findViewById<TextView>(R.id.text_pubkey)
                    pubkey.text = anodeUtil.getPubKey()
                    val nodeLink = findViewById<TextView>(R.id.text_nodes)
                    val link: Spanned = HtmlCompat.fromHtml("<a href='http://h.snode.cjd.li/#" + nodeinfo["myIp6"].str() + "'>Find yourself on the map</a>", HtmlCompat.FROM_HTML_MODE_LEGACY)
                    nodeLink.movementMethod = LinkMovementMethod.getInstance()
                    nodeLink.text = link
                    val username = findViewById<TextView>(R.id.text_username)
                    val prefs = getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
                    username.text = prefs.getString("username", "")
                    val node = findViewById<TextView>(R.id.text_node)
                    node.text = prefs.getString("ServerPublicKey", "")
                    val peerstats = findViewById<TextView>(R.id.text_peerstats)
                    val peers: ArrayList<Benc.Bdict> = CjdnsSocket.InterfaceController_peerStats()
                    var info = ""
                    for (i in 0 until peers.count()) {
                        info += peers[i]["lladdr"].toString() + " " + peers[i]["addr"].toString() + " " + peers[i]["state"].toString() + " in " + peers[i]["recvKbps"].toString() + " out " + peers[i]["sendKbps"].toString() + " " + peers[i]["user"].toString() + "\n"
                    }
                    peerstats.text = info

                    val listconnnections = findViewById<TextView>(R.id.text_listconnections)
                    val connections: ArrayList<Benc.Bdict> = CjdnsSocket.IpTunnel_listConnections()
                    info = ""
                    for (i in 0 until connections.count()) {
                        info += connections[i].toString()
                    }
                    listconnnections.text = info
                    val publicip = findViewById<TextView>(R.id.text_publicIP)
                    publicip.text = "updating ip..."
                    GetPublicIP().execute(publicip)
                    //val inforespons = Rpc.GetInfoResponse.newBuilder()
                    /*val walletResp = Rpc.WalletBalanceResponse.newBuilder()
                    val walletReq = Rpc.WalletBalanceRequest.newBuilder()
                    walletReq.walletResp.totalBalance
                    val rpc = findViewById<TextView>(R.id.text_rpc)
                    rpc.text = "RPC msg: " + inforespons.version*/
                })
                Thread.sleep(8000)
            }
        }, "DebugActivity.RefreshValues").start()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    class GetPublicIP(): AsyncTask<TextView, Void, String>() {
        private var ipText:TextView? = null

        override fun doInBackground(vararg params: TextView?): String {
            ipText = params[0]
            return try {
                URL("https://api.ipify.org").readText(Charsets.UTF_8)
            } catch (e: Exception) {
                "error in getting public ip"
            }
        }
        @SuppressLint("SetTextI18n")
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            ipText?.post(Runnable { ipText?.text = result })
        }
    }
}

class DebugException(message: String): Exception(message)