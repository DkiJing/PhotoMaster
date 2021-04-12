package com.example.photomaster

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.viewpager.widget.ViewPager
import com.example.photomaster.filters.FilterListFragmentListener
import com.example.photomaster.util.AssetsUtil
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.yalantis.ucrop.UCrop
import com.zomato.photofilters.imageprocessors.Filter
import kotlinx.android.synthetic.main.activity_photo_edit.*
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class photoEdit : AppCompatActivity(), FilterListFragmentListener {
    companion object {
        init {
            System.loadLibrary("SuperResolution")
            System.loadLibrary("NativeImageProcessor")
        }
    }

    // initialize fragment
    private lateinit var filteredFragment: filterFragment
    private lateinit var toolsFragment: toolsFragment
    private lateinit var exportFragment: exportFragment


    lateinit var picture: Bitmap
    lateinit var filteredPicture: Bitmap
    lateinit var resultPicture: Bitmap

    lateinit var cropPicture: Bitmap

    var bundle: Bundle? = null
    private var useGPU = true
    private var superResolutionNativeHandle: Long = 0
    private lateinit var model: MappedByteBuffer
    private lateinit var path: Uri
    private lateinit var clipPath: Uri

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

        setupViewPager(viewPager)
        // It is used to join TabLayout with ViewPager.
        tabLayout.setupWithViewPager(viewPager)

        // load image from camera or album
        bundle = intent.extras
        path = bundle?.get("imgUri") as Uri
        editImg.setImageURI(path)
        picture = editImg.drawable.toBitmap()
        resultPicture = picture.copy(Bitmap.Config.ARGB_8888, true)
    }

    override fun onFilterSelected(filter: Filter) {
        filteredPicture = picture.copy(Bitmap.Config.ARGB_8888, true)
        resultPicture = filter.processFilter(filteredPicture)
        editImg.setImageBitmap(resultPicture)
    }

    private fun setupViewPager(viewPager: ViewPager) {
        val viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        filteredFragment = filterFragment()
        filteredFragment.setListener(this)
        toolsFragment = toolsFragment()
        exportFragment = exportFragment()

        viewPagerAdapter.addFragment(filteredFragment, "Filter")
        viewPagerAdapter.addFragment(toolsFragment, "Tools")
        viewPagerAdapter.addFragment(exportFragment, "Export")
        viewPager.adapter = viewPagerAdapter
    }

    fun detectEmotion(v: View) {
        resultPicture = Emojifier.detectFaces(v.context, resultPicture)
        editImg.setImageBitmap(resultPicture)
    }

    fun enhanceResolution(v: View) {
        //Toast.makeText(v.context, "Enhance", Toast.LENGTH_SHORT).show()
        if(picture.width >= 500 || picture.height >= 500) {
            Toast.makeText(v.context, "Please select an image with lower resolution.", Toast.LENGTH_SHORT).show()
            return
        }
        if (superResolutionNativeHandle == 0L) {
            superResolutionNativeHandle = initTFLiteInterpreter(useGPU)
        }
        if (superResolutionNativeHandle == 0L) {
            Toast.makeText(v.context, "TFLite interpreter failed to create!", Toast.LENGTH_SHORT).show()
        }

        val lowResRGB = IntArray(LR_IMAGE_WIDTH * LR_IMAGE_HEIGHT)
        val resizedPicture = Bitmap.createScaledBitmap(resultPicture, LR_IMAGE_WIDTH, LR_IMAGE_HEIGHT, true)
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
        resultPicture = Bitmap.createScaledBitmap(srImgBitmap, scaledWidth, scaledHeight, true)

        // Set the enhanced and scaled image to the image view.
        editImg.setImageBitmap(resultPicture)
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


    //show info dialog
    fun openInfo(view: View?){
        MaterialAlertDialogBuilder(this)
            .setTitle("About us")
            .setMessage(R.string.info_msg)
            .setNegativeButton("OK", null)
            .show()
    }

    //photo crop
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        System.out.println("onActivityResult()")
//        super.onActivityResult(requestCode, resultCode, data)
//        if(resultCode == Activity.RESULT_OK && requestCode == UCrop.REQUEST_CROP){
//            val resultUri = UCrop.getOutput(data!!)
//
//        } else if (resultCode == UCrop.RESULT_ERROR) {
//            val cropError = UCrop.getError(data!!)
//        }
//
//    }

    fun startCrop(view: View?){
        System.out.println("startcrop()")
        //val destinationUri = Uri.fromFile(File(externalCacheDir, "uCrop.jpg"))
        val resulturi = Uri.parse(
                MediaStore.Images.Media.insertImage(
                        contentResolver,
                        resultPicture,
                        null,
                        null
                )
        )
        cropPicture = resultPicture.copy(Bitmap.Config.ARGB_8888, true)
        //cropPicture = resultPicture
        //处理后图片的uri
        val cropUri = Uri.parse(
            MediaStore.Images.Media.insertImage(
                contentResolver,
                cropPicture,
                null,
                null
            )
        )
        Log.d("TAG", cropUri.toString())
        //uCrop setting
        val uCrop = UCrop.of(resulturi, cropUri)//裁剪前的uri，裁剪后的uri
        val options = UCrop.Options()
        options.setFreeStyleCropEnabled(true)
        cropPicture = MediaStore.Images.Media.getBitmap(this.contentResolver, cropUri)
        resultPicture = cropPicture.copy(Bitmap.Config.ARGB_8888, true)
        editImg.setImageBitmap(resultPicture)
        //resultPicture = MediaStore.Images.Media.getBitmap(this.contentResolver, cropUri
        uCrop.withOptions(options)
        uCrop?.start(this)
    }

     fun photoClip(view: View?) {
        // 调用系统中自带的图片剪裁
        val intent = Intent("com.android.camera.action.CROP")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        intent.setDataAndType(path, "image/*")
        clipPath = Uri.parse("file://" + "/" + Environment.getExternalStorageDirectory().getPath() + "/" + System.currentTimeMillis() + ".jpg")
        //intent.putExtra(MediaStore.EXTRA_OUTPUT, clipPath)
        intent.putExtra("return-data", false)
         intent.putExtra("output", clipPath);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        // 下面这个crop=true是设置在开启的Intent中设置显示的VIEW可裁剪
        intent.putExtra("crop", "true")
        //直接返回bitmaps
        startActivityForResult(intent, 2)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d("RST", resultCode.toString())
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK){
            //val bitmap = decodeUriAsBitmap(imageUri)
            Log.d("CLIP", clipPath.toString())
            resultPicture = decodeUriAsBitmap(clipPath)
            editImg.setImageBitmap(resultPicture)
        }
//        var photoPath: String
//        if(resultCode == Activity.RESULT_OK && requestCode == 2){
//            val bundle = intent!!.extras
//            Log.d("BUN", intent.extras.toString())
//            if (bundle != null) {
//                Log.d("Bun","bundle is not null")
//                //在这里获得了剪裁后的Bitmap对象，可以用于上传
//                val image = bundle.getParcelable<Bitmap>("outputFormat")
//                Log.d("IMG", image.toString())
//                if (image != null) {
//                    Log.d("Tag","image is not null")
//                    resultPicture = image.copy(Bitmap.Config.ARGB_8888, true)
//                }
//                editImg.setImageBitmap(resultPicture)
//            }
//        }
    }
    fun decodeUriAsBitmap(uri: Uri):Bitmap {
        val bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri))
        return bitmap
    }


}