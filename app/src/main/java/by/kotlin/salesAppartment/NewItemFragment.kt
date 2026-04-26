package by.kotlin.salesAppartment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import by.kotlin.salesAppartment.databinding.FragmentNewItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewItemFragment : Fragment() {

    private var _binding: FragmentNewItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: PropertyDatabase
    private var listingId: Long = -1

    private val propertyTypes = listOf(
        "Flat", "Cottage", "Room", "Garage", "Commercial real estate"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            listingId = it.getLong("listingId", -1)
        }
    }

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
        db = PropertyDatabase.getInstance(requireContext())

        binding.layoutPropertyType.setOnClickListener {
            showPropertyTypeDialog()
        }

        if (listingId != -1L) {
            loadListingForEdit()
            binding.btnSave.text = "Update"
            binding.btnDelete.visibility = View.VISIBLE
            binding.btnDelete.setOnClickListener { deleteListing() }
        } else {
            binding.btnDelete.visibility = View.GONE
        }

        binding.btnSave.setOnClickListener {
            if (listingId == -1L) saveNewListing() else updateListing()
        }
    }

    private fun loadListingForEdit() {
        lifecycleScope.launch {
            val listing = db.propertyListingDao().getListingById(listingId)
            if (listing != null) {
                withContext(Dispatchers.Main) {
                    if (listing.transactionType == "Sale") {
                        binding.radioSale.isChecked = true
                    } else {
                        binding.radioRent.isChecked = true
                    }
                    binding.tvTheme.text = listing.propertyType
                    if (listing.rooms != null) binding.etRooms.setText(listing.rooms.toString())
                    if (listing.floor != null) binding.etFloor.setText(listing.floor.toString())
                    binding.etLocality.setText(listing.locality)
                    binding.etStreet.setText(listing.street)
                    binding.etHouseNumber.setText(listing.houseNumber)
                    if (listing.negotiable) binding.radioNegotiableYes.isChecked
                    else binding.radioNegotiableNo.isChecked
                    if (listing.price != null) binding.etPrice.setText(listing.price.toString())
                }
            }
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

    private fun saveNewListing() {
        if (!validateFields()) return

        val transactionType = if (binding.radioSale.isChecked) "Sale" else "Rent"
        val propertyType = binding.tvTheme.text.toString()
        val rooms = binding.etRooms.text.toString().toIntOrNull()
        val floor = binding.etFloor.text.toString().toIntOrNull()
        val locality = binding.etLocality.text.toString()
        val street = binding.etStreet.text.toString()
        val houseNumber = binding.etHouseNumber.text.toString()
        val negotiable = binding.radioNegotiableYes.isChecked
        val price = binding.etPrice.text.toString().toDoubleOrNull()

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

        lifecycleScope.launch(Dispatchers.IO) {
            db.propertyListingDao().insert(listing)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Saved", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun updateListing() {
        if (!validateFields()) return

        val transactionType = if (binding.radioSale.isChecked) "Sale" else "Rent"
        val propertyType = binding.tvTheme.text.toString()
        val rooms = binding.etRooms.text.toString().toIntOrNull()
        val floor = binding.etFloor.text.toString().toIntOrNull()
        val locality = binding.etLocality.text.toString()
        val street = binding.etStreet.text.toString()
        val houseNumber = binding.etHouseNumber.text.toString()
        val negotiable = binding.radioNegotiableYes.isChecked
        val price = binding.etPrice.text.toString().toDoubleOrNull()

        val updatedListing = PropertyListing(
            id = listingId,
            transactionType = transactionType,
            propertyType = propertyType,
            rooms = rooms,
            floor = floor,
            locality = locality,
            street = street,
            houseNumber = houseNumber,
            negotiable = negotiable,
            price = price,
            createdAt = System.currentTimeMillis()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            db.propertyListingDao().update(updatedListing)
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun deleteListing() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete listing")
            .setMessage("Are you sure?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    val existing = db.propertyListingDao().getListingById(listingId)
                    existing?.let { db.propertyListingDao().delete(it) }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show()
                        parentFragmentManager.popBackStack()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateFields(): Boolean {
        if (binding.tvTheme.text.isBlank()) {
            binding.tvTheme.error = "Select property type"
            return false
        }
        if (binding.etLocality.text.isBlank()) {
            binding.etLocality.error = "Enter locality"
            return false
        }
        if (binding.etStreet.text.isBlank()) {
            binding.etStreet.error = "Enter street"
            return false
        }
        if (binding.etHouseNumber.text.isBlank()) {
            binding.etHouseNumber.error = "Enter house number"
            return false
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}