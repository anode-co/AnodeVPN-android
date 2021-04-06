package co.anode.anodium

import android.content.SharedPreferences
import android.util.Log
import com.github.lightningnetwork.lnd.lnrpc.*
import com.github.lightningnetwork.lnd.routerrpc.SendPaymentRequest
import com.google.common.io.BaseEncoding
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import javax.net.ssl.HostnameVerifier

object LndRPCController {
    private lateinit var mSecureChannel: ManagedChannel
    private lateinit var stub: WalletUnlockerGrpc.WalletUnlockerBlockingStub

    fun createSecurechannel() {
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

    fun createLocalWallet(preferences: SharedPreferences) {
        val gsr = stub.genSeed(GenSeedRequest.newBuilder().build())
        val password: ByteString = ByteString.copyFrom("password", Charsets.UTF_8)
        val bldr = InitWalletRequest.newBuilder().setWalletPassword(password)
        for (i in 0 until gsr.cipherSeedMnemonicCount) {
            Log.i(LOGTAG, gsr.getCipherSeedMnemonic(i))
            bldr.addCipherSeedMnemonic(gsr.getCipherSeedMnemonic(i))
        }
        val walletresponse = stub.initWallet(bldr.build())
        Log.i(LOGTAG, walletresponse.adminMacaroon.toStringUtf8())
        with(preferences.edit()) {
            putString("admin_macaroon", walletresponse.adminMacaroon.toStringUtf8())
            commit()
        }
    }

    fun openWallet(preferences: SharedPreferences) {
        stub = WalletUnlockerGrpc.newBlockingStub(mSecureChannel)
        //if we do not have a stored macaroon, create new local wallet
        if (preferences.getString("admin_macaroon", "") == "") {
            createLocalWallet(preferences)
        }
        val password: ByteString = ByteString.copyFrom("password", Charsets.UTF_8)
        val response = stub.unlockWallet(UnlockWalletRequest.newBuilder().setWalletPassword(password).build())
        val initialized = response.isInitialized
    }

    fun getPubKey() {
        val lndstub = LightningGrpc.newBlockingStub(mSecureChannel).withCallCredentials(null)
        val response = lndstub.getInfo(GetInfoRequest.getDefaultInstance())
        val pubkey = response.identityPubkey
    }

    fun generateRequest() {
        val addressRequest = NewAddressRequest.newBuilder().setTypeValue(0).build()
        val addressResponse = LightningGrpc.newBlockingStub(mSecureChannel).newAddress(addressRequest)
        addressResponse.address
    }

    fun getPaymentRequest() {
        var payrequest = PayReq.newBuilder()
        payrequest.paymentAddr

    }

    fun getBalance() {
        val walletBallanceRequest = WalletBalanceRequest.newBuilder().build()
        val walletBalanceResponse = LightningGrpc.newBlockingStub(mSecureChannel).walletBalance(walletBallanceRequest)
        val confirmedBalance = walletBalanceResponse.confirmedBalance/1073741824
        val unconfirmedBalance = walletBalanceResponse.unconfirmedBalance/1073741824
        val totalBalance = walletBalanceResponse.totalBalance/1073741824
    }

    fun sendCoins(address: String, amount: Long) {
        val sendcoinsrequest = SendCoinsRequest.newBuilder()
        sendcoinsrequest.addr = address
        sendcoinsrequest.amount = amount * 1073741824
        sendcoinsrequest.targetConf = 0
        sendcoinsrequest.satPerByte = 0
        sendcoinsrequest.sendAll = false
        sendcoinsrequest.label = ""
        sendcoinsrequest.minConfs = 1
        sendcoinsrequest.spendUnconfirmed = false

        val sendcoinsresponse = LightningGrpc.newBlockingStub(mSecureChannel).sendCoins(sendcoinsrequest.build())
        val hash = sendcoinsresponse.hashCode()

    }

    // ByteString values when using for example "paymentRequest.getDescriptionBytes()" can for some reason not directly be used as they are double in length
    private fun byteStringFromHex(hexString: String): ByteString? {
        val hexBytes = BaseEncoding.base16().decode(hexString.toUpperCase())
        return ByteString.copyFrom(hexBytes)
    }
}