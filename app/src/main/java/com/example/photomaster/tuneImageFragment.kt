package com.example.photomaster

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

/**
 * A simple [Fragment] subclass.
 * Use the [tuneImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class tuneImageFragment : Fragment() {
    private var tuneType = ""

    fun setTuneType(type: String) {
        this.tuneType = type
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tune_image, container, false)
    }
}