package co.anode.anodium.volley

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import org.json.JSONObject

class ServiceVolley : ServiceInterface {
    val TAG = ServiceVolley::class.java.simpleName

    override fun get(url: String, completionHandler: (response: JSONObject?) -> Unit) {
        val jsonObjReq = object : JsonObjectRequest(Method.GET, url, null,
            Response.Listener { response ->
                Log.d(TAG, "/post request OK! Response: $response")
                completionHandler(response)
            },
            Response.ErrorListener { error ->
                VolleyLog.e(TAG, "/post request fail! Error: ${error.message}")
                if ((error.networkResponse != null) && (error.networkResponse.data != null )) {
                    val errorString = String(error.networkResponse.data)
                    val jsonError = JSONObject()
                    jsonError.put("error", errorString)
                    completionHandler(jsonError)
                } else {
                    completionHandler(JSONObject("{}"))
                }
            }) {}
        jsonObjReq.retryPolicy = DefaultRetryPolicy(4000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)
    }

    override fun post(url: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val jsonObjReq = object : JsonObjectRequest(Method.POST, url, params,
            Response.Listener { response ->
                Log.d(TAG, "/post request OK! Response: $response")
                completionHandler(response)
            },
            Response.ErrorListener { error ->
                VolleyLog.e(TAG, "/post request fail! Error: ${error.message}")
                if ((error.networkResponse != null) && (error.networkResponse.data != null )) {
                    val errorString = String(error.networkResponse.data)
                    val jsonError = JSONObject()
                    jsonError.put("error", errorString)
                    completionHandler(jsonError)
                } else {
                    completionHandler(JSONObject("{}"))
                }
            }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers["Content-Type"] = "application/json"
                return headers
            }
        }
        //Increase timeout when we are creating a wallet
        if (url.contains("wallet/create")) {
            jsonObjReq.retryPolicy = DefaultRetryPolicy(40000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        } else if (url.contains("wallet/unlock")) {
            jsonObjReq.retryPolicy = DefaultRetryPolicy(15000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        } else {
            jsonObjReq.retryPolicy = DefaultRetryPolicy(4000, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        }
        BackendVolley.instance?.addToRequestQueue(jsonObjReq, TAG)
    }
}