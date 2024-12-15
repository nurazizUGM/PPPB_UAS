package com.example.ppapb_uas.ui.auth

import android.graphics.Rect
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.PrefManager
import com.example.ppapb_uas.databinding.FragmentLoginBinding
import com.example.ppapb_uas.network.ApiService
import com.example.ppapb_uas.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {
    private val binding by lazy { FragmentLoginBinding.inflate(layoutInflater) }
    private var isLoading = false

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        with(binding) {
            btnTogglePassword.setOnClickListener {
                if (loginPassword.transformationMethod == null) {
                    loginPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                    btnTogglePassword.setImageResource(R.drawable.mdi_eye_off)
                } else {
                    loginPassword.transformationMethod = null
                    btnTogglePassword.setImageResource(R.drawable.mdi_eye)
                }
                loginPassword.setSelection(loginPassword.text.length)
            }

            btnLogin.setOnClickListener {
                if (isLoading) {
                    Toast.makeText(requireContext(), "Please wait...", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val username = loginUsername.text.toString()
                val password = loginPassword.text.toString()
                if (username.isEmpty() || password.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Username and Password is required",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    lifecycleScope.launch {
                        isLoading = true
                        val res = authenticate(username, password)
                        if (res != null) {
                            withContext(Dispatchers.Main) {
                                context?.let { Toast.makeText(it, res, Toast.LENGTH_SHORT).show() }
                            }
                        }
                        isLoading = false
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_login, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            view.getWindowVisibleDisplayFrame(rect)

            val screenHeight = view.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            val tabLayout = requireActivity().findViewById<View>(R.id.tab_layout)

            // Adjust only when the keyboard is visible
            if (keyboardHeight > screenHeight * 0.15) {
                var focusedView = activity?.currentFocus
                if (focusedView != null) {
                    if (focusedView == binding.loginPassword) {
                        focusedView = binding.loginPassword.parent as View
                    }
                    val scrollY =
                        (focusedView.bottom - rect.bottom) + tabLayout.bottom
                    if (scrollY > 0) {
                        view.scrollBy(
                            0,
                            scrollY
                        )
                    }
                }
            } else {
                view.scrollTo(0, 0)
            }
        }
    }

    private suspend fun authenticate(username: String, password: String): String? {
        val users: List<User>? = ApiService.userApi.getUsers().body()
        if (users == null) {
            return "Network error!"
        }

        val user = users.find { it.username == username || it.email == username }
        if (user == null) {
            return "User not found!"
        }

        try {
            if (!BCrypt.checkpw(password, user.password)) {
                return "Incorrect password!"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return "Error"
        }
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Login Success", Toast.LENGTH_SHORT).show()
            context?.let { PrefManager(it).setUser(user.id!!) }
        }
        requireActivity().finish()
        return null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment LoginFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            LoginFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}