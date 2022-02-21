package co.anode.anodium.volley

import org.json.JSONObject

class APIController constructor(serviceInjection: ServiceInterface): ServiceInterface {
    private val baseRestAPIURL = "http://localhost:8080/api/v1"
    val unlockWalletURL = "$baseRestAPIURL/wallet/unlock"
    val getInfoURL = "$baseRestAPIURL/meta/getinfo"
    val getBalanceURL = "http://127.0.0.1:8080/api/v1/lightning/walletbalance"
    val getNewAddressURL = "$baseRestAPIURL/lightning/getnewaddress"
    val getTransactionsURL = "http://127.0.0.1:8080/api/v1/lightning/gettransactions"
    val sendFromURL = "$baseRestAPIURL/lightning/sendfrom"

    private val service: ServiceInterface = serviceInjection
    override fun get(url: String, completionHandler: (response: JSONObject?) -> Unit) {
        service.get(url, completionHandler)
    }

    override fun post(url: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        service.post(url, params, completionHandler)
    }
}