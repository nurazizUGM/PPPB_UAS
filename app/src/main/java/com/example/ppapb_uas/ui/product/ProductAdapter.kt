package com.example.ppapb_uas.ui.product

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.ppapb_uas.R
import com.example.ppapb_uas.databinding.RvProductCardBinding
import com.example.ppapb_uas.models.Product
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.runBlocking

class ProductAdapter(
    private val products: List<Product>,
    private val isProductSaved: (String) -> Deferred<Boolean>,
    private val toggleBookmark: (Product) -> Deferred<Boolean>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {
    inner class ProductViewHolder(private val binding: RvProductCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            with(binding) {
                root.setOnClickListener {
                    val intent = Intent(root.context, ProductDetailActivity::class.java)
                    intent.putExtra("product", Gson().toJson(product))
                    root.context.startActivity(intent)
                }
                btnBookmark.setOnClickListener {
                    runBlocking {
                        val isSaved = toggleBookmark(product).await()
                        if (isSaved) {
                            btnBookmark.setImageResource(R.drawable.mdi_bookmark_check)
                        } else {
                            btnBookmark.setImageResource(R.drawable.mdi_bookmark_outline)
                        }
                    }
                }
                tvProductName.text = product.name
                product.price.apply {
                    val price = String.format("%,d", this).replace(',', '.')
                    tvProductPrice.text = "Rp $price,-"
                }
                Picasso.get().load(product.image).into(ivProduct)
                runBlocking {
                    val isSaved = isProductSaved(product.id).await()
                    if (isSaved) {
                        btnBookmark.setImageResource(R.drawable.mdi_bookmark_check)
                    } else {
                        btnBookmark.setImageResource(R.drawable.mdi_bookmark_outline)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = RvProductCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return products.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }
}