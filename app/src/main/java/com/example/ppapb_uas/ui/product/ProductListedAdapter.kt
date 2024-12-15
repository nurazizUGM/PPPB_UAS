package com.example.ppapb_uas.ui.product

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.ppapb_uas.databinding.RvProductListedBinding
import com.example.ppapb_uas.models.Product
import com.squareup.picasso.Picasso

class ProductListedAdapter(
    var products: List<Product>,
    val deleteProduct: (String, () -> Unit) -> Unit
) :
    RecyclerView.Adapter<ProductListedAdapter.ProductViewHolder>() {
    private var isLoading = false

    inner class ProductViewHolder(val binding: RvProductListedBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            with(binding) {
                tvProductName.text = product.name
                product.price.apply {
                    val price = String.format("%,d", this).replace(',', '.')
                    tvProductPrice.text = "Rp $price,-"
                }
                Picasso.get().load(product.image).into(ivProduct)

                btnDelete.setOnClickListener {
                    Toast.makeText(root.context, "Please wait...", Toast.LENGTH_SHORT).show()
                    if (isLoading) {
                        return@setOnClickListener
                    }

                    isLoading = true
                    deleteProduct(product.id) {
                        notifyItemRemoved(products.indexOf(product))
                        products.toMutableList().apply {
                            remove(product)
                            products = this
                        }
                        isLoading = false
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding =
            RvProductListedBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return products.size
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }
}