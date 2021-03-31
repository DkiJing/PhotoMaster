package com.example.photomaster

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.viewpager.widget.ViewPager
import com.example.photomaster.util.AssetsUtil
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

    // initialize fragment
    private lateinit var filteredFragment: filterFragment

    var viewPagerAdapter: ViewPagerAdapter? = null
    lateinit var picture: Bitmap
    lateinit var filteredPicture: Bitmap
    var bundle: Bundle? = null
    private var useGPU = true
    private var superResolutionNativeHandle: Long = 0
    private lateinit var model: MappedByteBuffer
    private lateinit var path: Uri
    private val MODEL_NAME = "ESRGAN.tflite"
    private val LR_IMAGE_HEIGHT = 50
    private val LR_IMAGE_WIDTH = 50
    private val UPSCALE_FACTOR = 4
    private val SR_IMAGE_HEIGHT = LR_IMAGE_HEIGHT * UPSCALE_FACTOR
    private val SR_IMAGE_WIDTH = LR_IMAGE_WIDTH * UPSCALE_FACTOR

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
        path = bundle?.get("imgUri") as Uri
        editImg.setImageURI(path)
        picture = editImg.drawable.toBitmap()
        filteredPicture = Bitmap.createScaledBitmap(picture, 300, 300, true)
    }

    fun detectEmotion(v: View) {
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

        val lowResRGB = IntArray(LR_IMAGE_WIDTH * LR_IMAGE_HEIGHT)
        val resizedPicture = Bitmap.createScaledBitmap(picture, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT, true)
        resizedPicture.getPixels(lowResRGB, 0, LR_IMAGE_WIDTH, 0, 0, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT)
        val superResRGB = doSuperResolution(lowResRGB)
        if (superResRGB == null) {
            Toast.makeText(v.context, "Enhance resolution failed!", Toast.LENGTH_SHORT).show()
            return
        }

        // Force refreshing the ImageView
        editImg.setImageDrawable(null)
        var srImgBitmap = Bitmap.createBitmap(superResRGB, SR_IMAGE_WIDTH, SR_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888)

        // Get the scaled size of image
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val scaledWidth = displayMetrics.widthPixels
        val scaledHeight = SR_IMAGE_HEIGHT * (scaledWidth / SR_IMAGE_WIDTH)
        srImgBitmap = Bitmap.createScaledBitmap(srImgBitmap, scaledWidth, scaledHeight, true)

        // Set the enhanced and scaled image to the image view.
        editImg.setImageBitmap(srImgBitmap)
        Log.d("TAG", "Finish!!!")
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