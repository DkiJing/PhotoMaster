package com.example.photomaster

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageDecoder
import android.media.Image
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
//import kotlinx.android.synthetic.main.bottonsheet_dialog.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //打开图片按钮的点击事件，只有写了才能实现阴影效果
        addButton.setOnClickListener {
            bottomSheet.newInstance().show(supportFragmentManager, "test")
        }
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), MainActivity.REQUEST_CODE)
        }
    }

    fun openCamera(v: View) {
        intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            return
        }
        startActivity(intent)
    }

    fun openAlbum(v: View) {
        intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == RESULT_OK) {
            if(requestCode == IMAGE_GALLERY_REQUEST_CODE) {
                if(data != null && data.data != null) {
                    val image = data.data
                    val source = ImageDecoder.createSource(contentResolver, image!!)
                    val bitmap = ImageDecoder.decodeBitmap(source)
                    // TODO set bitmap on the BitMap UI
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        private const val REQUEST_CODE = 1
        private const val IMAGE_GALLERY_REQUEST_CODE = 2
        private val PERMISSIONS_REQ = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE
        )
    }

}
