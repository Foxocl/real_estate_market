package by.kotlin.salesAppartment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kotlin.salesAppartment.databinding.FragmentNewItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewItemFragment : Fragment() {

    private var _binding: FragmentNewItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: PropertyDatabase

    // List of property types for the dialog
    private val propertyTypes = listOf(
        "Flat", "Cottage", "Room", "Garage", "Commercial real estate"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentNewItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize database
        db = PropertyDatabase.getInstance(requireContext())

        // Set up property type selection click
        binding.layoutPropertyType.setOnClickListener {
            showPropertyTypeDialog()
        }

        // Set up save button click
        binding.btnSave.setOnClickListener {
            saveListing()
        }
    }

    private fun showPropertyTypeDialog() {
        val currentType = binding.tvTheme.text.toString()
        val checkedItem = propertyTypes.indexOf(currentType).takeIf { it >= 0 } ?: 0

        AlertDialog.Builder(requireContext())
            .setTitle("Select property type")
            .setSingleChoiceItems(propertyTypes.toTypedArray(), checkedItem) { dialog, which ->
                binding.tvTheme.text = propertyTypes[which]
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveListing() {
        // Validate required fields
        if (binding.tvTheme.text.isBlank()) {
            binding.tvTheme.error = "Select property type"
            return
        }

        if (binding.etLocality.text.isBlank()) {
            binding.etLocality.error = "Select locality"
            return
        }

        if (binding.etStreet.text.isBlank()) {
            binding.etStreet.error = "Select street"
            return
        }

        if (binding.etHouseNumber.text.isBlank()) {
            binding.etHouseNumber.error = "Select house number"
            return
        }

        // Get values
        val transactionType = if (binding.radioSale.isChecked) "Sale" else "Rent"
        val propertyType = binding.tvTheme.text.toString()
        val rooms = binding.etRooms.text.toString().toIntOrNull()
        val floor = binding.etFloor.text.toString().toIntOrNull()
        val locality = binding.etLocality.text.toString()
        val street = binding.etStreet.text.toString()
        val houseNumber = binding.etHouseNumber.text.toString()
        val negotiable = binding.radioNegotiableYes.isChecked
        val price = binding.etPrice.text.toString().toDoubleOrNull()

        // Create listing object
        val listing = PropertyListing(
            transactionType = transactionType,
            propertyType = propertyType,
            rooms = rooms,
            floor = floor,
            locality = locality,
            street = street,
            houseNumber = houseNumber,
            negotiable = negotiable,
            price = price
        )

        // Insert into database in background thread
        lifecycleScope.launch(Dispatchers.IO) {
            db.propertyListingDao().insert(listing)
            withContext(Dispatchers.Main) {
                // Show success message and close fragment
                android.widget.Toast.makeText(
                    requireContext(),
                    "Successfully saved",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}