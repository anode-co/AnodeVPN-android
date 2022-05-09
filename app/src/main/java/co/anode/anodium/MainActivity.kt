package co.anode.anodium

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import co.anode.anodium.databinding.ActivityMainBinding
import co.anode.anodium.support.AnodeClient
import co.anode.anodium.support.AnodeUtil
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {
    private val LOGTAG = "co.anode.anodium"
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        initializeApp()
        val navController = navHostFragment.navController
        navView.setupWithNavController(navController)
    }

    private fun initializeApp() {
        AnodeClient.init(applicationContext, this)
        AnodeUtil.init(applicationContext)
        AnodeUtil.initializeApp()
        AnodeUtil.launchPld()
    }
}