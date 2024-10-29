package com.leet.facedetection.camera

import android.annotation.SuppressLint
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.viewbinding.ViewBinding
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.leet.facedetection.base.BaseFragment

abstract class CameraFragment<VB : ViewBinding> : BaseFragment<VB>(), ICamera {
    protected var camera: Camera? = null
    protected var cameraProvider: ProcessCameraProvider? = null
    protected var preview: Preview? = null
    protected var lensFacing: Int = -1
    protected var cameraSelector: CameraSelector? = null

    private val detector by lazy {
        FaceMeshDetection.getClient(
            FaceMeshDetectorOptions.Builder().setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                .build()
        )
    }

    @SuppressLint("WrongConstant", "RestrictedApi")
    override fun startCamera(previewView: PreviewView) {
        if (lensFacing == -1) {
            lensFacing = CameraSelector.LENS_FACING_FRONT
        }

        if (cameraSelector == null) {
            cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
        }
        ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
        )
            .get(CameraXViewModel::class.java)
            .processCameraProvider
            .observe(
                this,
            ) { provider: ProcessCameraProvider? ->
                cameraProvider = provider
                try {
                    // Unbind use cases before rebinding
                    cameraProvider?.unbindAll()
                    // Bind use cases to camera
                    bindCameraTo(previewView)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
    }


    private var analysisUseCase: ImageAnalysis? = null

    @OptIn(ExperimentalGetImage::class)
    private fun bindAnalyticsUseCase() {
        if (cameraProvider == null) {
            return
        }
        if (analysisUseCase != null) {
            cameraProvider!!.unbind(analysisUseCase)
        }
        analysisUseCase = ImageAnalysis.Builder().build()
        analysisUseCase?.setAnalyzer(
            ContextCompat.getMainExecutor(requireActivity()),
        ) {
            it.image?.let { it1 ->
                val image = InputImage.fromMediaImage(
                    it1,
                    it.imageInfo.rotationDegrees
                )
                detector.process(image)
                    .addOnSuccessListener { result ->
                        handleFaceMesh(result, it)
                    }
                    .addOnFailureListener { _ ->
                        it.close()
                    }
            }
        }

        cameraProvider!!.bindToLifecycle(this, cameraSelector!!, analysisUseCase)

    }

    open fun handleFaceMesh(result: MutableList<FaceMesh>, imageProxy: ImageProxy) {}
//    open fun handleFaceDetector(result: MutableList<Face>, imageProxy: ImageProxy) {}

    override fun stopCamera() {
        try {
            cameraProvider?.unbindAll()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun bindCameraTo(previewView: PreviewView) {
        val builder = Preview.Builder()
        preview = builder.build()
        preview!!.setSurfaceProvider(previewView.getSurfaceProvider())

        camera = cameraProvider?.bindToLifecycle(this, cameraSelector!!, preview)

        bindAnalyticsUseCase()

        onUpdateCamera(camera!!)
    }


    open fun onUpdateCamera(camera: Camera) {
        camera.cameraInfo.torchState.removeObservers(this)
        camera.cameraInfo.torchState.observe(this) { state ->
            onObserveTorchState(state)
        }
    }

    open fun onObserveTorchState(state: Int): Int {
        return state
    }

}