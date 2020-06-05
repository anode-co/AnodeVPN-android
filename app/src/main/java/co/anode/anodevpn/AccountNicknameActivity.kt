package co.anode.anodevpn

import android.content.Context
import android.content.Intent
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
        val prefsnickname = prefs!!.getString("nickname","nickname")
        val nicknameText: EditText = findViewById(R.id.editTextNickname)
        nicknameText.setText(prefsnickname)

        val continueButton: Button = findViewById(R.id.button_continue)
        continueButton.setOnClickListener() {
            val nickname = nicknameText.text.toString()
            if (nickname.isNullOrEmpty()) {
                Toast.makeText(this,"Nickname can not be empty", Toast.LENGTH_SHORT).show()
            } else {
                //Save nickname
                with (prefs.edit()) {
                    putString("nickname",nickname)
                    commit()
                }
                //Start activity
                val accountMainActivity = Intent(applicationContext, AccountMainActivity::class.java)
                startActivity(accountMainActivity)
            }
        }
    }
}