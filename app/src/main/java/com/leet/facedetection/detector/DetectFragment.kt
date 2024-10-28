package com.leet.facedetection.detector

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.facemesh.FaceMesh
import com.leet.facedetection.camera.CameraFragment
import com.leet.facedetection.databinding.FragmentDetectionBinding
import java.util.concurrent.atomic.AtomicBoolean

class DetectFragment : CameraFragment<FragmentDetectionBinding>() {
    override fun initBinding() = FragmentDetectionBinding.inflate(layoutInflater)
    private val needSetupImageSource = AtomicBoolean(true)

    @OptIn(ExperimentalGetImage::class)
    override fun initView() {
        startCamera(binding.previewView)
    }

    override fun handleFaceMesh(result: MutableList<FaceMesh>, imageProxy: ImageProxy) {
        if (needSetupImageSource.compareAndSet(true, false)) {
            val isImageFlipped = lensFacing == CameraSelector.LENS_FACING_FRONT
            val rotationDegrees = imageProxy.imageInfo.rotationDegrees
            if (rotationDegrees == 0 || rotationDegrees == 180) {
                binding.overView.setImageSourceInfo(
                    imageProxy.width,
                    imageProxy.height,
                    isImageFlipped
                )
            } else {
                binding.overView.setImageSourceInfo(
                    imageProxy.height,
                    imageProxy.width,
                    isImageFlipped
                )
            }
        }
        binding.overView.drawRectangleHead(result)
        imageProxy.close()
    }

    override fun addEvent() {
    }

    override fun onFirebaseLogEvent() {
    }
}