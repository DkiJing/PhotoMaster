package com.example.photomaster

import android.annotation.SuppressLint
import android.app.Dialog
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
        @SuppressLint("Recycle")
        get() {
            val imageArray = resources.obtainTypedArray(R.array.images_array)
            val models = ArrayList<ImageModel>()
            for(i in 0..3) {
                val p = ImageModel()
                p.image = imageArray.getResourceId(i, -1)
                models.add(p)
            }
            return models
        }
}