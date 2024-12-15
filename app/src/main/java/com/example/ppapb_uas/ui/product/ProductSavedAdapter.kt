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

class ProductSavedAdapter(
    private var products: List<Product>,
    private val removeBookmark: (product: Product) -> Unit
) : RecyclerView.Adapter<ProductSavedAdapter.ProductViewHolder>() {
    inner class ProductViewHolder(private val binding: RvProductCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            with(binding) {
                root.setOnClickListener {
                    Intent(root.context, ProductDetailActivity::class.java).apply {
                        putExtra("product", Gson().toJson(product))
                        root.context.startActivity(this)
                    }
                }
                btnBookmark.setOnClickListener {
                    val position = products.indexOf(product)
                    removeBookmark(product)
                    products.toMutableList().apply {
                        remove(product)
                        products = this.toList()
                    }
                    notifyItemRemoved(position)
                }
                tvProductName.text = product.name
                product.price.apply {
                    val price = String.format("%,d", this).replace(',', '.')
                    tvProductPrice.text = "Rp $price,-"
                }
                Picasso.get().load(product.image).into(ivProduct)
                btnBookmark.setImageResource(R.drawable.mdi_bookmark_minus)
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