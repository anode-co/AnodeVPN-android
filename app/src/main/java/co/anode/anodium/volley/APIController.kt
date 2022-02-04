package co.anode.anodium.volley

import org.json.JSONObject

class APIController constructor(serviceInjection: ServiceInterface): ServiceInterface {
    private val service: ServiceInterface = serviceInjection
    override fun get(url: String, completionHandler: (response: JSONObject?) -> Unit) {
        service.get(url, completionHandler)
    }

    override fun post(url: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        service.post(url, params, completionHandler)
    }
}