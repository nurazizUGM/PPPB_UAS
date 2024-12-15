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
import com.example.ppapb_uas.databinding.FragmentRegisterBinding
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
 * Use the [RegisterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RegisterFragment : Fragment() {
    private val binding by lazy { FragmentRegisterBinding.inflate(layoutInflater) }
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
                if (registerPassword.transformationMethod == null) {
                    registerPassword.transformationMethod =
                        PasswordTransformationMethod.getInstance()
                    btnTogglePassword.setImageResource(R.drawable.mdi_eye_off)
                } else {
                    registerPassword.transformationMethod = null
                    btnTogglePassword.setImageResource(R.drawable.mdi_eye)
                }
                registerPassword.setSelection(registerPassword.text.length)
            }
            btnTogglePasswordConfirmation.setOnClickListener {
                if (registerPasswordConfirmation.transformationMethod == null) {
                    registerPasswordConfirmation.transformationMethod =
                        PasswordTransformationMethod.getInstance()
                    btnTogglePasswordConfirmation.setImageResource(R.drawable.mdi_eye_off)
                } else {
                    registerPasswordConfirmation.transformationMethod = null
                    btnTogglePasswordConfirmation.setImageResource(R.drawable.mdi_eye)
                }
                registerPasswordConfirmation.setSelection(registerPasswordConfirmation.text.length)
            }
            btnRegister.setOnClickListener {
                if (isLoading) {
                    Toast.makeText(requireContext(), "Please wait...", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val fullname = registerFullname.text.toString()
                val username = registerUsername.text.toString()
                val email = registerEmail.text.toString()
                val password = registerPassword.text.toString()
                val passwordConfirmation = registerPasswordConfirmation.text.toString()
                if (fullname.isEmpty()) {
                    Toast.makeText(requireContext(), "Full Name is required", Toast.LENGTH_SHORT)
                        .show()
                } else if (username.isEmpty()) {
                    Toast.makeText(requireContext(), "Username is required", Toast.LENGTH_SHORT)
                        .show()
                } else if (email.isEmpty()) {
                    Toast.makeText(requireContext(), "Email is required", Toast.LENGTH_SHORT).show()
                } else if (password.isEmpty()) {
                    Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT)
                        .show()
                } else if (passwordConfirmation.isEmpty()) {
                    Toast.makeText(
                        requireContext(), "Password Confirmation is required", Toast.LENGTH_SHORT
                    ).show()
                } else if (password != passwordConfirmation) {
                    Toast.makeText(
                        requireContext(), "Password confirmation doesn't match", Toast.LENGTH_SHORT
                    ).show()
                } else {
                    isLoading = true
                    val user = User(
                        fullname, username, email, BCrypt.hashpw(password, BCrypt.gensalt())
                    )
                    lifecycleScope.launch {
                        val res = register(user)
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

    private suspend fun register(user: User): String? {
        val users = ApiService.userApi.getUsers().body()
        if (users == null) {
            return "Network Error"
        }
        users.find { it.username == user.username }.also {
            if (it != null) {
                return@register "Username already exists"
            }
        }
        users.find { it.email == user.email }.also {
            if (it != null) {
                return@register "Email already exists"
            }
        }

        val response = ApiService.userApi.createUser(user)
        if (!response.isSuccessful) {
            println(response.errorBody()?.string())
            return "Register Failed"
        }
        withContext(Dispatchers.Main) {
            context?.let { PrefManager(it).setUser(user.id!!) }
            Toast.makeText(context, "Register Success", Toast.LENGTH_LONG).show()
            requireActivity().finish()
        }
        return null
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.fragment_register, container, false)
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
                    if (focusedView == binding.registerPassword) {
                        focusedView = binding.registerPassword.parent as View
                    } else if (focusedView == binding.registerPasswordConfirmation) {
                        focusedView = binding.registerPasswordConfirmation.parent as View
                    }
                    val scrollY =
                        (focusedView.bottom - rect.bottom) + tabLayout.bottom + binding.registerLayout.top
                    if (scrollY > 0) {
                        view.scrollBy(
                            0, scrollY
                        )
                    }
                }
            } else {
                view.scrollTo(0, 0)
            }
        }
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment RegisterFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) = RegisterFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PARAM1, param1)
                putString(ARG_PARAM2, param2)
            }
        }
    }
}