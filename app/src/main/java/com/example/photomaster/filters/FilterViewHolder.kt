package com.example.photomaster.filters

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.photomaster.R

class FilterViewHolder(v: View): RecyclerView.ViewHolder(v) {
    var mImage: ImageView
    var mFilterName: TextView

    init {
        mImage = v.findViewById(R.id.filter)
        mFilterName = v.findViewById(R.id.filter_name)
    }
}