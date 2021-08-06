package co.anode.anodium

import android.content.SharedPreferences
import android.util.Log
import com.github.lightningnetwork.lnd.lnrpc.*
import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import java.lang.Exception
import javax.net.ssl.HostnameVerifier

object LndRPCController {
    private lateinit var mSecureChannel: ManagedChannel
    //private lateinit var stub: WalletUnlockerGrpc.WalletUnlockerBlockingStub
    private const val LOGTAG = "co.anode.anodium"

    fun createSecurechannel() {
        Log.i(LOGTAG, "LndRPCController.createSecurechannel")
        val hostnameVerifier: HostnameVerifier? = null
        mSecureChannel = OkHttpChannelBuilder
                .forAddress("localhost", 10009)
                .hostnameVerifier(hostnameVerifier) // null = default hostnameVerifier
                .usePlaintext()
                .build()
    }

    fun clearWalletinPreferences(preferences: SharedPreferences) {
        with(preferences.edit()) {
            putString("admin_macaroon", "")
            commit()
        }
    }

    fun createLocalWallet(preferences: SharedPreferences, password:String):String {
        Log.i(LOGTAG, "LndRPCController.createLocalWallet")
        var result = ""
        val walletpassword: ByteString = ByteString.copyFrom(password, Charsets.UTF_8)
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        try {
            var stub = WalletUnlockerGrpc.newBlockingStub(mSecureChannel)
            val gsr = stub.genSeed(GenSeedRequest.newBuilder().build())
            val bldr = InitWalletRequest.newBuilder().setWalletPassword(walletpassword)
            val seed: MutableList<String> = mutableListOf()
            for (i in 0 until gsr.cipherSeedMnemonicCount) {
                seed.add(gsr.getCipherSeedMnemonic(i))
                bldr.addCipherSeedMnemonic(gsr.getCipherSeedMnemonic(i))
            }
            val walletresponse = stub.initWallet(bldr.build())

            if (walletresponse.isInitialized) {
                Log.i(LOGTAG, "LndRPCController wallet created")
                with(preferences.edit()) {
                    //adminMacaroon is empty!
                    putString("admin_macaroon", walletresponse.adminMacaroon.toStringUtf8())
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

    fun openWallet(preferences: SharedPreferences):String {
        Log.i(LOGTAG, "LndRPCController.openWallet")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        try {
            var stub = WalletUnlockerGrpc.newBlockingStub(mSecureChannel)
            val walletpassword: ByteString =
                ByteString.copyFrom(preferences.getString("walletpassword", ""), Charsets.UTF_8)
            val response = stub.unlockWallet(
                UnlockWalletRequest.newBuilder().setWalletPassword(walletpassword).build()
            )
        } catch(e:Exception) {
            Log.e(LOGTAG, e.toString())
            return e.toString()
        }
        return "OK"
    }

    fun getInfo(): GetInfoResponse? {
        Log.i(LOGTAG, "LndRPCController.getPubKey")
        if (!this::mSecureChannel.isInitialized) { return null }
        return try {
            val lndstub = LightningGrpc.newBlockingStub(mSecureChannel).withCallCredentials(null)
            lndstub.getInfo(GetInfoRequest.getDefaultInstance())
        } catch (e: Exception) {
            Log.e(LOGTAG, e.toString())
            null
        }
    }

    fun generateAddress() : String {
        Log.i(LOGTAG, "LndRPCController.generateAddress")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        return try {
            val addressRequest = NewAddressRequest.newBuilder().setTypeValue(0).build()
            val addressResponse =
                LightningGrpc.newBlockingStub(mSecureChannel).newAddress(addressRequest)
            addressResponse.address
        } catch (e:Exception) {
            Log.e(LOGTAG, e.toString())
            return ""
        }
    }

    fun getConfirmedBalance():Long {
        Log.i(LOGTAG, "LndRPCController.getConfirmedBalance")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        val walletBallanceRequest = WalletBalanceRequest.newBuilder().build()
        val walletBalanceResponse = LightningGrpc.newBlockingStub(mSecureChannel).walletBalance(walletBallanceRequest)
        return walletBalanceResponse.confirmedBalance/1073741824
    }

    fun getUncofirmedBalance():Long {
        Log.i(LOGTAG, "LndRPCController.getUncofirmedBalance")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        val walletBallanceRequest = WalletBalanceRequest.newBuilder().build()
        val walletBalanceResponse = LightningGrpc.newBlockingStub(mSecureChannel).walletBalance(walletBallanceRequest)
        return walletBalanceResponse.unconfirmedBalance / 1073741824
    }

    fun getTotalBalance(): Float {
        Log.i(LOGTAG, "LndRPCController.getTotalBalance")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        return try {
            val walletBallanceRequest = WalletBalanceRequest.newBuilder().build()
            val walletBalanceResponse = LightningGrpc.newBlockingStub(mSecureChannel).walletBalance(walletBallanceRequest)
            (walletBalanceResponse.totalBalance / 1073741824).toFloat()
        } catch (e:Exception) {
            Log.e(LOGTAG, e.toString())
            //throw LndRPCException("LndRPCException: "+e.message)
            //Return an empty list
            -1.1f
        }
    }

    fun sendCoins(address: String, amount: Long) {
        Log.i(LOGTAG, "LndRPCController.sendCoins")
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        try {
            val sendcoinsrequest = SendCoinsRequest.newBuilder()
            sendcoinsrequest.addr = address
            sendcoinsrequest.amount = amount * 1073741824
            sendcoinsrequest.targetConf = 0
            sendcoinsrequest.satPerByte = 0
            sendcoinsrequest.sendAll = false
            sendcoinsrequest.label = ""
            sendcoinsrequest.minConfs = 1
            sendcoinsrequest.spendUnconfirmed = false
            val sendcoinsresponse =
                LightningGrpc.newBlockingStub(mSecureChannel).sendCoins(sendcoinsrequest.build())
            val hash = sendcoinsresponse.hashCode()
        } catch (e: Exception) {
            Log.e(LOGTAG, e.toString())
        }
    }

    fun getTransactions(): MutableList<Transaction> {
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        return try {
            val transactionsrequest = GetTransactionsRequest.newBuilder().build()
            val transactions = LightningGrpc.newBlockingStub(mSecureChannel).getTransactions(transactionsrequest)
            transactions.transactionsList
        } catch (e:Exception) {
            Log.e(LOGTAG, e.toString())
            //throw LndRPCException("LndRPCException: "+e.message)
            //Return an empty list
            mutableListOf<Transaction>()
        }
    }

    // ByteString values when using for example "paymentRequest.getDescriptionBytes()" can for some reason not directly be used as they are double in length
    private fun byteStringFromHex(hexString: String): ByteString? {
        val hexBytes = BaseEncoding.base16().decode(hexString.toUpperCase())
        return ByteString.copyFrom(hexBytes)
    }
}

class LndRPCException(message: String): Exception(message)