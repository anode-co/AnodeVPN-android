package co.anode.anodium

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.gson.JsonObject
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import lnrpc.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import javax.net.ssl.HostnameVerifier
import lnrpc.Rpc.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToLong

object LndRPCController {
    private lateinit var mSecureChannel: ManagedChannel
    private const val LOGTAG = "co.anode.anodium"
    private const val PostErrorLogs = false
    private const val baseRestAPIURL = "http://localhost:8080"
    private const val getInfo2URL = "$baseRestAPIURL/pkt/v1/getinfo2"
    private const val getBalanceURL = "$baseRestAPIURL/v1/balance/blockchain"
    private const val getNewAddressURL = "$baseRestAPIURL/pkt/v1/getnewaddress/false"
    private const val unlockWalletURL = "$baseRestAPIURL/v1/unlockwallet"
    private const val getTransactionsURL = "$baseRestAPIURL/v1/transactions"
    private var volleyQueue: RequestQueue? = null

    fun getRequest(ctx: Context, url: String): String{
        if (volleyQueue == null) {
            volleyQueue = Volley.newRequestQueue(ctx)
        }
        var stringResponse = ""
        val stringRequest = JsonObjectRequest(Request.Method.GET, url, null, {
                response -> stringResponse = response.toString()
        }, { error -> stringResponse = "Error: $error" })
        volleyQueue!!.add(stringRequest)
        return stringResponse
    }

    fun postRequest(ctx: Context, url: String, jsonRequest: JSONObject): String {
        if (volleyQueue == null) {
            volleyQueue = Volley.newRequestQueue(ctx)
        }
        var stringResponse = ""
        val stringRequest = JsonObjectRequest(Request.Method.POST, url, jsonRequest, {
            response -> stringResponse = response.toString()
        }, { error -> stringResponse = "Error: $error" })
        volleyQueue!!.add(stringRequest)
        return stringResponse
    }
    /**
     * Returns a JSON containing information about neutrino, wallet and lighning
     * using the getinfo2 REST command
     *
     * @param Context
     * @return JSONObject
     */
    fun getInfo(ctx: Context): JSONObject {
        Log.i(LOGTAG, "LndRPCController.getInfo")
        val infoString = getRequest(ctx, getInfo2URL)
        if (infoString.isNotEmpty()) {
            return JSONObject(infoString)
        }
        return  JSONObject()
    }

    /**
     * Returns the wallet's balance in satoshis
     *
     * @param Context
     * @return Double
     */
//    fun getBalance(ctx: Context): String {
//        Log.i(LOGTAG, "LndRPCController.getBalance")
//        val balanceString = getRequest(ctx, getBalanceURL)
//        if (balanceString.isNotEmpty()) {
//            val json = JSONObject(balanceString)
//            return satoshisToPKT( json.getString("total_balance").toLong())
//        }
//        return "unknown"
//    }

    /**
     * Returns a PKT address after making a REST call to getnewaddress
     *
     * @param Context
     */
    fun generateAddress(ctx: Context) : String {
        Log.i(LOGTAG, "LndRPCController.generateAddress")
        val response = JSONObject(postRequest(ctx, getNewAddressURL, JSONObject("")))
        if (response.has("address")) {
            return response.getString("address")
        }
        return ""
    }
    fun satoshisToPKT(satoshis: Long):String {
        var amount = satoshis.toFloat()
        val onePKT = 1073741824
        val mPKT = 1073741.824F
        val uPKT  = 1073.741824F
        val nPKT  = 1.073741824F
        if (amount > 1000000000) {
            amount /= onePKT
            return "PKT %.2f".format(amount)
        } else if (amount > 1000000) {
            amount /= mPKT
            return "mPKT %.2f".format(amount)
        } else if (amount > 1000) {
            amount /= uPKT
            return "μPKT %.2f".format(amount)
        } else if (amount < 1000000000) {
            amount /= onePKT
            return "PKT %.2f".format(amount)
        } else if (amount < 1000000) {
            amount /= mPKT
            return "mPKT %.2f".format(amount)
        } else if (amount < 1000) {
            amount /= uPKT
            return "μPKT %.2f".format(amount)
        } else {
            amount /= nPKT
            return "nPKT %.2f".format(amount)
        }
    }
    /**
     * Unlocks the PKT wallet using unlockwallet REST command
     *
     * @param Context
     * @return Boolean
     */
    fun unlockWallet(ctx: Context, password: String): Boolean {
        Log.i(LOGTAG, "LndRPCController.unlockWallet")
        var jsonRequest = JSONObject()
        val bytesPassword: ByteString = ByteString.copyFrom(password, Charsets.UTF_8)
        jsonRequest.put("wallet_password", bytesPassword)
        val response = JSONObject(postRequest(ctx, unlockWalletURL, jsonRequest))
        if (response.length() > 2) {
            Log.d(LOGTAG, "Error trying to unlock wallet: $response")
            return false
        }
        Log.i(LOGTAG, "PKT Wallet opened!")
        return true
    }

