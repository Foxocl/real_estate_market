package by.kotlin.salesAppartment

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kotlin.salesAppartment.databinding.FragmentNewItemBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class NewItemFragment : Fragment() {

    private var _binding: FragmentNewItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: PropertyDatabase
    private lateinit var nominatimViewModel: NominatimViewModel
    private var listingId: Long = -1
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImagePreviewAdapter

    private val propertyTypes = listOf(
        "Flat", "Cottage", "Room", "Garage", "Commercial real estate"
    )

    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            selectedImageUris.clear()
            selectedImageUris.addAll(uris)
            imageAdapter.submitList(uris.toList())
        }

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
    ): View {
        _binding = FragmentNewItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = PropertyDatabase.getInstance(requireContext())
        nominatimViewModel = ViewModelProvider(this).get(NominatimViewModel::class.java)

        // Инициализация адаптера для миниатюр выбранных изображений
        imageAdapter = ImagePreviewAdapter { uri ->
            // Здесь может быть открытие полноэкранного просмотра
        }
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }

        binding.layoutPropertyType.setOnClickListener {
            showPropertyTypeDialog()
        }

        binding.btnSelectImages.setOnClickListener {
            pickImagesLauncher.launch("image/*")
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
                    if (listing.transactionType == "Sale") binding.radioSale.isChecked = true
                    else binding.radioRent.isChecked = true
                    binding.tvTheme.text = listing.propertyType
                    binding.etRooms.setText(listing.rooms?.toString() ?: "")
                    binding.etFloor.setText(listing.floor?.toString() ?: "")
                    binding.etCountry.setText(listing.country)
                    binding.etLocality.setText(listing.locality)
                    binding.etStreet.setText(listing.street)
                    binding.etHouseNumber.setText(listing.houseNumber)
                    binding.radioNegotiableYes.isChecked = listing.negotiable
                    binding.radioNegotiableNo.isChecked = !listing.negotiable
                    binding.etPrice.setText(listing.price?.toString() ?: "")
                    imageAdapter.submitUrls(listing.imageUrls)
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

    /**
     * Загружает выбранные изображения в ImgBB и возвращает список URL.
     */
    private suspend fun uploadSelectedImages(): List<String> {
        val urls = mutableListOf<String>()
        for (uri in selectedImageUris) {
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            // Заменяем вызов CloudStorageManager на ImgBBManager
            val url = ImgBBManager.uploadImage(file, "listing_${System.currentTimeMillis()}")
            if (url != null) urls.add(url)
        }
        return urls
    }

    private fun saveNewListing() {
        if (!validateFields()) return

        val transactionType = if (binding.radioSale.isChecked) "Sale" else "Rent"
        val propertyType = binding.tvTheme.text.toString()
        val rooms = binding.etRooms.text.toString().toIntOrNull()
        val floor = binding.etFloor.text.toString().toIntOrNull()
        val country = binding.etCountry.text.toString().trim()
        val locality = binding.etLocality.text.toString().trim()
        val street = binding.etStreet.text.toString().trim()
        val houseNumber = binding.etHouseNumber.text.toString().trim()
        val negotiable = binding.radioNegotiableYes.isChecked
        val price = binding.etPrice.text.toString().toDoubleOrNull()
        val fullAddress = "$country, $locality, $street $houseNumber"

        lifecycleScope.launch(Dispatchers.IO) {
            val coords = nominatimViewModel.fetchCoordinates(fullAddress)
            val imageUrls = uploadSelectedImages()

            val listing = PropertyListing(
                transactionType = transactionType,
                propertyType = propertyType,
                rooms = rooms,
                floor = floor,
                country = country,
                locality = locality,
                street = street,
                houseNumber = houseNumber,
                negotiable = negotiable,
                price = price,
                latitude = coords?.first,
                longitude = coords?.second,
                imageUrls = imageUrls
            )
            db.propertyListingDao().insert(listing)

            // Firestore — опционально
            try {
                FirestoreRepository().addListing(listing)
            } catch (e: Exception) {
                // Ошибка сети — данные уже в локальной базе
            }

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
        val country = binding.etCountry.text.toString().trim()
        val locality = binding.etLocality.text.toString().trim()
        val street = binding.etStreet.text.toString().trim()
        val houseNumber = binding.etHouseNumber.text.toString().trim()
        val negotiable = binding.radioNegotiableYes.isChecked
        val price = binding.etPrice.text.toString().toDoubleOrNull()

        lifecycleScope.launch(Dispatchers.IO) {
            val existing = db.propertyListingDao().getListingById(listingId)
            val newImageUrls = uploadSelectedImages()
            val finalImageUrls = if (selectedImageUris.isEmpty()) existing?.imageUrls ?: emptyList() else newImageUrls

            val updatedListing = PropertyListing(
                id = listingId,
                transactionType = transactionType,
                propertyType = propertyType,
                rooms = rooms,
                floor = floor,
                country = country,
                locality = locality,
                street = street,
                houseNumber = houseNumber,
                negotiable = negotiable,
                price = price,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                latitude = existing?.latitude,
                longitude = existing?.longitude,
                imageUrls = finalImageUrls
            )
            db.propertyListingDao().update(updatedListing)

            // Firestore — опционально
            try {
                // FirestoreRepository().updateListing(firestoreId, updatedListing)
            } catch (_: Exception) {}

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
                    existing?.let {
                        db.propertyListingDao().delete(it)
                        try {
                            // FirestoreRepository().deleteListing(firestoreId)
                        } catch (_: Exception) {}
                    }
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
        if (binding.etCountry.text.isBlank()) {
            binding.etCountry.error = "Enter country"
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