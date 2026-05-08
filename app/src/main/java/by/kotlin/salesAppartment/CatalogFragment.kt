package by.kotlin.salesAppartment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kotlin.salesAppartment.databinding.FragmentCatalogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: PropertyDatabase
    private lateinit var adapter: PropertyListingAdapter
    private var allListings = listOf<PropertyListing>()
    private val firestoreRepo = FirestoreRepository()
    private var filterMy = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = PropertyDatabase.getInstance(requireContext())
        filterMy = arguments?.getBoolean("filterMy", false) ?: false
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

        adapter = PropertyListingAdapter(
            onItemClick = { listing ->
                val fragment = NewItemFragment().apply {
                    arguments = Bundle().apply { putLong("listingId", listing.id) }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onItemLongClick = { listing ->
                openMap(listing)
                true
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CatalogFragment.adapter
        }

        // Первичная загрузка из Firestore в Room (upsert)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val remoteListings = firestoreRepo.getAllListings()
                for (listing in remoteListings) {
                    db.propertyListingDao().upsert(listing)
                }
            } catch (e: Exception) { }
        }

        // Подписка на изменения Firestore → upsert в Room
        Firebase.firestore.collection("listings")
            .addSnapshotListener { snapshot, e ->
                if (e != null) return@addSnapshotListener
                if (snapshot != null) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val listing = doc.toObject(PropertyListing::class.java)?.copy(firestoreId = doc.id)
                            listing?.let { db.propertyListingDao().upsert(it) }
                        }
                    }
                }
            }

        // Подписка на локальную базу с фильтром
        lifecycleScope.launch {
            if (filterMy && currentUserId != null) {
                db.propertyListingDao().getListingsByUser(currentUserId).collect { listings ->
                    allListings = listings
                    applyFilters()
                }
            } else {
                db.propertyListingDao().getAllListings().collect { listings ->
                    allListings = listings
                    applyFilters()
                }
            }
        }

        setupSearchAndFilter()
    }

    private fun setupSearchAndFilter() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                applyFilters()
                return true
            }
        })

        binding.spinnerFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun applyFilters() {
        if (_binding == null) return
        val query = binding.searchView.query?.toString() ?: ""
        val filter = binding.spinnerFilter.selectedItem.toString()
        val sort = binding.spinnerSort.selectedItemPosition

        var result = allListings
        when (filter) {
            "Sale" -> result = result.filter { it.transactionType == "Sale" }
            "Rent" -> result = result.filter { it.transactionType == "Rent" }
        }
        if (query.isNotBlank()) {
            result = result.filter { SearchUtils.fuzzyMatch(it, query) }
        }
        result = when (sort) {
            1 -> result.sortedBy { it.createdAt }
            2 -> result.sortedBy { it.price ?: Double.MAX_VALUE }
            3 -> result.sortedByDescending { it.price ?: 0.0 }
            else -> result.sortedByDescending { it.createdAt }
        }
        adapter.submitList(result)
    }

    private fun openMap(listing: PropertyListing) {
        val lat = listing.latitude
        val lon = listing.longitude
        if (lat != null && lon != null) {
            val uri = "geo:$lat,$lon?z=16"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Coordinates not defined", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}