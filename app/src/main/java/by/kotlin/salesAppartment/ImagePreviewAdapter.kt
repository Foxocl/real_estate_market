package by.kotlin.salesAppartment

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImagePreviewAdapter(
    private val onItemClick: (Any) -> Unit
) : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

    private var uris = listOf<Uri>()
    private var urls = listOf<String>()

    fun submitList(uris: List<Uri>) {
        this.uris = uris
        this.urls = emptyList()
        notifyDataSetChanged()
    }

    fun submitUrls(urls: List<String>) {
        this.urls = urls
        this.uris = emptyList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_preview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when {
            uris.isNotEmpty() -> {
                val uri = uris[position]
                Glide.with(holder.itemView.context)
                    .load(uri)
                    .centerCrop()
                    .into(holder.imageView)
                holder.itemView.setOnClickListener { onItemClick(uri) }
            }
            urls.isNotEmpty() -> {
                val url = urls[position]
                Glide.with(holder.itemView.context)
                    .load(url)
                    .centerCrop()
                    .into(holder.imageView)
                holder.itemView.setOnClickListener { onItemClick(url) }
            }
        }
    }

    override fun getItemCount() = if (uris.isNotEmpty()) uris.size else urls.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.ivPreview)
    }
}