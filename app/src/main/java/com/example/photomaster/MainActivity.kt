package com.example.photomaster

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //打开图片按钮的点击事件，只有写了才能实现阴影效果
        addButton.setOnClickListener {
            bottomSheet.newInstance().show(supportFragmentManager, "test")
        }

    }

}
