package co.anode.anodium

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RatingBar
import android.widget.ToggleButton
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class RatingFragment : BottomSheetDialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_rating, container, false)
        val prefs = requireContext().getSharedPreferences("co.anode.anodium", Context.MODE_PRIVATE)
        val ratingBar: RatingBar = v.findViewById(R.id.ratingBar)
        val tb1: ToggleButton = v.findViewById(R.id.toggleButton1)
        val tb2: ToggleButton = v.findViewById(R.id.toggleButton2)
        val tb3: ToggleButton = v.findViewById(R.id.toggleButton3)
        val tb4: ToggleButton = v.findViewById(R.id.toggleButton4)
        val tb5: ToggleButton = v.findViewById(R.id.toggleButton5)
        hideComments(v)
        val buttonSubmitRating = v.findViewById<Button>(R.id.buttonSubmitRating)
        buttonSubmitRating.visibility = View.GONE

        ratingBar.setOnRatingBarChangeListener { ratingBar, rating, fromUser ->
            showComments(v)
            buttonSubmitRating.visibility = View.VISIBLE
            if (rating in 0f..1f) {
                tb1.textOn = getString(R.string.rating1_1)
                tb1.textOff = getString(R.string.rating1_1)
                tb2.textOn = getString(R.string.rating1_2)
                tb2.textOff = getString(R.string.rating1_2)
                tb3.textOn = getString(R.string.rating1_3)
                tb3.textOff = getString(R.string.rating1_3)
                tb4.textOn = getString(R.string.rating1_4)
                tb4.textOff = getString(R.string.rating1_4)
                tb5.textOn = getString(R.string.rating1_5)
                tb5.textOff = getString(R.string.rating1_5)
                tb1.isChecked = false
                tb2.isChecked = false
                tb3.isChecked = false
                tb4.isChecked = false
                tb5.isChecked = false
            } else if (rating in 1f..2f) {
                tb1.textOn = getString(R.string.rating2_1)
                tb1.textOff = getString(R.string.rating2_1)
                tb2.textOn = getString(R.string.rating2_2)
                tb2.textOff = getString(R.string.rating2_2)
                tb3.textOn = getString(R.string.rating2_3)
                tb3.textOff = getString(R.string.rating2_3)
                tb4.textOn = getString(R.string.rating2_4)
                tb4.textOff = getString(R.string.rating2_4)
                tb5.textOn = getString(R.string.rating2_5)
                tb5.textOff = getString(R.string.rating2_5)
                tb1.isChecked = false
                tb2.isChecked = false
                tb3.isChecked = false
                tb4.isChecked = false
                tb5.isChecked = false
            } else if (rating in 2f..3f) {
                tb1.textOn = getString(R.string.rating3_1)
                tb1.textOff = getString(R.string.rating3_1)
                tb2.textOn = getString(R.string.rating3_2)
                tb2.textOff = getString(R.string.rating3_2)
                tb3.textOn = getString(R.string.rating3_3)
                tb3.textOff = getString(R.string.rating3_3)
                tb4.textOn = getString(R.string.rating3_4)
                tb4.textOff = getString(R.string.rating3_4)
                tb5.textOn = getString(R.string.rating3_5)
                tb5.textOff = getString(R.string.rating3_5)
                tb1.isChecked = false
                tb2.isChecked = false
                tb3.isChecked = false
                tb4.isChecked = false
                tb5.isChecked = false
            } else if (rating in 4f..5f) {
                tb1.textOn = getString(R.string.rating4_1)
                tb1.textOff = getString(R.string.rating4_1)
                tb2.textOn = getString(R.string.rating4_2)
                tb2.textOff = getString(R.string.rating4_2)
                tb3.textOn = getString(R.string.rating4_3)
                tb3.textOff = getString(R.string.rating4_3)
                tb4.textOn = getString(R.string.rating4_4)
                tb4.textOff = getString(R.string.rating4_4)
                tb5.textOn = getString(R.string.rating4_5)
                tb5.textOff = getString(R.string.rating4_5)
                tb1.isChecked = false
                tb2.isChecked = false
                tb3.isChecked = false
                tb4.isChecked = false
                tb5.isChecked = false
            }
        }

        buttonSubmitRating.setOnClickListener{
            this.context?.let { it1 -> AnodeClient.eventLog(it1, "Button RATING clicked") }
            var comment = ""
            if (tb1.isChecked) {
                comment += tb1.textOn.toString() + ","
            }
            if (tb2.isChecked) {
                comment += tb2.textOn.toString() + ","
            }
            if (tb3.isChecked) {
                comment += tb3.textOn.toString() + ","
            }
            if (tb4.isChecked) {
                comment += tb4.textOn.toString() + ","
            }
            if (tb5.isChecked) {
                comment += tb5.textOn.toString() + ","
            }
            comment = comment.dropLast(1)
            val rating = ratingBar.rating
            var ServerPublicKey = ""
            ServerPublicKey = prefs.getString("ServerPublicKey", "").toString()
            context?.let { it1 -> AnodeClient.storeRating(it1, ServerPublicKey, rating, comment) }
            dismiss()
        }

        return v
    }

    fun hideComments(v: View) {
        val tb1: ToggleButton = v.findViewById(R.id.toggleButton1)
        val tb2: ToggleButton = v.findViewById(R.id.toggleButton2)
        val tb3: ToggleButton = v.findViewById(R.id.toggleButton3)
        val tb4: ToggleButton = v.findViewById(R.id.toggleButton4)
        val tb5: ToggleButton = v.findViewById(R.id.toggleButton5)
        tb1.visibility = View.GONE
        tb2.visibility = View.GONE
        tb3.visibility = View.GONE
        tb4.visibility = View.GONE
        tb5.visibility = View.GONE
    }

    fun showComments(v: View) {
        val tb1: ToggleButton = v.findViewById(R.id.toggleButton1)
        val tb2: ToggleButton = v.findViewById(R.id.toggleButton2)
        val tb3: ToggleButton = v.findViewById(R.id.toggleButton3)
        val tb4: ToggleButton = v.findViewById(R.id.toggleButton4)
        val tb5: ToggleButton = v.findViewById(R.id.toggleButton5)
        tb1.visibility = View.VISIBLE
        tb2.visibility = View.VISIBLE
        tb3.visibility = View.VISIBLE
        tb4.visibility = View.VISIBLE
        tb5.visibility = View.VISIBLE
        tb1.isChecked = false
        tb2.isChecked = false
        tb3.isChecked = false
        tb4.isChecked = false
        tb5.isChecked = false
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
        @JvmStatic
        //fun newInstance(param1: String, param2: String) =
        fun newInstance() =
                RatingFragment().apply {
                    arguments = Bundle().apply {
                        //putString(ARG_PARAM1, param1)
                        //putString(ARG_PARAM2, param2)
                    }
                }
    }
}