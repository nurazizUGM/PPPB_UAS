package com.example.ppapb_uas.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.AppDatabase
import com.example.ppapb_uas.databinding.FragmentSavedBinding
import com.example.ppapb_uas.models.Product
import com.example.ppapb_uas.ui.product.ProductSavedAdapter
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SavedFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SavedFragment : Fragment() {
    private val binding by lazy { FragmentSavedBinding.inflate(layoutInflater) }
    private lateinit var adapter: ProductSavedAdapter
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
        // return inflater.inflate(R.layout.fragment_product_saved, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        runBlocking {
            val products = getSavedProducts().await()
            binding.progressBar.visibility = View.GONE
            if (products.isEmpty()) {
                Toast.makeText(requireContext(), "No saved products", Toast.LENGTH_SHORT).show()
                with(binding) {
                    ivNetworkError.visibility = View.VISIBLE
                    rvProduct.visibility = View.GONE
                }
            } else {
                adapter = ProductSavedAdapter(products, ::removeBookmark)
                with(binding) {
                    rvProduct.adapter = adapter
                    rvProduct.layoutManager = LinearLayoutManager(context)
                    rvProduct.visibility = View.VISIBLE
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun removeBookmark(product: Product) = GlobalScope.async {
        productDao.delete(product)
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getSavedProducts(): Deferred<List<Product>> = GlobalScope.async {
        productDao.getAll()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SavedFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SavedFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}