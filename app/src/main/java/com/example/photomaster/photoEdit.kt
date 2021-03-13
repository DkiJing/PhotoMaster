package com.example.photomaster

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_photo_edit.*

class photoEdit : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)
        val bundle: Bundle? = intent.extras
        val path: Uri = bundle?.get("imgUri") as Uri
        editImg.setImageURI(path)
    }
}