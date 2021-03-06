package co.anode.anodium
import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.ListActivity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.*
import kotlin.collections.ArrayList


class VPNListAdapter(private val context: Context, private val fragmentManager: FragmentManager,
                     private var dataList: ArrayList<HashMap<String, String>>) : BaseAdapter() {
    var list = dataList
    private var tempdataList: ArrayList<HashMap<String, String>> = ArrayList(list)

    private val inflater: LayoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getCount(): Int { return dataList.size }
    override fun getItem(position: Int): Int { return position }
    override fun getItemId(position: Int): Long { return 0 }


    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        var dataitem = dataList[position]
        val holder: ViewHolder

        view = inflater.inflate(R.layout.list_row, parent, false)
        val prefs = context.getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)

        holder = ViewHolder()
        holder.infobox = view.findViewById<LinearLayout>(R.id.info_box)
        holder.nameTextView = view.findViewById(R.id.row_name)
        holder.nameTextView.text = dataitem["name"]
        holder.countryImageView = view.findViewById(R.id.row_country)
        val id = context.resources.getIdentifier("ic_"+dataitem["countryCode"]?.toLowerCase(Locale.ROOT), "drawable", context.packageName)
        holder.countryImageView.setImageResource(id)
        holder.connectButton = view.findViewById(R.id.button_smallconnectvpn)
        holder.ratingbar = view.findViewById(R.id.list_ratingbar)
        if (dataitem["averageRating"].isNullOrEmpty() || dataitem["averageRating"] == "null") {
            holder.ratingbar.rating = 0.0f
        } else {
            holder.ratingbar.rating = dataitem["averageRating"]!!.toFloat()
        }
        val signal = dataitem["quality"]?.toInt()
        val imageSignal = view.findViewById<ImageView>(R.id.serversignal)
        when (signal) {
            0 -> imageSignal.setBackgroundResource(R.drawable.ic_0_signal)
            1 -> imageSignal.setBackgroundResource(R.drawable.ic_1_signal)
            2 -> imageSignal.setBackgroundResource(R.drawable.ic_2_signals)
            3 -> imageSignal.setBackgroundResource(R.drawable.ic_3_signals)
        }


        holder.infobox.setOnClickListener {
            AnodeClient.eventLog(context, "Show VPN Server details " + dataitem["name"])
            //VPN Details
            val vpndetailsFragment: BottomSheetDialogFragment = VPNDetailsFragment()
            val args = Bundle()
            args.putString("name", dataitem["name"])
            args.putString("countryCode", dataitem["countryCode"]?.toLowerCase(Locale.ROOT))
            args.putString("publicKey", dataitem["publicKey"])
            args.putString("load", dataitem["load"])
            args.putFloat("averageRating", dataitem["averageRating"]!!.toFloat())
            vpndetailsFragment.arguments = args
            vpndetailsFragment.show(fragmentManager, "")
        }

        holder.connectButton.setOnClickListener {
            AnodeClient.eventLog(context, "Button CONNECT to " + dataitem["name"])
            val intent: Intent = Intent()
            intent.putExtra("action", "connect")
            intent.putExtra("publickey", dataitem["publicKey"])
            (context as Activity).setResult(RESULT_OK,intent)
            (context as Activity).finish()
        }

        view.tag = holder

        return view
    }

    private class ViewHolder {
        lateinit var countryImageView: ImageView
        lateinit var nameTextView: TextView
        lateinit var speedTextView: TextView
        lateinit var connectButton: Button
        lateinit var favoriteButton: Button
        lateinit var ratingbar: RatingBar
        lateinit var infobox: LinearLayout
    }

    fun setFilter(str: String?) {
        val text = str!!.toLowerCase(Locale.getDefault())
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


}