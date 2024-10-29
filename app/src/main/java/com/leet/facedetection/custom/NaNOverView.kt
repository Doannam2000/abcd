package com.leet.facedetection.custom

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.widget.FrameLayout
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMesh.NOSE_BRIDGE
import com.google.mlkit.vision.facemesh.FaceMeshPoint
import com.leet.facedetection.R
import com.leet.facedetection.detector.FaceDirection.Companion.getFaceDirection
import java.util.concurrent.ExecutionException
import kotlin.math.atan2

class NaNOverView : FrameLayout {
    enum class NaNType {
        ROLL_IMAGE, ANSWER_QUESTION
    }

    private val lock = Any()
    private val transformationMatrix = Matrix()
    private var imageWidth = 0
    private var imageHeight = 0
    private var scaleFactor = 1.0f
    private var postScaleWidthOffset = 0f
    private var postScaleHeightOffset = 0f
    private var isImageFlipped = false
    private var needUpdateTransformation = true
    private val faceMeshes = ArrayList<FaceMesh>()
    private var mBitmap: Bitmap? = null
    private var typeRollImage = NaNType.ANSWER_QUESTION
    private val listImage = arrayListOf(
        R.drawable.trash_bin,
        R.drawable.good_plant,
        R.drawable.ic_wolf,
        R.drawable.ic_lion,
        R.drawable.ic_cat_meows,
        R.drawable.ic_shore_meows,
        R.drawable.ic_dog_bark_2
    )
    private var timeRandom = 5000L

    private val mHandler = Handler(Looper.getMainLooper())
    private var currentItem = listImage.first()
    private val runnable = object : Runnable {
        override fun run() {
            timeRandom -= 100L
            if (timeRandom >= 0) {
                currentItem = listImage.random()
                mHandler.postDelayed(this, 100L)
            }
        }
    }

