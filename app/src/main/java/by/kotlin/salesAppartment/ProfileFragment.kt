package by.kotlin.salesAppartment

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException

class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var auth: FirebaseAuth

    private lateinit var btnMyProducts: View
    private lateinit var btnLogout: View
    private lateinit var btnDeleteAccount: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        auth = FirebaseAuth.getInstance()

        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)

        val btnEditName = view.findViewById<ImageButton>(R.id.btnEditName)
        val btnEditEmail = view.findViewById<ImageButton>(R.id.btnEditEmail)
        val btnEditPhone = view.findViewById<ImageButton>(R.id.btnEditPhone)

        btnMyProducts = view.findViewById(R.id.btnMyProducts)
        btnLogout = view.findViewById(R.id.btnLogout)
        btnDeleteAccount = view.findViewById(R.id.btnDeleteAccount)

        loadSavedData()
        updateUserInfo()

        btnEditName.setOnClickListener { showEditDialog("Name", tvName) }
        btnEditEmail.setOnClickListener { showEditDialog("Email", tvEmail) }
        btnEditPhone.setOnClickListener { showEditDialog("Phone", tvPhone) }

        btnMyProducts.setOnClickListener {
            val fragment = CatalogFragment().apply {
                arguments = Bundle().apply { putBoolean("filterMy", true) }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(requireContext(), RegistrationActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            requireActivity().finish()
        }

        btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }

        return view
    }

    private fun updateUserInfo() {
        val user = auth.currentUser
        if (user != null) {
            tvEmail.text = user.email ?: "No email"
        }
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete account")
            .setMessage("Are you sure you want to delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                val user = auth.currentUser
                user?.delete()
                    ?.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                            auth.signOut()
                            startActivity(Intent(requireContext(), RegistrationActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            })
                            requireActivity().finish()
                        } else {
                            if (task.exception is FirebaseAuthRecentLoginRequiredException) {
                                Toast.makeText(
                                    requireContext(),
                                    "Please log out and log in again before deleting your account.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to delete account: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(fieldName: String, textView: TextView) {
        val currentValue = textView.text.toString()
        val editText = android.widget.EditText(requireContext())
        editText.setText(currentValue)
        editText.hint = "Enter new $fieldName"

        AlertDialog.Builder(requireContext())
            .setTitle("Edit $fieldName")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                val newValue = editText.text.toString().trim()
                when (fieldName) {
                    "Phone" -> {
                        val formatted = formatPhoneNumber(newValue)
                        if (formatted != null) {
                            textView.text = formatted
                            saveToPreferences(fieldName, formatted)
                        } else {
                            Toast.makeText(requireContext(), "Invalid phone number", Toast.LENGTH_LONG).show()
                        }
                    }
                    else -> {
                        if (newValue.isNotEmpty()) {
                            textView.text = newValue
                            saveToPreferences(fieldName, newValue)
                        } else {
                            Toast.makeText(requireContext(), "Value cannot be empty", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatPhoneNumber(input: String): String? {
        val digits = input.filter { it.isDigit() }
        if (digits.isEmpty()) return null
        val trimmed = if (digits.length > 15) digits.substring(0, 15) else digits
        return "+$trimmed"
    }

    private fun saveToPreferences(key: String, value: String) {
        requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
            .edit().putString(key, value).apply()
    }

    private fun loadSavedData() {
        val prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
        prefs.getString("Name", null)?.let { tvName.text = it }
        prefs.getString("Phone", null)?.let { tvPhone.text = it }
    }
}