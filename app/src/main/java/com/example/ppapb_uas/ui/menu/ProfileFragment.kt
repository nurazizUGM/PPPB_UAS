package com.example.ppapb_uas.ui.menu

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.ppapb_uas.R
import com.example.ppapb_uas.database.PrefManager
import com.example.ppapb_uas.databinding.FragmentProfileBinding
import com.example.ppapb_uas.models.User
import com.example.ppapb_uas.network.ApiService
import com.example.ppapb_uas.network.StorageService
import com.example.ppapb_uas.ui.auth.AuthActivity
import com.example.ppapb_uas.ui.product.ProductListedActivity
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt
import java.io.File

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    private val binding by lazy { FragmentProfileBinding.inflate(layoutInflater) }
    private val prefManager by lazy { PrefManager(requireContext()) }
    private val authIntent by lazy { Intent(requireContext(), AuthActivity::class.java) }
    private var user: User? = null
    private var selectedImageUri: Uri? = null
    private var isLoading = false
    private var profilePicture: String? = null

    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

        val userId = prefManager.getUser()
        if (userId == null) {
            Toast.makeText(requireContext(), "You are not logged in", Toast.LENGTH_SHORT).show()
            requireActivity().startActivity(authIntent)
            return
        }

        with(binding) {
            btnLogout.setOnClickListener {
                prefManager.clear()
                requireActivity().startActivity(authIntent)
                Toast.makeText(requireContext(), "Logout success", Toast.LENGTH_SHORT).show()
            }
            btnListedProducts.setOnClickListener {
                startActivity(Intent(requireContext(), ProductListedActivity::class.java))
            }
            btnUpload.setOnClickListener {
                if (isLoading) {
                    Toast.makeText(requireContext(), "Please wait...", Toast.LENGTH_SHORT)
                        .show()
                    return@setOnClickListener
                }
                if (checkPermission()) {
                    selectImage()
                } else {
                    requestPermission()
                }
            }
        }

        lifecycleScope.launch {
            isLoading = true
            user = ApiService.userApi.getUserById(userId).body()
            isLoading = false
            if (user == null) {
                withContext(Dispatchers.Main) {
                    prefManager.clear()
                    context?.let { Toast.makeText(it, "User not found", Toast.LENGTH_SHORT).show() }
                    requireActivity().startActivity(authIntent)
                }
                return@launch
            }

            with(binding) {
                etFullName.setText(user!!.fullName)
                etEmail.setText(user!!.email)
                etUsername.setText(user!!.username)
                etPhone.setText(user!!.phone)
                if (user!!.profilePicture != null) {
                    Picasso.get().load(user!!.profilePicture).into(ivProfile)
                }
                btnSave.setOnClickListener {
                    updateUser()
                }
                btnToggleOld.setOnClickListener {
                    if (etOldPassword.transformationMethod == null) {
                        etOldPassword.transformationMethod = PasswordTransformationMethod()
                        btnToggleOld.setImageResource(R.drawable.mdi_eye_off)
                    } else {
                        etOldPassword.transformationMethod = null
                        btnToggleOld.setImageResource(R.drawable.mdi_eye)
                    }
                }
                btnToggleNew.setOnClickListener {
                    if (etNewPassword.transformationMethod == null) {
                        etNewPassword.transformationMethod = PasswordTransformationMethod()
                        btnToggleNew.setImageResource(R.drawable.mdi_eye_off)
                    } else {
                        etNewPassword.transformationMethod = null
                        btnToggleNew.setImageResource(R.drawable.mdi_eye)
                    }
                }
                btnToggleConfirm.setOnClickListener {
                    if (etConfirmPassword.transformationMethod == null) {
                        etConfirmPassword.transformationMethod = PasswordTransformationMethod()
                        btnToggleConfirm.setImageResource(R.drawable.mdi_eye_off)
                    } else {
                        etConfirmPassword.transformationMethod = null
                        btnToggleConfirm.setImageResource(R.drawable.mdi_eye)
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
        // return inflater.inflate(R.layout.fragment_profile, container, false)
        return binding.root
    }

    private fun validate(): Boolean {
        val fullName = binding.etFullName.text.toString()
        val phone = binding.etPhone.text.toString()
        val oldPassword = binding.etOldPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full name is required"
            return false
        }
        if (phone.isEmpty()) {
            binding.etPhone.error = "Phone is required"
            return false
        }
        if (newPassword.isNotEmpty()) {
            if (oldPassword.isEmpty()) {
                Toast.makeText(requireContext(), "Old password is required", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
            if (!BCrypt.checkpw(oldPassword, user!!.password)) {
                Toast.makeText(requireContext(), "Old password is incorrect", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
            if (newPassword != confirmPassword) {
                Toast.makeText(requireContext(), "Password does not match", Toast.LENGTH_SHORT)
                    .show()
                return false
            }
        }
        return true
    }

    private fun updateUser() {
        if (!validate()) {
            return
        }

        Toast.makeText(requireContext(), "Please wait...", Toast.LENGTH_SHORT).show()
        if (isLoading) {
            return
        }
        user!!.fullName = binding.etFullName.text.toString()
        user!!.phone = binding.etPhone.text.toString()
        if (profilePicture != null) {
            user!!.profilePicture = profilePicture
        }
        if (binding.etNewPassword.text.toString().isNotEmpty()) {
            user!!.password =
                BCrypt.hashpw(binding.etNewPassword.text.toString(), BCrypt.gensalt())
        }

        lifecycleScope.launch {
            isLoading = true
            val updatedUser = ApiService.userApi.updateUser(user!!.id!!, user!!)
            isLoading = false
            if (!updatedUser.isSuccessful) {
                Toast.makeText(requireContext(), "Network error!", Toast.LENGTH_SHORT).show()
                return@launch
            }
            Toast.makeText(requireContext(), "Profile updated", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(
                requireActivity(),
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
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), 101)
    }

    // Launch image picker
    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        imagePickerLauncher.launch(intent)
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                Toast.makeText(requireContext(), "Uploading...", Toast.LENGTH_SHORT).show()
                selectedImageUri = result.data?.data
                selectedImageUri?.let { imageUri ->
                    val path = StorageService.getRealPathFromUri(requireContext(), imageUri)
                    if (path == null) {
                        Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
                        isLoading = false
                        return@registerForActivityResult
                    }
                    lifecycleScope.launch {
                        isLoading = true
                        val image = StorageService.uploadFile(File(path), "profile")
                        isLoading = false
                        if (image == null) {
                            Toast.makeText(
                                requireContext(),
                                "Network error!",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                            isLoading = false
                            return@launch
                        }
                        binding.ivProfile.setImageURI(imageUri)
                        profilePicture = image
                    }
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
         * @return A new instance of fragment ProfileFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ProfileFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}