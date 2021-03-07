package com.example.photomaster

import android.app.Dialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

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
        }
    }
}