package com.example.photomaster.recentimage

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.photomaster.R

class ImageHolder(v: View) : RecyclerView.ViewHolder(v){
    var mImage: ImageView
    var mRelative: RelativeLayout

    init {
        mImage = v.findViewById(R.id.image)
        mRelative = v.findViewById(R.id.relative)
    }
}