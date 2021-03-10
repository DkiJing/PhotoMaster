package com.example.photomaster

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class ImageAdapter(val c: Context, var models: ArrayList<ImageModel>):
        RecyclerView.Adapter<ImageHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.image_item, null)
        return ImageHolder(v)
    }

    override fun getItemCount(): Int {
        return models.size
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.mImage.setImageResource(models[position].image)
    }
}