package com.leet.facedetection.detector

enum class FaceDirection {
    LEFT, CENTER, RIGHT;

    companion object {
        fun getFaceDirection(eulerXFromImage: Float): FaceDirection {
            if (eulerXFromImage > 25) {
                return LEFT
            }
            if (eulerXFromImage < -25) {
                return RIGHT
            }
            return CENTER
        }

        fun getFaceDirectionString(eulerXFromImage: Float?): String {
            return eulerXFromImage?.let { getFaceDirection(it).name } ?: "NULL"
        }
    }
}