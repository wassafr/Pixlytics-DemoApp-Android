package io.wassa.pixlyticssdksampleapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import io.wassa.pixlytics_sdk.online.model.ModelItem
import kotlinx.android.synthetic.main.model_item_layout.view.*

/**
 * Adapter use to display ModelItem in recyclerView.
 */
class ModelItemAdapter : RecyclerView.Adapter<ModelItemAdapter.ViewHolder>() {

    private val data: MutableList<ModelItem> = mutableListOf()
    val itemsSelected = mutableListOf<ModelItem>()

    /**
     * This method add items to the list of data
     */
    fun addItems(items: List<ModelItem>) {
        data.addAll(items)
        notifyDataSetChanged()
    }

    /**
     * This method reset the adapter in it's initial state
     */
    fun reset() {
        data.clear()
        itemsSelected.clear()
        notifyDataSetChanged()
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ModelItemAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.model_item_layout, parent, false)
        return ViewHolder(view)
    }

    /**
     * Setup the view for modelItem.
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position >= data.size)
            return
        data[position].images?.forEach {
            // For each image find the default one to display it
            if (it.is_default)
                Picasso.get().load(it.image_url).into(holder.view.imageView)
        }
        // Setup text of the item
        holder.view.itemName.text = data[position].name
        holder.view.authorName.text = "${data[position].author?.first_name}  ${data[position].author?.last_name}"
        // Onclick select item
        holder.view.setOnClickListener {
            if (itemsSelected.contains(data[position]))
                itemsSelected.remove(data[position])
            else if (data[position].public_id != null)
                itemsSelected.add(data[position])
            displayItemState(holder.view, data[position])
        }
        displayItemState(holder.view, data[position])
    }

    // Change background of item when selected
    private fun displayItemState(view: View, item: ModelItem) {
        if (!itemsSelected.contains(item))
            view.setBackgroundColor(Color.WHITE)
        else if (item.public_id != null)
            view.setBackgroundColor(Color.LTGRAY)
    }


    override fun getItemCount() = data.size
}