package com.leet.facedetection.base

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import java.util.concurrent.atomic.AtomicBoolean


abstract class BaseFragment<VB : ViewBinding> : Fragment() {
    protected var mHandler: Handler = Handler(Looper.getMainLooper())
    lateinit var binding: VB
    val isShowing = AtomicBoolean(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val bd = initBinding()
        return if (bd == null) {
            null
        } else {
            binding = bd
            binding.root
        }
    }

    override fun onResume() {
        super.onResume()
        isShowing.set(true)
    }

    override fun onPause() {
        isShowing.set(false)
        super.onPause()
    }

    abstract fun initBinding(): VB?

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initView()
        addEvent()

        onFirebaseLogEvent()
    }

    open fun onBackPressAds() {
        onBackPressed()

    }

    abstract fun initView()

    abstract fun addEvent()

    abstract fun onFirebaseLogEvent()
    open fun onBackPressed() {
        activity?.supportFragmentManager?.popBackStack()
    }

    override fun onSaveInstanceState(outState: Bundle) {}

}