package co.anode.anodevpn
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class VPNListAdapter(private val context: Context,
                     private var dataList: ArrayList<HashMap<String, String>>) : BaseAdapter() {

    var list = dataList
    private var tempdataList: ArrayList<HashMap<String, String>> = ArrayList(list)

    private val inflater: LayoutInflater = this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
    override fun getCount(): Int { return dataList.size }
    override fun getItem(position: Int): Int { return position }
    override fun getItemId(position: Int): Long { return 0 }

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var dataitem = dataList[position]
        val rowView = inflater.inflate(R.layout.list_row, parent, false)
        rowView.findViewById<TextView>(R.id.row_name).text = dataitem.get("name")
        rowView.findViewById<TextView>(R.id.row_country).text = dataitem.get("countryCode")
        rowView.findViewById<TextView>(R.id.row_speed).text = dataitem.get("speed")

        rowView.tag = position
        return rowView
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
}