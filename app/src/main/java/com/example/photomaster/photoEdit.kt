package com.example.photomaster

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_photo_edit.*


class photoEdit : AppCompatActivity() {

    var viewPagerAdapter: ViewPagerAdapter? = null
    lateinit var picture: Bitmap
    var bundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)
        val viewPager: ViewPager = findViewById(R.id.tab_viewpager)
        val tabLayout: TabLayout = findViewById(R.id.tabs)
        viewPagerAdapter = ViewPagerAdapter(
                supportFragmentManager)
        viewPager.adapter = viewPagerAdapter
        //viewPager.setAdapter(viewPagerAdapter)
        // It is used to join TabLayout with ViewPager.
        tabLayout.setupWithViewPager(viewPager)

        // load image from camera or album
        bundle = intent.extras
        val path: Uri = bundle?.get("imgUri") as Uri
        editImg.setImageURI(path)
        picture = editImg.drawable.toBitmap()
    }

    fun detectEmotion(v: View) {
        Toast.makeText(v.context, "Detecting emotion...", Toast.LENGTH_SHORT).show()
        val mResultBitmap = Emojifier.detectFaces(v.context, picture)
        editImg.setImageBitmap(mResultBitmap)
    }
}