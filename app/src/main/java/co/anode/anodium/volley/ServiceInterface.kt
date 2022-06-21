package co.anode.anodium.volley

import org.json.JSONArray
import org.json.JSONObject

interface ServiceInterface {
    fun get(url: String, completionHandler: (response: JSONObject?) -> Unit)
    fun getArray(url: String, completionHandler: (response: JSONArray?) -> Unit)
    fun post(url: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit)
}