    /**
     * Retrieves transactions from unlocked wallet
     * using the transactions REST command
     *
     * @returns JSONArray List of transactions
     */
    fun getTransactions(ctx: Context): JSONArray {
        Log.i(LOGTAG, "LndRPCController.getTransactions")
        var jsonRequest = JSONObject()
        jsonRequest.put("coinbase", 1)
        val stringTransactions = postRequest(ctx, getTransactionsURL, jsonRequest)
        if (stringTransactions.isNotEmpty()) {
            val json = JSONObject(stringTransactions)
            return json.getJSONArray("transactions")
        }
        return JSONArray()
    }

    fun createSecurechannel() {
        Log.i(LOGTAG, "LndRPCController.createSecurechannel")
        val hostnameVerifier: HostnameVerifier? = null
        mSecureChannel = OkHttpChannelBuilder
                .forAddress("localhost", 10009)
                .maxInboundMessageSize(100000000)
                .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                .usePlaintext()
                .build()
    }

    fun createLocalWallet(preferences: SharedPreferences, password:String):String {
        Log.i(LOGTAG, "LndRPCController.createLocalWallet")
        var result: String
        val walletpassword: ByteString = ByteString.copyFrom(password, Charsets.UTF_8)
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        try {
            val stub = WalletUnlockerGrpc.newBlockingStub(mSecureChannel)
            val gsr = stub.genSeed(Walletunlocker.GenSeedRequest.newBuilder().build())
            val bldr = Walletunlocker.InitWalletRequest.newBuilder().setWalletPassword(walletpassword)
            val seed: MutableList<String> = mutableListOf()
            for (i in 0 until gsr.cipherSeedMnemonicCount) {
                seed.add(gsr.getCipherSeedMnemonic(i))
                bldr.addCipherSeedMnemonic(gsr.getCipherSeedMnemonic(i))
            }
            val walletresponse = stub.initWallet(bldr.build())

            if (walletresponse.isInitialized) {
                Log.i(LOGTAG, "LndRPCController wallet created")
                with(preferences.edit()) {
                    putString("walletpassword", password)
                    putBoolean("lndwalletopened", true)
                    commit()
                }
                result = "Success"
                for (i in 0 until seed.size ) {
                    result += seed[i]+" "
                }
            } else {
                result = "Error"
            }
        }catch (e:Exception) {
            result = e.toString()
        }
        return result
    }

    @SuppressLint("CheckResult")
    fun openWallet(preferences: SharedPreferences):String {
        Log.i(LOGTAG, "LndRPCController.openWallet")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        try {
            val stub = WalletUnlockerGrpc.newBlockingStub(mSecureChannel)
            val walletpassword: ByteString = ByteString.copyFrom(preferences.getString("walletpassword", ""), Charsets.UTF_8)
            stub.unlockWallet(Walletunlocker.UnlockWalletRequest.newBuilder().setWalletPassword(walletpassword).build())
        } catch(e:Exception) {
            Log.e(LOGTAG, e.toString())
            preferences.edit().putBoolean("lndwalletopened", false).apply()
            if (PostErrorLogs) {
                Thread({
                    AnodeClient.httpPostMessage("lnd", "Failed to open wallet ${e.toString()}")
                }, "LndRPCController.UploadMessageThread").start()
            }
            return e.toString()
        }
        preferences.edit().putBoolean("lndwalletopened", true).apply()
        return "OK"
    }

    fun getAddresses(): MutableList<GetAddressBalancesResponseAddr>? {
        Log.i(LOGTAG, "LndRPCController.getAddresses")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        return try {
            val addressBalancesRequest = GetAddressBalancesRequest.newBuilder().build()
            val addressBalancesResponce = LightningGrpc.newBlockingStub(mSecureChannel).getAddressBalances(addressBalancesRequest)
            addressBalancesResponce.addrsList
        } catch (e:Exception) {
            Log.e(LOGTAG, e.toString())
            if (PostErrorLogs) {
                Thread({
                    AnodeClient.httpPostMessage("lnd", "Failed to get addresses ${e.toString()}")
                }, "LndRPCController.UploadMessageThread").start()
            }
            null
        }
    }

    fun sendCoins(address: String, amount: Long): String {
        Log.i(LOGTAG, "LndRPCController.sendCoins")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        try {
            val sendCoinsRequest = SendCoinsRequest.newBuilder()
            sendCoinsRequest.addr = address
            sendCoinsRequest.amount = (amount.toDouble() * 1073741824).roundToLong()
            sendCoinsRequest.targetConf = 0
            sendCoinsRequest.satPerByte = 0
            sendCoinsRequest.sendAll = false
            sendCoinsRequest.label = ""
            sendCoinsRequest.minConfs = 1
            sendCoinsRequest.spendUnconfirmed = false
            val sendcoinsresponse = LightningGrpc.newBlockingStub(mSecureChannel).sendCoins(sendCoinsRequest.build())
            //sendcoinsresponse.hashCode()
            return "OK"
        } catch (e: Exception) {
            Log.e(LOGTAG, e.toString())
            if (PostErrorLogs) {
                Thread({
                    AnodeClient.httpPostMessage("lnd", "Failed to send coins ${e.toString()}")
                }, "LndRPCController.UploadMessageThread").start()
            }
            return e.toString()
        }
    }

    fun isPldRunning(): Boolean {
        val process = Runtime.getRuntime().exec("pidof pld")
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
        return bufferedReader.readText().isNotEmpty()
    }
}

class LndRPCException(message: String): Exception(message)