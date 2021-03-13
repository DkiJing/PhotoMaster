package com.example.photomaster

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_photo_edit.*

class photoEdit : AppCompatActivity() {
    var viewPagerAdapter: ViewPagerAdapter? = null
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

        val bundle: Bundle? = intent.extras
        val path: Uri = bundle?.get("imgUri") as Uri
        editImg.setImageURI(path)
    }
}