package co.anode.anodium.volley

import org.json.JSONObject

class APIController constructor(serviceInjection: ServiceInterface): ServiceInterface {
    private val baseRestAPIURL = "http://localhost:8080/api/v1"
    //Create seed
    val createSeedURL = "$baseRestAPIURL/util/seed/create"
    //Wallet endpoints
    val unlockWalletURL = "$baseRestAPIURL/wallet/unlock"
    val walletCreateURL = "$baseRestAPIURL/wallet/create"
    val getBalanceURL = "http://127.0.0.1:8080/api/v1/wallet/balance"
    val getAddressBalancesURL = "$baseRestAPIURL/wallet/address/balances"
    val getNewAddressURL = "$baseRestAPIURL/wallet/address/create"
    val getTransactionsURL = "http://127.0.0.1:8080/api/v1/wallet/transaction/query"
    val getSeedURL = "$baseRestAPIURL/wallet/seed"
    val sendFromURL = "$baseRestAPIURL/wallet/transaction/sendfrom"
    val changePassphraseURL = "$baseRestAPIURL/wallet/changepassphrase"
    val checkPassphraseURL = "$baseRestAPIURL/wallet/checkpassphrase"
    val getSecretURL = "$baseRestAPIURL/wallet/getsecret"
    //GetInfo
    val getInfoURL = "$baseRestAPIURL/meta/getinfo"

    private val service: ServiceInterface = serviceInjection
    override fun get(url: String, completionHandler: (response: JSONObject?) -> Unit) {
        service.get(url, completionHandler)
    }

    override fun post(url: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        service.post(url, params, completionHandler)
    }
}