    constructor(context: Context?) : super(context!!)

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    )

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context!!,
        attrs,
        defStyleAttr
    )

    @SuppressLint("UseCompatLoadingForDrawables")
    override fun dispatchDraw(canvas: Canvas) {
        if (faceMeshes.isEmpty()) return
        updateTransformationIfNeeded()
        canvas.setMatrix(transformationMatrix)
        if (isRollImage()) {
            handleRollImage(canvas)
        } else {
            handleQuestion(canvas)
        }
        super.dispatchDraw(canvas)
    }

    private fun handleQuestion(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        paint.strokeWidth = 2f
        paint.textSize = 14f
        paint.textScaleX = -1f
        val faceMesh = this.faceMeshes.first()

        val pointTop = faceMesh.getPoints(NOSE_BRIDGE).first()
        val pointBottom = faceMesh.getPoints(NOSE_BRIDGE).last()

        val centerHead = PointF(
            2 * pointTop.position.x - pointBottom.position.x,
            2 * pointTop.position.y - pointBottom.position.y
        )
        val bottomRectangle = PointF(
            2 * centerHead.x - pointTop.position.x, 2 * centerHead.y - pointTop.position.y
        )
        val rectangle = RectF(
            centerHead.x - 100,
            bottomRectangle.y - 125,
            centerHead.x + 100,
            bottomRectangle.y
        )

        canvas.drawRoundRect(rectangle, 10f, 10f, paint)
        paint.color = Color.BLACK

        canvas.drawText(
            "xmlns:android",
            centerHead.x + 100, bottomRectangle.y - 50, paint
        )
        getEulerXFromImage()?.let {
            Log.d("zzzzzz", "handleQuestion: " + getFaceDirection(it))
        }
    }

    @Throws(ExecutionException::class, InterruptedException::class)
    fun getEulerXFromImage(): Float? {
        try {
            var faceMeshPoint1: FaceMeshPoint?
            var canNext2: Boolean
            var canNext: Boolean
            var faceMeshPoint2: Any? = null
            val allPoints = faceMeshes.first().allPoints
            val iterator: Iterator<FaceMeshPoint> = allPoints.iterator()
            while (true) {
                if (!iterator.hasNext()) {
                    faceMeshPoint1 = null
                    break
                }
                faceMeshPoint1 = iterator.next()
                canNext = faceMeshPoint1.index == 130
                if (canNext) {
                    break
                }
            }
            val faceMeshPoint = faceMeshPoint1
            val iterator2: Iterator<FaceMeshPoint> = allPoints.iterator()
            while (true) {
                if (!iterator2.hasNext()) {
                    break
                }
                val next = iterator2.next()
                canNext2 = next.index == 263
                if (canNext2) {
                    faceMeshPoint2 = next
                    break
                }
            }
            return calculateEulerX(faceMeshPoint, faceMeshPoint2 as FaceMeshPoint?)
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
        return ((atan2(fArr[1].toDouble(), fArr[0].toDouble()).toFloat()) * (180f)) / 3.1415927f
    }

    private fun handleRollImage(canvas: Canvas) {
        val faceMesh = this.faceMeshes.first()

        val pointTop = faceMesh.getPoints(NOSE_BRIDGE).first()
        val pointBottom = faceMesh.getPoints(NOSE_BRIDGE).last()

        val centerHead = PointF(
            2 * pointTop.position.x - pointBottom.position.x,
            2 * pointTop.position.y - pointBottom.position.y
        )
        val bottomRectangle = PointF(
            2 * centerHead.x - pointTop.position.x, 2 * centerHead.y - pointTop.position.y
        )
        val rectangle = Rect(
            centerHead.x.toInt() - 100,
            bottomRectangle.y.toInt() - 125,
            centerHead.x.toInt() + 100,
            bottomRectangle.y.toInt()
        )

        if (mBitmap?.isRecycled != true) {
            mBitmap?.recycle()
        }
        mBitmap = BitmapFactory.decodeResource(resources, currentItem)
        mBitmap?.let {
            canvas.drawBitmap(it, null, rectangle, null)
        }
    }

    fun drawRectangleHead(result: MutableList<FaceMesh>) {
        faceMeshes.clear()
        faceMeshes.addAll(result)
        invalidate()
    }

    fun setImageSourceInfo(imageWidth: Int, imageHeight: Int, isFlipped: Boolean) {
        synchronized(lock) {
            this.imageWidth = imageWidth
            this.imageHeight = imageHeight
            this.isImageFlipped = isFlipped
            needUpdateTransformation = true
            mHandler.post(runnable)
        }
        updateTransformationIfNeeded()
    }

    private fun updateTransformationIfNeeded() {
        if (!needUpdateTransformation || imageWidth <= 0 || imageHeight <= 0) {
            return
        }
        val viewAspectRatio = width.toFloat() / height
        val imageAspectRatio: Float = imageWidth.toFloat() / imageHeight
        postScaleWidthOffset = 0f
        postScaleHeightOffset = 0f
        if (viewAspectRatio > imageAspectRatio) {
            // The image needs to be vertically cropped to be displayed in this view.
            scaleFactor = width.toFloat() / imageWidth
            postScaleHeightOffset = (width.toFloat() / imageAspectRatio - height) / 2
        } else {
            // The image needs to be horizontally cropped to be displayed in this view.
            scaleFactor = height.toFloat() / imageHeight
            postScaleWidthOffset = (height.toFloat() * imageAspectRatio - width) / 2
        }

        transformationMatrix.reset()
        transformationMatrix.setScale(scaleFactor, scaleFactor)
        transformationMatrix.postTranslate(-postScaleWidthOffset, -postScaleHeightOffset)

        if (isImageFlipped) {
            transformationMatrix.postScale(-1f, 1f, width / 2f, height / 2f)
        }
        needUpdateTransformation = false
    }

    private fun isRollImage(): Boolean {
        return typeRollImage == NaNType.ROLL_IMAGE
    }

}