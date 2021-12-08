package co.anode.anodium

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.Log
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import lnrpc.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Exception
import javax.net.ssl.HostnameVerifier
import lnrpc.Rpc.*

object LndRPCController {
    private lateinit var mSecureChannel: ManagedChannel
    private const val LOGTAG = "co.anode.anodium"
    private const val PostErrorLogs = false

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

    fun getInfo(): Metaservice.GetInfo2Response? {
        Log.i(LOGTAG, "LndRPCController.getPubKey")
        if (!this::mSecureChannel.isInitialized) { return null }
        return try {
            val lndstub = LightningGrpc.newBlockingStub(mSecureChannel).withCallCredentials(null)
            val getinforesponse = lndstub.getInfo(GetInfoRequest.getDefaultInstance())
            val metaservice = MetaServiceGrpc.newBlockingStub(mSecureChannel)
            metaservice.getInfo2(Metaservice.GetInfo2Request.newBuilder().setInfoResponse(getinforesponse).build())
        } catch (e: Exception) {
            Log.e(LOGTAG, e.toString())
            if (PostErrorLogs) {
                Thread({
                    AnodeClient.httpPostMessage("lnd", "Failed to get info ${e.toString()}")
                }, "LndRPCController.UploadMessageThread").start()
            }
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
            val addressResponse = LightningGrpc.newBlockingStub(mSecureChannel).newAddress(addressRequest)
            addressResponse.address
        } catch (e:Exception) {
            Log.e(LOGTAG, e.toString())
            if (PostErrorLogs) {
                Thread({
                    AnodeClient.httpPostMessage("lnd", "Failed to generate address ${e.toString()}")
                }, "LndRPCController.UploadMessageThread").start()
            }
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
            if (PostErrorLogs) {
                Thread({
                    AnodeClient.httpPostMessage("lnd", "Failed to get balance ${e.toString()}")
                }, "LndRPCController.UploadMessageThread").start()
            }
            -1.0f
        }
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
            sendCoinsRequest.amount = amount * 1073741824
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

    fun getTransactions(): MutableList<Transaction> {
        if (!this::mSecureChannel.isInitialized) {
            createSecurechannel()
        }
        return try {
            //coinbase 0=Include, 1=Exclude, 2=Only
                //TODO: check TxnsLimit
            val transactionsRequest = GetTransactionsRequest.newBuilder()
                .setCoinbase(1)
                .build()
            val transactions = LightningGrpc.newBlockingStub(mSecureChannel).getTransactions(transactionsRequest)
            transactions.transactionsList
        } catch (e:Exception) {
            Log.e(LOGTAG, e.toString())
            if (PostErrorLogs) {
                Thread({
                    AnodeClient.httpPostMessage("lnd", "Failed to get transactions ${e.toString()}")
                }, "LndRPCController.UploadMessageThread").start()
            }
            //Return an empty list
            mutableListOf<Transaction>()
        }
    }

    fun isPltdRunning(): Boolean {
        val process = Runtime.getRuntime().exec("pidof pltd")
        val bufferedReader = BufferedReader(InputStreamReader(process.inputStream))
        return bufferedReader.readText().isNotEmpty()
    }
}

class LndRPCException(message: String): Exception(message)