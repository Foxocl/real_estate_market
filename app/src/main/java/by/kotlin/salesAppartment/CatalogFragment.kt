package by.kotlin.salesAppartment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import by.kotlin.salesAppartment.databinding.FragmentCatalogBinding
import com.google.android.material.snackbar.Snackbar
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
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = PropertyDatabase.getInstance(requireContext())

        adapter = PropertyListingAdapter(
            onItemClick = { listing ->
                // короткое нажатие – редактирование
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
                true // возвращаем true, если обработали долгое нажатие
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@CatalogFragment.adapter
        }

        lifecycleScope.launch {
            db.propertyListingDao().getAllListings().collect { listings ->
                adapter.submitList(listings)
            }
        }
    }

    private fun openMap(listing: PropertyListing) {
        val lat = listing.latitude
        val lon = listing.longitude
        if (lat != null && lon != null) {
            val uri = "geo:$lat,$lon?z=16"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
            startActivity(intent)
        } else {
            Toast.makeText(requireContext(), "Координаты для этого объявления не определены", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}