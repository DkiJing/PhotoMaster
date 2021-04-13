package com.example.photomaster

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var currentPhotoPath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //打开图片按钮的点击事件，只有写了才能实现阴影效果
        addButton.setOnClickListener {
            bottomSheet.newInstance().show(supportFragmentManager, "test")
        }
        checkPermissions()
    }

    /**
     * Checks the dynamically-controlled permissions and requests missing permissions from end user.
     */
    protected fun checkPermissions() {
        val missingPermissions: MutableList<String> = ArrayList()
        // check all required dynamic permissions
        for (permission in PERMISSIONS_REQ) {
            val result = ContextCompat.checkSelfPermission(this, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission)
            }
        }
        if (missingPermissions.isNotEmpty()) {
            // request all missing permissions
            val permissions = missingPermissions
                    .toTypedArray()
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
        } else {
            val grantResults = IntArray(PERMISSIONS_REQ.size)
            Arrays.fill(grantResults, PackageManager.PERMISSION_GRANTED)
            onRequestPermissionsResult(REQUEST_CODE, PERMISSIONS_REQ,
                    grantResults)
        }
    }

//    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            REQUEST_CODE -> {
                var index = permissions.size - 1
                while (index >= 0) {
                    if ((grantResults.isNotEmpty() &&
                                    grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                        // Permission is granted. Continue the action or workflow
                        // in your app.
                        return
                    } else {
                        Toast.makeText(this, "Required permission '" + permissions[index]
                                + "' not granted, exiting", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
                return
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
                "JPEG_${timeStamp}_", /* prefix */
                ".jpg", /* suffix */
                storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    fun openCamera(v: View) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val file = createImageFile()
        val uri = FileProvider.getUriForFile(this, this.applicationContext.packageName + ".fileprovider", file)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(intent, TAKE_PHOTO_ID)
    }

    fun openAlbum(v: View) {
        intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
            startActivityForResult(this, IMAGE_GALLERY_REQUEST_CODE)
        }
    }

    fun selectPhoto(v: View) {
        // TODO send intent to the editPhoto activity
        Toast.makeText(this, "A recent photo is selected.", Toast.LENGTH_SHORT).show()
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(resultCode == RESULT_OK) {
            if(requestCode == IMAGE_GALLERY_REQUEST_CODE) {
                if(data != null && data.data != null) {
                    val image = data.data
                    intent = Intent(this, photoEdit::class.java)
                    intent.putExtra("imgUri", image)
                    startActivity(intent)
                }
            }
            if(requestCode == TAKE_PHOTO_ID) {
                val file = File(currentPhotoPath)
                val image: Uri = FileProvider.getUriForFile(this, this.applicationContext.packageName + ".fileprovider", file)
                intent = Intent(this, photoEdit::class.java)
                intent.putExtra("imgUri", image)
                startActivity(intent)
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    companion object {
        const val TAKE_PHOTO_ID = 1
        const val REQUEST_CODE = 2
        const val IMAGE_GALLERY_REQUEST_CODE = 3
        val PERMISSIONS_REQ = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        )
    }

}
