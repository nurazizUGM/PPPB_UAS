package com.example.ppapb_uas.ui.product

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.PrefManager
import com.example.ppapb_uas.databinding.ActivityProductAddBinding
import com.example.ppapb_uas.models.Product
import com.example.ppapb_uas.models.User
import com.example.ppapb_uas.network.ApiService
import com.example.ppapb_uas.network.StorageService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File

class ProductAddActivity : AppCompatActivity() {
    private val binding by lazy { ActivityProductAddBinding.inflate(layoutInflater) }
    private val prefManager by lazy { PrefManager(this) }
    private var selectedImageUri: Uri? = null
    private var productImage: String? = null
    private var user: User? = null
    private var isLoading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        prefManager.getUser().let {
            if (it == null) {
                Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            lifecycleScope.launch {
                try {
                    ApiService.userApi.getUserById(it).apply {
                        if (isSuccessful) {
                            user = body()
                            binding.btnAddProduct.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(
                                this@ProductAddActivity,
                                "Network error!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            println(body())
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@ProductAddActivity, "Network error!", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }
        }

        with(binding) {
            btnAddProduct.visibility = View.GONE
            btnUpload.setOnClickListener {
                if (isLoading) {
                    Toast.makeText(this@ProductAddActivity, "Please wait...", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                if (checkPermission()) {
                    selectImage()
                } else {
                    requestPermission()
                }
            }

            btnAddProduct.setOnClickListener {
                if (isLoading) {
                    Toast.makeText(this@ProductAddActivity, "Please wait...", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                isLoading = true
                val name = etProductName.text.toString()
                val price = etProductPrice.text.toString()
                val description = etProductDescription.text.toString()

                if (name.isEmpty() || price.isEmpty() || description.isEmpty()) {
                    Toast.makeText(
                        this@ProductAddActivity,
                        "Please fill all fields",
                        Toast.LENGTH_SHORT
                    ).show()
                    isLoading = false
                    return@setOnClickListener
                } else if (productImage == null) {
                    Toast.makeText(
                        this@ProductAddActivity,
                        "Please upload product image",
                        Toast.LENGTH_SHORT
                    ).show()
                    isLoading = false
                    return@setOnClickListener
                }

                val product = Product(name, price.toInt(), description, productImage!!, user!!.id!!)
                addProduct(product)
            }
        }
    }

    private fun addProduct(product: Product) {
        Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            try {
                val res = ApiService.productApi.createProduct(product)
                if (res.isSuccessful) {
                    Toast.makeText(
                        this@ProductAddActivity,
                        "Add product success",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    updateUserProduct(product.id).await()
                    finish()
                } else {
                    Toast.makeText(
                        this@ProductAddActivity,
                        "Network error!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProductAddActivity, "Network error!", Toast.LENGTH_SHORT)
                    .show()
            }
            isLoading = false
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun updateUserProduct(id: String) = GlobalScope.async {
        if (user == null) {
            return@async
        }
        val products = user!!.products?.toMutableList() ?: mutableListOf()
        products.add(id)
        user!!.products = products.toList()
        println(user)
        ApiService.userApi.updateUser(user!!.id!!, user!!)
        return@async
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Request permission
    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        ActivityCompat.requestPermissions(this, arrayOf(permission), 101)
    }

    // Launch image picker
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    // Handle image picker result
    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show()
                selectedImageUri = result.data?.data
                selectedImageUri?.let { imageUri ->
                    val path = StorageService.getRealPathFromUri(this, imageUri)
                    if (path == null) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@registerForActivityResult
                    }
                    lifecycleScope.launch {
                        isLoading = true
                        val image = StorageService.uploadFile(File(path), "products")
                        isLoading = false
                        if (image == null) {
                            Toast.makeText(
                                this@ProductAddActivity,
                                "Network error!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            isLoading = false
                            return@launch
                        }
                        binding.ivProduct.setImageURI(imageUri)
                        productImage = image
                    }
                }
            }
        }
}