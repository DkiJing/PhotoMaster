package com.example.photomaster.filters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.photomaster.R
import com.zomato.photofilters.utils.ThumbnailItem

class FilterViewAdapter(val c: Context, val filterItemList: List<ThumbnailItem>, val listener: FilterListFragmentListener)
    : RecyclerView.Adapter<FilterViewHolder>() {

    private var selectedIndex = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.filter_item, null)
        return FilterViewHolder(v)
    }

    override fun getItemCount(): Int {
        return filterItemList.size
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.mImage.load(filterItemList[position].image)
        holder.mFilterName.text = filterItemList[position].filterName
        holder.mImage.setOnClickListener {
            listener.onFilterSelected(filterItemList[position].filter)
            selectedIndex = position
            notifyDataSetChanged()
        }
    }

}