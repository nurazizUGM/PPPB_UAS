package com.example.ppapb_uas.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.PrefManager
import com.example.ppapb_uas.databinding.ActivityMainBinding
import com.example.ppapb_uas.network.ApiService
import com.example.ppapb_uas.ui.auth.AuthActivity
import kotlinx.coroutines.launch


class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigationView.setupWithNavController(navController)

        val userId = PrefManager(this).getUser()
        println(userId)
        if (userId == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            return
        }

        lifecycleScope.launch {
            try {
                val user = ApiService.userApi.getUserById(userId).body()
                if (user == null) {
                    PrefManager(this@MainActivity).clear()
                    startActivity(Intent(this@MainActivity, AuthActivity::class.java))
                    return@launch
                }
                Toast.makeText(this@MainActivity, "Welcome ${user.fullName}", Toast.LENGTH_SHORT)
                    .show()
            } catch (e: Exception) {
                println(e)
                e.printStackTrace()
                Toast.makeText(this@MainActivity, "Network error!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}