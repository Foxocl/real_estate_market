package by.kotlin.salesAppartment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = PropertyDatabase.getInstance(requireContext())

        adapter = PropertyListingAdapter { listing ->
            android.widget.Toast.makeText(
                requireContext(),
                "You have chosen: ${listing.propertyType}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CatalogFragment.adapter
        }

        // Observe listings from database
        lifecycleScope.launch {
            db.propertyListingDao().getAllListings().collect { listings ->
                adapter.submitList(listings)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}