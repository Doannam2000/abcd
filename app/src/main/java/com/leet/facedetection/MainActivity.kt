package com.leet.facedetection

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.leet.facedetection.databinding.ActivityMainBinding
import com.leet.facedetection.detector.DetectFragment

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        supportFragmentManager.beginTransaction().replace(R.id.main_container, DetectFragment()).commitAllowingStateLoss()


    }


}