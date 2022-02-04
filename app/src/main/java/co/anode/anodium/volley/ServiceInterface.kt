package co.anode.anodium.volley

import org.json.JSONObject

interface ServiceInterface {
    fun get(url: String, completionHandler: (response: JSONObject?) -> Unit)
    fun post(url: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit)
}