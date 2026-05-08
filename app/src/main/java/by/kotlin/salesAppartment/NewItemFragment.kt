package by.kotlin.salesAppartment

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kotlin.salesAppartment.databinding.FragmentNewItemBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class NewItemFragment : Fragment() {

    private var _binding: FragmentNewItemBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: PropertyDatabase
    private lateinit var nominatimViewModel: NominatimViewModel
    private var listingId: Long = -1
    private val selectedImageUris = mutableListOf<Uri>()
    private lateinit var imageAdapter: ImagePreviewAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val propertyTypes = listOf(
        "Flat", "Cottage", "Room", "Garage", "Commercial real estate"
    )

    private val pickImagesLauncher =
        registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
            selectedImageUris.addAll(uris)
            imageAdapter.submitList(selectedImageUris.toList())
        }

    private lateinit var takePhotoLauncher: ActivityResultLauncher<Uri>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var locationPermissionLauncher: ActivityResultLauncher<String>

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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        cameraPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) launchCamera()
            else showCameraPermissionDeniedDialog()
        }

        locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) getCurrentLocation()
            else showLocationPermissionDeniedDialog()
        }

        imageAdapter = ImagePreviewAdapter { uri -> }
        binding.rvImages.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = imageAdapter
        }

        binding.btnSelectImages.setOnClickListener {
            pickImagesLauncher.launch("image/*")
        }

        takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success) imageAdapter.submitList(selectedImageUris.toList())
        }
        binding.btnTakePhoto.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
            ) launchCamera()
            else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        binding.btnMyLocation.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) getCurrentLocation()
            else locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.layoutPropertyType.setOnClickListener { showPropertyTypeDialog() }

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

    private fun launchCamera() {
        val photoUri = createImageUri()
        if (photoUri != null) {
            selectedImageUris.add(photoUri)
            takePhotoLauncher.launch(photoUri)
        }
    }

    private fun createImageUri(): Uri? {
        val imageFile = File(requireContext().cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            imageFile
        )
    }

    private fun getCurrentLocation() {
        try {
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val lat = it.latitude
                    val lon = it.longitude
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lon, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        binding.etCountry.setText(addr.countryName ?: "")
                        binding.etLocality.setText(addr.locality ?: addr.adminArea ?: "")
                        binding.etStreet.setText(addr.thoroughfare ?: "")
                        binding.etHouseNumber.setText(addr.subThoroughfare ?: "")
                    }
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Location permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showCameraPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Camera Permission Required")
            .setMessage("This app needs camera access to take photos. Please grant camera permission in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLocationPermissionDeniedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Location Permission Required")
            .setMessage("This app needs location access to fill your address automatically. Please grant location permission in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", requireContext().packageName, null)
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
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

    private suspend fun uploadSelectedImages(): List<String> {
        val urls = mutableListOf<String>()
        for (uri in selectedImageUris) {
            val file = File(requireContext().cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val fullAddress = "$country, $locality, $street $houseNumber"

        lifecycleScope.launch(Dispatchers.IO) {
            val imageUrls = uploadSelectedImages()
            val coords = nominatimViewModel.fetchCoordinates(fullAddress)

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
                imageUrls = imageUrls,
                userId = currentUserId
            )

            val firestoreId = try {
                FirestoreRepository().addListing(listing)
            } catch (e: Exception) {
                null
            }

            val listingWithId = listing.copy(firestoreId = firestoreId)
            db.propertyListingDao().upsert(listingWithId)

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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        lifecycleScope.launch(Dispatchers.IO) {
            val existing = db.propertyListingDao().getListingById(listingId) ?: return@launch
            val newImageUrls = if (selectedImageUris.isEmpty()) existing.imageUrls else uploadSelectedImages()

            val updatedListing = existing.copy(
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
                imageUrls = newImageUrls,
                userId = currentUserId
            )

            db.propertyListingDao().update(updatedListing)

            existing.firestoreId?.let { fid ->
                try {
                    FirestoreRepository().updateListing(fid, updatedListing)
                } catch (_: Exception) {}
            }

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
                        it.firestoreId?.let { fid ->
                            try { FirestoreRepository().deleteListing(fid) } catch (_: Exception) {}
                        }
                        db.propertyListingDao().delete(it)
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
        if (binding.tvTheme.text.isBlank()) { binding.tvTheme.error = "Select property type"; return false }
        if (binding.etCountry.text.isBlank()) { binding.etCountry.error = "Enter country"; return false }
        if (binding.etLocality.text.isBlank()) { binding.etLocality.error = "Enter locality"; return false }
        if (binding.etStreet.text.isBlank()) { binding.etStreet.error = "Enter street"; return false }
        if (binding.etHouseNumber.text.isBlank()) { binding.etHouseNumber.error = "Enter house number"; return false }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}