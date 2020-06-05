package co.anode.anodevpn
import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import java.util.*
import kotlin.collections.ArrayList


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
        val view: View
        var dataitem = dataList[position]
        val holder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_row, parent, false)

            holder = ViewHolder()
            holder.nameTextView = view.findViewById(R.id.row_name)
            holder.nameTextView.text = dataitem["name"]
            holder.countryTextView = view.findViewById(R.id.row_country)
            holder.countryTextView.text = dataitem["countryCode"]
            holder.speedTextView = view.findViewById(R.id.row_speed)
            holder.speedTextView.text = dataitem["speed"]
            holder.connectButton = view.findViewById(R.id.row_button)

            holder.connectButton.setOnClickListener {
                Toast.makeText(context, "Button "+dataitem["name"]+" clicked", Toast.LENGTH_LONG).show()
            }

            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        return view
    }

    private class ViewHolder {
        lateinit var countryTextView: TextView
        lateinit var nameTextView: TextView
        lateinit var speedTextView: TextView
        lateinit var connectButton: Button
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