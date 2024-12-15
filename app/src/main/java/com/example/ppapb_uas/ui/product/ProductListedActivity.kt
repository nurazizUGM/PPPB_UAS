package com.example.ppapb_uas.ui.product

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.PrefManager
import com.example.ppapb_uas.databinding.ActivityProductListedBinding
import com.example.ppapb_uas.models.Product
import com.example.ppapb_uas.models.User
import com.example.ppapb_uas.network.ApiService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ProductListedActivity : AppCompatActivity() {
    private val binding by lazy { ActivityProductListedBinding.inflate(layoutInflater) }
    private lateinit var products: List<Product>
    private lateinit var adapter: ProductListedAdapter
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        lifecycleScope.launch {
            val error = getUser().await()
            if (error != null) {
                Toast.makeText(this@ProductListedActivity, error, Toast.LENGTH_SHORT).show()
                return@launch
            }

            getProducts() {
                adapter = ProductListedAdapter(products, ::deleteProduct)
                binding.rvProduct.adapter = adapter
                binding.rvProduct.layoutManager =
                    LinearLayoutManager(this@ProductListedActivity)
                binding.progressBar.hide()
                binding.rvProduct.visibility = View.VISIBLE
            }
        }
    }

    private fun getUser(): Deferred<String?> = lifecycleScope.async {
        val userId = PrefManager(this@ProductListedActivity).getUser()
        if (userId == null) {
            finish()
            return@async "You are not logged in"
        }
        try {
            val response = ApiService.userApi.getUserById(userId)
            if (response.isSuccessful) {
                user = response.body()
                if (user == null) {
                    finish()
                    return@async "User not found"
                }
                return@async null
            } else {
                return@async "Network error"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@async "Network error"
        }
    }

    private fun getProducts(onResult: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val userProducts = user!!.products
                if (userProducts == null) {
                    return@launch
                }
                println("Fetching ${userProducts.size} products")
                products = userProducts.map { productId ->
                    async {
                        val response = ApiService.productApi.getProductById(productId)
                        response.body()
                    }
                }.awaitAll().filterNotNull()

                withContext(Dispatchers.Main) {
                    onResult()
                }
            } catch (e: Exception) {
                println(e)
                e.printStackTrace()
            }
        }
    }

    private fun deleteProduct(id: String, onSuccess: () -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiService.productApi.deleteProduct(id)
                if (response.isSuccessful) {
                    user!!.products!!.toMutableList().apply {
                        remove(id)
                        user!!.products = this
                        val res = ApiService.userApi.updateUser(user!!.id!!, user!!)
                        if (res.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                products.toMutableList().apply {
                                    removeIf { it.id == id }
                                    products = this
                                    onSuccess()
                                }
                            }
                        } else {
                            println("Failed to update user")
                        }
                    }
                } else {
                    println("Failed to delete product")
                }
            } catch (e: Exception) {
                println(e)
                e.printStackTrace()
            }
        }
    }
}