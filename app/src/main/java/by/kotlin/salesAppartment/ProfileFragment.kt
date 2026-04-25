package by.kotlin.salesAppartment

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast

class ProfileFragment : Fragment() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        // Находим элементы
        tvName = view.findViewById(R.id.tvName)
        tvEmail = view.findViewById(R.id.tvEmail)
        tvPhone = view.findViewById(R.id.tvPhone)

        val btnEditName = view.findViewById<ImageButton>(R.id.btnEditName)
        val btnEditEmail = view.findViewById<ImageButton>(R.id.btnEditEmail)
        val btnEditPhone = view.findViewById<ImageButton>(R.id.btnEditPhone)

        // Загружаем сохранённые значения
        loadSavedData()

        // Обработчики нажатий
        btnEditName.setOnClickListener { showEditDialog("Name", tvName) }
        btnEditEmail.setOnClickListener { showEditDialog("Email", tvEmail) }
        btnEditPhone.setOnClickListener { showEditDialog("Phone", tvPhone) }

        return view
    }

    private fun showEditDialog(fieldName: String, textView: TextView) {
        val currentValue = textView.text.toString()

        val editText = EditText(requireContext())
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
                            Toast.makeText(
                                requireContext(),
                                "Invalid phone number: must contain at least one digit",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    else -> {
                        if (newValue.isNotEmpty()) {
                            textView.text = newValue
                            saveToPreferences(fieldName, newValue)
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Value cannot be empty",
                                Toast.LENGTH_SHORT
                            ).show()
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
        val prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString(key, value).apply()
    }

    private fun loadSavedData() {
        val prefs = requireContext().getSharedPreferences("profile_prefs", Context.MODE_PRIVATE)

        // Загружаем, если есть или оставляем плейсхолдеры XML
        prefs.getString("Name", null)?.let { tvName.text = it }
        prefs.getString("Email", null)?.let { tvEmail.text = it }
        prefs.getString("Phone", null)?.let { tvPhone.text = it }
    }
}