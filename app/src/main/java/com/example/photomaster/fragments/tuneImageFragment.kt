package com.example.photomaster.fragments

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import com.example.photomaster.R
import com.example.photomaster.tune.TuneImageFragmentListener

/**
 * A simple [tuneImageFragment] subclass.
 * Use the [tuneImageFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
@Suppress("SENSELESS_COMPARISON")
class tuneImageFragment : Fragment(), SeekBar.OnSeekBarChangeListener {
    private var listener: TuneImageFragmentListener? = null
    private var tuneType = ""
    private lateinit var seekBarView: SeekBar
    private lateinit var maxValue: TextView
    private lateinit var minValue: TextView
    private lateinit var currValue: TextView

    fun setListener(listener: TuneImageFragmentListener) {
        this.listener = listener
    }

    fun setTuneType(type: String) {
        this.tuneType = type
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_tune_image, container, false)
        seekBarView = view.findViewById(R.id.seekbar)
        maxValue = view.findViewById(R.id.maxValueTextView)
        minValue = view.findViewById(R.id.minValueTextView)
        currValue = view.findViewById(R.id.currentValueTextView)
        seekBarView.setOnSeekBarChangeListener(this)
        if (tuneType != null) {
            if (tuneType == "Brightness") {
                seekBarView.max = 100
                seekBarView.min = -100
                seekBarView.progress = 0
                maxValue.text = "100"
                minValue.text = "-100"
                currValue.text = "0"
            } else if (tuneType == "Contrast") {
                seekBarView.max = 20
                seekBarView.min = 0
                seekBarView.progress = 0
                maxValue.text = "20"
                minValue.text = "0"
                currValue.text = ""
            } else if (tuneType == "Saturation") {
                seekBarView.max = 30
                seekBarView.min = 0
                seekBarView.progress = 0
                maxValue.text = "30"
                minValue.text = "0"
                currValue.text = ""
            }
        }
        return view
    }

    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
        if (listener != null) {
            if (tuneType == "Brightness") {
                listener!!.onBrightnessChanged(progress)
            } else if (tuneType == "Contrast") {
                val prog = progress + 10
                val contrastProgress = .10f * prog
                listener!!.onContrastChanged(contrastProgress)
            } else if (tuneType == "Saturation") {
                val prog = progress + 10
                val saturationProgress = .10f * prog
                listener!!.onSaturationChanged(saturationProgress)
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {
        if (listener != null) {
            listener!!.onTuneStarted()
        }
    }

    override fun onStopTrackingTouch(seekBar: SeekBar?) {
        if (listener != null) {
            listener!!.onTuneCompleted()
        }
    }
}