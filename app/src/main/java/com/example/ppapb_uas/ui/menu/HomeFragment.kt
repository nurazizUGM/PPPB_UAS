package com.example.ppapb_uas.ui.menu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.AppDatabase
import com.example.ppapb_uas.databinding.FragmentHomeBinding
import com.example.ppapb_uas.models.Product
import com.example.ppapb_uas.network.ApiService
import com.example.ppapb_uas.ui.product.ProductAdapter
import com.example.ppapb_uas.ui.product.ProductAddActivity
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private val binding by lazy { FragmentHomeBinding.inflate(layoutInflater) }
    private lateinit var adapter: ProductAdapter
    private val productDao by lazy { AppDatabase.getDatabase(requireContext()).productDao() }

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        // return inflater.inflate(R.layout.fragment_product_list, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            btnAddProduct.setOnClickListener {
                startActivity(Intent(requireContext(), ProductAddActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            try {
                val products = ApiService.productApi.getProducts().body()
                if (products == null) {
                    withContext(Dispatchers.Main) {
                        onNetworkError()
                        Toast.makeText(context, "Network error!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                } else if (products.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        binding.progressBar.visibility = View.GONE
                        binding.rvProduct.visibility = View.GONE
                        binding.ivEmpty.visibility = View.VISIBLE
                    }

                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "No data!", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                adapter = ProductAdapter(products, ::isProductSaved, ::toggleBookmark)
                with(binding) {
                    rvProduct.adapter = adapter
                    rvProduct.layoutManager =
                        LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    progressBar.visibility = View.GONE
                    ivEmpty.visibility = View.GONE
                    rvProduct.visibility = View.VISIBLE
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    onNetworkError()
                    Toast.makeText(context, "Network error!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onNetworkError() {
        with(binding) {
            if (progressBar.visibility.equals(View.VISIBLE)) {
                progressBar.visibility = View.GONE
                ivNetworkError.visibility = View.VISIBLE
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun isProductSaved(id: String): Deferred<Boolean> = GlobalScope.async {
        val saved = productDao.getById(id)
        return@async saved != null
    }


    @OptIn(DelicateCoroutinesApi::class)
    fun toggleBookmark(product: Product): Deferred<Boolean> = GlobalScope.async {
        val saved = isProductSaved(product.id).await()
        if (saved) {
            productDao.delete(product)
            return@async false
        }
        productDao.insert(product)
        return@async true
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ProductListFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}