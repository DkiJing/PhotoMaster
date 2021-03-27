package com.example.photomaster

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import kotlinx.android.synthetic.main.activity_photo_edit.*
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class photoEdit : AppCompatActivity() {
    companion object {
        init {
            System.loadLibrary("SuperResolution")
        }
    }

    var viewPagerAdapter: ViewPagerAdapter? = null
    lateinit var picture: Bitmap
    var bundle: Bundle? = null
    private var useGPU = true
    private var superResolutionNativeHandle: Long = 0
    private lateinit var model: MappedByteBuffer
    private val MODEL_NAME = "ESRGAN.tflite"

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

    fun enhanceResolution(v: View) {
        Toast.makeText(v.context, "Enhance", Toast.LENGTH_SHORT).show()
        if (superResolutionNativeHandle == 0L) {
            superResolutionNativeHandle = initTFLiteInterpreter(useGPU)
        }
        if (superResolutionNativeHandle == 0L) {
            Toast.makeText(v.context, "TFLite interpreter failed to create!", Toast.LENGTH_SHORT).show()
        }

        val lowResRGB = IntArray(picture.width * picture.height)
        val superResRGB = doSuperResolution(lowResRGB)
        if (superResRGB == null) {
            Toast.makeText(v.context, "Enhance resolution failed!", Toast.LENGTH_SHORT).show()
            return
        }
        Log.d("TAG", "image width: " + picture.width)
        Log.d("TAG", "image height" + picture.height)
        // Force refreshing the ImageView
        editImg.setImageDrawable(null)
        val srImgBitmap = Bitmap.createBitmap(superResRGB, 200, 200, Bitmap.Config.ARGB_8888)
        editImg.setImageBitmap(srImgBitmap)
        Log.d("TAG", "Finish!!")
    }

    @WorkerThread
    @Synchronized
    fun doSuperResolution(lowResRGB: IntArray?): IntArray? {
        return superResolutionFromJNI(superResolutionNativeHandle, lowResRGB!!)
    }

    @Throws(IOException::class)
    private fun loadModelFile(): MappedByteBuffer {
        AssetsUtil.getAssetFileDescriptorOrCached(applicationContext, MODEL_NAME)
            .use { fileDescriptor ->
                FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                    val fileChannel: FileChannel = inputStream.channel
                    val startOffset: Long = fileDescriptor.startOffset
                    val declaredLength: Long = fileDescriptor.declaredLength
                    return fileChannel.map(
                        FileChannel.MapMode.READ_ONLY,
                        startOffset,
                        declaredLength
                    )
                }
            }
    }

    private fun initTFLiteInterpreter(useGPU: Boolean): Long {
        try {
            model = loadModelFile()
        } catch (e: IOException) {
            Log.d("TAG", "Fail to load model$e")
        }
        return initWithByteBufferFromJNI(model, useGPU)
    }

    private external fun superResolutionFromJNI(
            superResolutionNativeHandle: Long,
            lowResRGB: IntArray
    ): IntArray?

    private external fun initWithByteBufferFromJNI(
            modelBuffer: MappedByteBuffer,
            useGPU: Boolean
    ): Long

    private external fun deinitFromJNI(superResolutionNativeHandle: Long)

}