package co.anode.anodium
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.util.*
import kotlin.collections.ArrayList


class VPNListAdapter(private val context: Context,
                     private var dataList: ArrayList<HashMap<String, String>>) : BaseAdapter() {
    private val API_VERSION = "0.3"
    private var API_FAVORITE_URL = "https://vpn.anode.co/api/$API_VERSION/vpn/servers/<server_public_key>/favorite/"

    var list = dataList
    private var tempdataList: ArrayList<HashMap<String, String>> = ArrayList(list)

    private val inflater: LayoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getCount(): Int { return dataList.size }
    override fun getItem(position: Int): Int { return position }
    override fun getItemId(position: Int): Long { return 0 }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        var dataitem = dataList[position]
        val holder: ViewHolder

        view = inflater.inflate(R.layout.list_row, parent, false)
        val prefs = context.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        holder = ViewHolder()
        holder.nameTextView = view.findViewById(R.id.row_name)
        holder.nameTextView.text = dataitem["name"]
        //holder.countryTextView = view.findViewById(R.id.row_country)
        //holder.countryTextView.text = dataitem["countryCode"]
        holder.countryImageView = view.findViewById(R.id.row_country)
        val id = context.resources.getIdentifier(dataitem["countryCode"]?.toLowerCase(Locale.ROOT), "drawable",context.packageName)
        holder.countryImageView.setImageResource(id)
        holder.speedTextView = view.findViewById(R.id.row_speed)
        holder.speedTextView.text = dataitem["speed"]
        holder.connectButton = view.findViewById(R.id.row_button)
        holder.favoriteButton = view.findViewById(R.id.button_favorite)
        holder.ratingbar = view.findViewById(R.id.list_ratingbar)
        if (dataitem["averageRating"].isNullOrEmpty() || dataitem["averageRating"] == "null") {
            holder.ratingbar.rating = 0.0f
        } else {
            holder.ratingbar.rating = dataitem["averageRating"]!!.toFloat()
        }

        if ((dataitem["isFavorite"] == "true") || (prefs.getBoolean("favorite_" + dataitem["name"], false))){
            holder.favoriteButton.setBackgroundResource(R.drawable.ic_baseline_favorite_24)
        } else {
            holder.favoriteButton.setBackgroundResource(R.drawable.ic_baseline_favorite_border_24)
        }

        holder.connectButton.setOnClickListener {
            AnodeClient.eventLog(context, "Button CONNECT to " + dataitem["name"])
            AnodeClient.AuthorizeVPN().execute(dataitem["publicKey"])
            (context as Activity).finish()
        }

        holder.favoriteButton.setOnClickListener {
            if (!prefs.getBoolean("favorite_" + dataitem["name"], false)) {
                AnodeClient.eventLog(context, "Button FAVORITE for " + dataitem["name"])
                toggleFavorite().execute("ADD")
                holder.favoriteButton.setBackgroundResource(R.drawable.ic_baseline_favorite_24)
                with(prefs.edit()) {
                    putBoolean("favorite_" + dataitem["name"], true)
                    commit()
                }
            } else {
                AnodeClient.eventLog(context, "Button UNFAVORITE for " + dataitem["name"])
                toggleFavorite().execute("DELETE")
                holder.favoriteButton.setBackgroundResource(R.drawable.ic_baseline_favorite_border_24)
                with(prefs.edit()) {
                    putBoolean("favorite_" + dataitem["name"], false)
                    commit()
                }
            }
        }
        view.tag = holder

        return view
    }

    private class ViewHolder {
        //lateinit var countryTextView: TextView
        lateinit var countryImageView: ImageView
        lateinit var nameTextView: TextView
        lateinit var speedTextView: TextView
        lateinit var connectButton: Button
        lateinit var favoriteButton: Button
        lateinit var ratingbar: RatingBar
    }

    fun setFilter(text: String?) {
        val text = text!!.toLowerCase(Locale.getDefault())
        list.clear()
        if (text.isEmpty()) {
            dataList.addAll(tempdataList)
        } else {
            for (i in 0 until tempdataList.size) {
                if (tempdataList[i]["name"]!!.toLowerCase(Locale.getDefault()).contains(text)) {
                    dataList.add(tempdataList[i])
                }
            }
        }
        notifyDataSetChanged()
    }

    inner class toggleFavorite() : AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg params: String?): String? {
            val prefs = context.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
            val url = API_FAVORITE_URL.replace("<server_public_key>", prefs!!.getString("ServerPublicKey", ""), true)
            return if (params[0]=="ADD") {
                AnodeClient.APIHttpReq(url, "", "POST", true, false)
            } else {
                AnodeClient.APIHttpReq(url, "", "DELETE", true, false)
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            Log.i(LOGTAG, "Received from $API_FAVORITE_URL: $result")
            if ((result.isNullOrBlank())) {
                //
            } else {
                try {
                    //
                } catch (e: Exception) {
                    Log.i(LOGTAG, e.toString())
                }
            }
        }
    }
}