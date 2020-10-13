package co.anode.anodium

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_rating.*
import kotlinx.android.synthetic.main.fragment_rating.view.*
import org.json.JSONException
import org.json.JSONObject
import java.io.File

class RatingFragment : Fragment() {
    private val API_VERSION = "0.3"
    private var API_RATING_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/servers/<public_key>/rating/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_rating, container, false)
        v.buttonSubmitRating.setOnClickListener{
            this.context?.let { it1 -> AnodeClient.eventLog(it1, "Button RATING clicked") }
            val ratingBar: RatingBar = v.findViewById(R.id.ratingBar)
            submitRating().execute(ratingBar.rating.toString())
            //activity?.supportFragmentManager?.popBackStack()
        }

        return v
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RatingFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
                RatingFragment().apply {
                    arguments = Bundle().apply {
                        //putString(ARG_PARAM1, param1)
                        //putString(ARG_PARAM2, param2)
                    }
                }
    }

    inner class submitRating() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val prefs = context?.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            val url = API_RATING_URL.replace("<public_key>", prefs!!.getString("ServerPublicKey", ""),true)
            val jsonObject = JSONObject()
            jsonObject.accumulate("rating", params[0])
            return AnodeClient.APIHttpReq(url, jsonObject.toString(), "POST", true, false)
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG,"Received from $API_RATING_URL: $result")
            if ((result.isNullOrBlank())) {
                //
            } else {
                try {
                    val json = JSONObject(result)
                    if (json.has("averageRating")) {
                        Toast.makeText(context, "Rating submitted successfully", Toast.LENGTH_SHORT).show()
                    }
                    else {
                        Toast.makeText(context, "Rating could not be submitted", Toast.LENGTH_SHORT).show()
                    }
                } catch (e:Exception) {
                    Log.i(LOGTAG,e.toString())
                }
            }
            activity?.supportFragmentManager?.popBackStack()
        }
    }
}