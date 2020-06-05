package co.anode.anodevpn

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class AccountNicknameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accountnickname)
        val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)

        val continueButton: Button = findViewById(R.id.button_continue)
        continueButton.setOnClickListener() {
            val nicknameText: EditText = findViewById(R.id.editTextNickname)
            val nickname = nicknameText.text.toString()
            if (nickname.isNullOrEmpty()) {
                Toast.makeText(this,"Nickname can not be empty", Toast.LENGTH_SHORT).show()
            } else {
                //Save nickname
                with (prefs.edit()) {
                    putString("nickname",nickname)
                    commit()
                }
                //TODO: Start new activity
            }
        }
    }

    override fun onCreateView(name: String, context: Context, attrs: AttributeSet): View? {
        val prefs = getSharedPreferences("co.anode.AnodeVPN", Context.MODE_PRIVATE)
        val nickname = prefs!!.getString("nickname","nickname")
        val nicknameText: EditText = findViewById(R.id.editTextNickname)
        nicknameText.setText(nickname)

        return super.onCreateView(name, context, attrs)
    }
}