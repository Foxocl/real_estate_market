// /app/src/main/java/by/kotlin/salesAppartment/CatalogFragment.kt
package by.kotlin.salesAppartment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kotlin.salesAppartment.databinding.FragmentCatalogBinding
import kotlinx.coroutines.launch

class CatalogFragment : Fragment() {

    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: PropertyDatabase
    private lateinit var adapter: PropertyListingAdapter
    private var allListings = listOf<PropertyListing>()

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

        lifecycleScope.launch {
            db.propertyListingDao().getAllListings().collect { listings ->
                if (_binding != null) {
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