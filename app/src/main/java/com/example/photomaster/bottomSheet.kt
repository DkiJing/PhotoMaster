package com.example.photomaster

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.bottonsheet_dialog.*

/**
 * @author Shigehiro Soejima
 */
class bottomSheet : BottomSheetDialogFragment() {

    companion object {
        fun newInstance() = bottomSheet()
    }

    override fun setupDialog(dialog: Dialog, style: Int) {
        dialog.apply {
            setContentView(R.layout.bottonsheet_dialog)
            images_list.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.HORIZONTAL, false)
            val imageAdapter = activity?.let { ImageAdapter(it, images) }
            images_list.adapter = imageAdapter
        }
    }

    private val images: ArrayList<ImageModel>
        @RequiresApi(Build.VERSION_CODES.Q)
        @SuppressLint("Recycle")
        get() {
            val models = ArrayList<ImageModel>()
            val projection = arrayOf(MediaStore.Images.Media.DATA,
                    MediaStore.Images.Media.DATE_TAKEN)

            val cursor = context!!.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    null,
                    null,
                    MediaStore.Files.FileColumns.DATE_MODIFIED + " DESC"
            )
            while (cursor != null && cursor.moveToNext()) {
                val p = ImageModel()
                p.imageName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                models.add(p)
            }
            return models
        }
}