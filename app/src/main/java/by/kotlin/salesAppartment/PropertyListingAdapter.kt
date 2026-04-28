package by.kotlin.salesAppartment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class PropertyListingAdapter(
    private val onItemClick: (PropertyListing) -> Unit,
    private val onItemLongClick: (PropertyListing) -> Boolean  // ← добавлено
) : RecyclerView.Adapter<PropertyListingAdapter.ViewHolder>() {

    private var listings = listOf<PropertyListing>()

    fun submitList(list: List<PropertyListing>) {
        listings = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property_listing, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listing = listings[position]
        holder.bind(listing)

        // Короткое нажатие
        holder.itemView.setOnClickListener { onItemClick(listing) }

        // Долгое нажатие
        holder.itemView.setOnLongClickListener {
            onItemLongClick(listing)
        }
    }

    override fun getItemCount() = listings.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTransactionType: TextView = itemView.findViewById(R.id.tvTransactionType)
        private val tvPrice: TextView = itemView.findViewById(R.id.tvPrice)
        private val tvPropertyType: TextView = itemView.findViewById(R.id.tvPropertyType)
        private val tvAddress: TextView = itemView.findViewById(R.id.tvAddress)
        private val tvRooms: TextView = itemView.findViewById(R.id.tvRooms)
        private val tvFloor: TextView = itemView.findViewById(R.id.tvFloor)
        private val tvNegotiable: TextView = itemView.findViewById(R.id.tvNegotiable)

        fun bind(listing: PropertyListing) {
            tvTransactionType.text = listing.transactionType

            // Format price with spaces as thousand separators
            val priceText = if (listing.price != null) {
                val formatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.getDefault()).apply {
                    groupingSeparator = ' '
                })
                "${formatter.format(listing.price)} $"
            } else {
                "Price is not selected"
            }
            tvPrice.text = priceText

            tvPropertyType.text = listing.propertyType

            // Build address string
            val address = buildString {
                append(listing.locality)
                if (listing.street.isNotBlank()) append(", st. ${listing.street}")
                if (listing.houseNumber.isNotBlank()) append(", h. ${listing.houseNumber}")
            }
            tvAddress.text = address

            // Rooms
            tvRooms.text = if (listing.rooms != null) "${listing.rooms} rooms" else ""

            // Floor
            tvFloor.text = if (listing.floor != null) "${listing.floor} floor" else ""

            // Negotiable
            tvNegotiable.text = if (listing.negotiable) "Bargain is possible" else "Without bargain"
        }
    }
}