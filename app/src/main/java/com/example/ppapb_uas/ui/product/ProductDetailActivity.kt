package com.example.ppapb_uas.ui.product

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.AppDatabase
import com.example.ppapb_uas.database.ProductDao
import com.example.ppapb_uas.databinding.ActivityProductDetailBinding
import com.example.ppapb_uas.models.Product
import com.example.ppapb_uas.models.User
import com.example.ppapb_uas.network.ApiService
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class ProductDetailActivity : AppCompatActivity() {
    private val binding by lazy { ActivityProductDetailBinding.inflate(layoutInflater) }
    private lateinit var product: Product
    private lateinit var productDao: ProductDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        productDao = AppDatabase.getDatabase(this).productDao()

        intent.getStringExtra("product")?.let {
            product = Gson().fromJson(it, Product::class.java)
            with(binding) {
                Picasso.get().load(product.image).into(ivProduct)
                txtProductName.text = product.name
                product.price.apply {
                    val price = String.format("%,d", this).replace(',', '.')
                    txtProductPrice.text = "Rp $price,-"
                }
                txtProductDesc.text = product.description
                getUser() { user ->
                    println(user)
                    txtSellerName.text = user.fullName
                    txtSellerPhone.text = user.phone ?: user.email
                    if (user.profilePicture != null) {
                        Picasso.get().load(user.profilePicture).into(ivSeller)
                    }
                }

                runBlocking {
                    isProductSaved(product.id).await().let { isSaved ->
                        btnBookmark.setImageResource(
                            if (isSaved) R.drawable.mdi_bookmark_minus
                            else R.drawable.mdi_bookmark_outline
                        )
                    }
                }

                btnBookmark.setOnClickListener {
                    runBlocking {
                        toggleBookmark(product).await().let { isSaved ->
                            btnBookmark.setImageResource(
                                if (isSaved) R.drawable.mdi_bookmark_minus
                                else R.drawable.mdi_bookmark_outline
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun isProductSaved(id: String): Deferred<Boolean> = GlobalScope.async {
        val product = productDao.getById(id)
        return@async product != null
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun toggleBookmark(product: Product): Deferred<Boolean> = GlobalScope.async {
        if (isProductSaved(product.id).await()) {
            productDao.delete(product)
            return@async false
        }
        productDao.insert(product)
        return@async true
    }

    private fun getUser(callback: (user: User) -> Unit) {
        lifecycleScope.launch {
            ApiService.userApi.getUserById(product.userId).let {
                if (it.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        callback(it.body()!!)
                    }
                }
            }
        }
    }
}