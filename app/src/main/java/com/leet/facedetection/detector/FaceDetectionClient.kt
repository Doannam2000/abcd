package com.leet.facedetection.detector

import android.media.Image
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMeshDetection
import com.google.mlkit.vision.facemesh.FaceMeshDetector
import com.google.mlkit.vision.facemesh.FaceMeshDetectorOptions
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import java.util.concurrent.ExecutionException
import kotlin.math.atan2

class FaceDetectionClient private constructor() {
    var faceDetection: FaceMeshDetector?
        get() {
            val faceMeshDetector = Companion.faceDetection
            if (faceMeshDetector != null) {
                return faceMeshDetector
            }
            return null
        }
        set(faceMeshDetector) {
            Companion.faceDetection = faceMeshDetector
        }

    @Throws(ExecutionException::class, InterruptedException::class)
    fun getEulerXFromImage(image: Image?, i2: Int): Float? {
        try {
            var obj: FaceMeshPoint?
            var z: Boolean
            var z2: Boolean
            if (this.faceDetection == null) {
                return 0f
            }
            if (image == null) {
                return null
            }
            val faces = Tasks.await(
                faceDetection!!.process(image, i2)
            )
            var obj2: Any? = null
            if (faces.isEmpty()) {
                return null
            }

            val allPoints = ((faces as List<Any>).first() as FaceMesh).allPoints
            val it: Iterator<FaceMeshPoint> = allPoints.iterator()
            while (true) {
                if (!it.hasNext()) {
                    obj = null
                    break
                }
                obj = it.next()
                z2 = obj.index == 130
                if (z2) {
                    break
                }
            }
            //        Intrinsics.checkNotNull(obj);
            val faceMeshPoint = obj
            val allPoints2 = ((faces as List<Any>).first() as FaceMesh).allPoints
            //        Intrinsics.checkNotNullExpressionValue(allPoints2, "faces.first().allPoints");
            val it2: Iterator<FaceMeshPoint> = allPoints2.iterator()
            while (true) {
                if (!it2.hasNext()) {
                    break
                }
                val next = it2.next()
                z = next.index == 263
                if (z) {
                    obj2 = next
                    break
                }
            }
            //        Intrinsics.checkNotNull(obj2);
            return calculateEulerX(faceMeshPoint, obj2 as FaceMeshPoint?)
        } catch (e: Exception) {
            return null
        }
    }

    private fun calculateEulerX(
        faceMeshPoint: FaceMeshPoint?, faceMeshPoint2: FaceMeshPoint?
    ): Float {
        val fArr = floatArrayOf(
            faceMeshPoint2!!.position.x - faceMeshPoint!!.position.x,
            faceMeshPoint2.position.y - faceMeshPoint.position.y,
            faceMeshPoint2.position.z - faceMeshPoint.position.z
        )
        val atan2 =
            ((atan2(fArr[1].toDouble(), fArr[0].toDouble()).toFloat()) * (180f)) / 3.1415927f
//        val d2 = 2.0f
//        val atan3 = atan2(
//            fArr[2].toDouble(), sqrt(
//                ((fArr[0].pow(d2)) + (fArr[1].pow(d2)).toDouble()
//            ).toFloat().toDouble()
//        ))
//        Log.d("doanvv", "calculateEulerX: atan2 = $atan2, atan3 = $atan3")
        return atan2
    }

    companion object {
        private var isInitializer = false
        val INSTANCE: FaceDetectionClient = FaceDetectionClient()
        var faceDetection: FaceMeshDetector? = null

        @JvmStatic
        fun init() {
            try {
                if (isInitializer) {
                    return
                }
                val client = FaceMeshDetection.getClient(
                    FaceMeshDetectorOptions.Builder().setUseCase(FaceMeshDetectorOptions.FACE_MESH)
                        .build()
                )
                INSTANCE.faceDetection = client
                isInitializer = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}