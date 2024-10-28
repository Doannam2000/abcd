package com.leet.facedetection.camera

import androidx.camera.view.PreviewView

interface ICamera {
    fun startCamera(previewView: PreviewView)
    fun stopCamera()
}