package com.example.photomaster.tune

interface TuneImageFragmentListener {
    fun onBrightnessChanged(brightness: Int)
    fun onContrastChanged(contrast: Float)
    fun onSaturationChanged(saturation: Float)
    fun onTuneStarted()
    fun onTuneCompleted()
}