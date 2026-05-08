package by.kotlin.salesAppartment

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class PropertyListingAdapter(
    private val onItemClick: (PropertyListing) -> Unit,
    private val onItemLongClick: (PropertyListing) -> Boolean
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

        holder.itemView.setOnClickListener { onItemClick(listing) }
        holder.itemView.setOnLongClickListener { onItemLongClick(listing) }
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
        private val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        private val btnShare: ImageButton = itemView.findViewById(R.id.btnShare)

        fun bind(listing: PropertyListing) {
            tvTransactionType.text = listing.transactionType
            val priceText = if (listing.price != null) {
                val formatter = DecimalFormat("#,##0", DecimalFormatSymbols(Locale.getDefault()).apply {
                    groupingSeparator = ' '
                })
                "${formatter.format(listing.price)} $"
            } else "Price is not selected"
            tvPrice.text = priceText
            tvPropertyType.text = listing.propertyType
            val address = buildString {
                append(listing.locality)
                if (listing.street.isNotBlank()) append(", st. ${listing.street}")
                if (listing.houseNumber.isNotBlank()) append(", h. ${listing.houseNumber}")
            }
            tvAddress.text = address
            tvRooms.text = if (listing.rooms != null) "${listing.rooms} rooms" else ""
            tvFloor.text = if (listing.floor != null) "${listing.floor} floor" else ""
            tvNegotiable.text = if (listing.negotiable) "Bargain is possible" else "Without bargain"

            if (listing.imageUrls.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(listing.imageUrls.first())
                    .centerCrop()
                    .into(ivThumbnail)
            } else {
                ivThumbnail.setImageResource(0)
            }

            btnShare.setOnClickListener {
                val shareText = "${listing.transactionType}: ${listing.propertyType} - ${priceText}, ${address}"
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    if (listing.imageUrls.isNotEmpty()) {
                        type = "image/*"
                        putExtra(Intent.EXTRA_STREAM, Uri.parse(listing.imageUrls.first()))
                    }
                }
                itemView.context.startActivity(Intent.createChooser(shareIntent, "Share via"))
            }
        }
    }
}