package com.example.photomaster

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.viewpager.widget.ViewPager
import com.example.photomaster.filters.FilterListFragmentListener
import com.example.photomaster.tune.TuneImageFragmentListener
import com.example.photomaster.util.AssetsUtil
import com.example.photomaster.util.BitmapUtils
import com.example.photomaster.view.VariedGestureController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.yalantis.ucrop.UCrop
import com.zomato.photofilters.imageprocessors.Filter
import com.zomato.photofilters.imageprocessors.subfilters.BrightnessSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.ContrastSubFilter
import com.zomato.photofilters.imageprocessors.subfilters.SaturationSubfilter
import kotlinx.android.synthetic.main.activity_photo_edit.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class photoEdit : AppCompatActivity(), FilterListFragmentListener, TuneImageFragmentListener {
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
    var tuneBrightnessFragment = tuneImageFragment()
    var tuneContrastFragment = tuneImageFragment()
    var tuneSaturationFragment = tuneImageFragment()

    lateinit var picture: Bitmap
    lateinit var filteredPicture: Bitmap
    lateinit var resultPicture: Bitmap

    lateinit var cropPicture: Bitmap

    // modified image values
    private var brightnessFinal = 0
    private var saturationFinal = 1.0f
    private var contrastFinal = 1.0f

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

    //text
    private var mVariedGestureController: VariedGestureController? = null
    private var mAngle = 0
    lateinit var mBitmap: Bitmap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo_edit)
        val viewPager: ViewPager = findViewById(R.id.tab_viewpager)
        val tabLayout: TabLayout = findViewById(R.id.tabs)

        setupViewPager(viewPager)
        // It is used to join TabLayout with ViewPager.
        tabLayout.setupWithViewPager(viewPager)

        //textEdit
        viewPager.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
            }

            override fun onPageSelected(position: Int) {
                Log.e("ViewPager", "position is $position")
                //判断滑动后选择的页面设置相应的RadioButton被选中
                if (position == 0 || position == 2) {
                    resultPicture = BitmapUtils.captureView(root);
                    editImg.setImageBitmap(resultPicture);
                    custom.setBitmap(mBitmap);
                }
            }
        })
        //TextEdit

        // load image from camera or album
        bundle = intent.extras
        path = bundle?.get("imgUri") as Uri
        editImg.setImageURI(path)
        // initialize bitmaps
        picture = editImg.drawable.toBitmap()
        filteredPicture = picture.copy(Bitmap.Config.ARGB_8888, true)
        resultPicture = picture.copy(Bitmap.Config.ARGB_8888, true)

        //text
        mBitmap = BitmapFactory.decodeResource(resources, R.drawable.trans_bg)
            .copy(Bitmap.Config.ARGB_8888, true);

        mVariedGestureController = VariedGestureController(this, custom)
        mVariedGestureController!!.setVariedListener(object :
            VariedGestureController.VariedListener {
            override fun onScale(scaleX: Float, scaleY: Float) {
                custom.setScale(scaleX, scaleY)
            }

            override fun onAngle(angle: Int) {
                custom.setAngle(mAngle + angle)
            }

            override fun onAngleEnd(angle: Int) {
                mAngle = mAngle + angle;
            }

            override fun onShift(horShift: Float, verShift: Float) {
                custom.setShift(horShift, verShift)
            }
        })
    }

    override fun onFilterSelected(filter: Filter) {
        resetControls()
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
        val scaledHeight = scaledWidth
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
    fun startCrop(){
        print("startcrop")

        //处理后图片的uri,此时cropPicture = null
        val cropUri = Uri.parse(
            MediaStore.Images.Media.insertImage(
                contentResolver,
                cropPicture,
                null,
                null
            )
        )
        //uCrop setting
        val uCrop = UCrop.of( path, cropUri)
        val options = UCrop.Options()
        resultPicture = MediaStore.Images.Media.getBitmap(this.contentResolver, cropUri)
    }

    /*

fun textClick(v: View) {
    val textEditorDialogFragment = TextEditorDialogFragment.show(this)
    textEditorDialogFragment.setOnTextEditorListener { inputText, colorCode ->
        val resultBitmap = Bitmap.createBitmap(picture.width, picture.height, picture.config)
        val canvas = Canvas(resultBitmap)

        val scale = v.context.resources.displayMetrics.density
        var paint = Paint(Paint.ANTI_ALIAS_FLAG);
        // text color
        paint.setColor(colorCode)
        // text size in pixels
        paint.setTextSize(30.0f * scale)
        // text shadow
        paint.setShadowLayer(1f, 0f, 1f, colorCode)
        // draw text to the Canvas center
        var bounds = Rect()
        paint.getTextBounds(inputText, 0, inputText.length, bounds)
        canvas.drawBitmap(picture, 0f, 0f, null)
        var x = (resultBitmap.getWidth() - bounds.width())/6;
        var y = (resultBitmap.getHeight() + bounds.height())/5;
        canvas.drawText(
                inputText,
                x * scale,
                y * scale,
                paint
        )

        editImg.setImageBitmap(resultBitmap)
    }
}

fun rotateClick(v: View) {
//        dir = (dir - 90) % 360
    dir = -90
    val resultBitmap = convert(picture, dir)
    if (resultBitmap != null) {
        picture = resultBitmap
        editImg.setImageBitmap(resultBitmap)
    }
}

private fun convert(a: Bitmap, orientationDegree: Int): Bitmap? {
    val w = a.width
    val h = a.height
    val newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) // 创建一个新的和SRC长度宽度一样的位图
    val cv = Canvas(newb)
    val m = Matrix()
    m.postRotate((dir).toFloat()) //旋转-90度
    val new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true)
    cv.drawBitmap(new2, Rect(0, 0, new2.width, new2.height), Rect(0, 0, w, h), null)
    return newb
}
 */



    fun textClick(v: View) {
        val textEditorDialogFragment = TextEditorDialogFragment.show(this)
        textEditorDialogFragment.setOnTextEditorListener { inputText, colorCode ->
//            val resultBitmap = Bitmap.createBitmap(picture.width, picture.height, picture.config)
            val resultBitmap = Bitmap.createBitmap(mBitmap.width, mBitmap.height, mBitmap.config)
            val canvas = Canvas(resultBitmap)

            val scale = v.context.resources.displayMetrics.density
            var paint = Paint(Paint.ANTI_ALIAS_FLAG);
            // text color
            paint.setColor(colorCode)
            // text size in pixels
            paint.setTextSize(30.0f * scale)
            // text shadow
            paint.setShadowLayer(1f, 0f, 1f, colorCode)
            // draw text to the Canvas center
            var bounds = Rect()
            paint.getTextBounds(inputText, 0, inputText.length, bounds)
            canvas.drawBitmap(mBitmap, 0f, 0f, null)
            var x = (resultBitmap.width - bounds.width()) / 7;
            var y = (resultBitmap.height + bounds.height()) / 4;
            canvas.drawText(
                inputText,
                x * scale,
                y * scale,
                paint
            )

            custom.setBitmap(resultBitmap);
//            custom.setImageBitmap(resultBitmap)
//            text.setText(inputText)
//            text.setTextColor(colorCode)
        }
    }

    fun rotateClick(v: View) {
//        dir = (dir - 90) % 360
        var dir = -90
        val resultBitmap = convert(resultPicture, dir)

        if (resultBitmap != null) {
            resultPicture = resultBitmap
            editImg.setImageBitmap(resultBitmap)
        }
    }

    private fun convert(a: Bitmap, orientationDegree: Int): Bitmap? {
        val w = a.width
        val h = a.height
        val newb = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888) // 创建一个新的和SRC长度宽度一样的位图
        val cv = Canvas(newb)
        val m = Matrix()
        m.postRotate((orientationDegree).toFloat()) //旋转-90度
        val new2 = Bitmap.createBitmap(a, 0, 0, w, h, m, true)
        cv.drawBitmap(new2, Rect(0, 0, new2.width, new2.height), Rect(0, 0, w, h), null)
        return newb
    }

    fun tuneBrightness(view: View) {
        // set brightness attribute
        tuneBrightnessFragment.setTuneType("Brightness")
        tuneBrightnessFragment.setListener(this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tuneView, tuneBrightnessFragment)
            .commit()
    }

    fun tuneContrast(view: View) {
        // set contrast attribute
        tuneContrastFragment.setTuneType("Contrast")
        tuneContrastFragment.setListener(this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tuneView, tuneContrastFragment)
            .commit()
    }
    fun tuneSaturation(view: View) {
        // set saturation attribute
        tuneSaturationFragment.setTuneType("Saturation")
        tuneSaturationFragment.setListener(this)
        supportFragmentManager.beginTransaction()
            .replace(R.id.tuneView, tuneSaturationFragment)
            .commit()
    }

    private fun resetControls() {
        editImg.setImageBitmap(picture)
        resultPicture = picture.copy(Bitmap.Config.ARGB_8888, true)
        // remove fragment
        supportFragmentManager.beginTransaction()
                .remove(tuneBrightnessFragment)
                .commit()
        supportFragmentManager.beginTransaction()
                .remove(tuneContrastFragment)
                .commit()
        supportFragmentManager.beginTransaction()
                .remove(tuneSaturationFragment)
                .commit()

        // Reset fragment by recreate the object
        tuneBrightnessFragment = tuneImageFragment()
        tuneBrightnessFragment.setTuneType("Brightness")
        tuneContrastFragment = tuneImageFragment()
        tuneBrightnessFragment.setTuneType("Contrast")
        tuneSaturationFragment = tuneImageFragment()
        tuneBrightnessFragment.setTuneType("Saturation")
    }

    fun reset(view: View) {
        resetControls()
    }

    override fun onBrightnessChanged(brightness: Int) {
        brightnessFinal = brightness
    }

    override fun onContrastChanged(contrast: Float) {
        contrastFinal = contrast
    }

    override fun onSaturationChanged(saturation: Float) {
        saturationFinal = saturation
    }

    override fun onTuneStarted() {
    }

    override fun onTuneCompleted() {
        val tuneFilter = Filter()
        tuneFilter.addSubFilter(BrightnessSubFilter(brightnessFinal))
        tuneFilter.addSubFilter(ContrastSubFilter(contrastFinal))
        tuneFilter.addSubFilter(SaturationSubfilter(saturationFinal))
        filteredPicture = picture.copy(Bitmap.Config.ARGB_8888, true)
        resultPicture = tuneFilter.processFilter(filteredPicture)
        editImg.setImageBitmap(resultPicture)
    }

    fun saveImage(v: View) {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                MainActivity.REQUEST_CODE
            )
        } else {
            val name = "photoMaster-" + System.currentTimeMillis() + ".JPEG"
            val dir = "/storage/emulated/0/Pictures/"
            val file = File(dir)
            if (!file.exists()) {
                file.mkdirs();
            }
            val mFile = File(dir + name)
            var out : FileOutputStream? = null
            try {
                out = FileOutputStream(mFile, false)
                resultPicture.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                val uri = Uri.fromFile(mFile)
                v.context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
                Toast.makeText(v.context, "save success!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(v.context, "save failed!", Toast.LENGTH_SHORT).show()
            } finally {
                if (out != null) {
                    out.close()
                }
            }

        }
    }
}