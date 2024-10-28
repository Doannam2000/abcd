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
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.facemesh.FaceMesh
import com.google.mlkit.vision.facemesh.FaceMesh.NOSE_BRIDGE
import com.leet.facedetection.R


class NaNOverView : FrameLayout {


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
        updateTransformationIfNeeded()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.RED
        paint.strokeWidth = 5f

        canvas.setMatrix(transformationMatrix)

        if (faceMeshes.isEmpty()) return
        val faceMesh = faceMeshes.first()
        val pointTop = faceMesh.getPoints(NOSE_BRIDGE).first()
        val pointBottom = faceMesh.getPoints(NOSE_BRIDGE).last()

        val centerHead = PointF(
            2 * pointTop.position.x - pointBottom.position.x,
            2 * pointTop.position.y - pointBottom.position.y
        )

        val bottomRectangle = PointF(
            2 * centerHead.x - pointTop.position.x, 2 * centerHead.y - pointTop.position.y
        )
        val topRectangle = PointF(
            2 * bottomRectangle.x - pointBottom.position.x,
            2 * bottomRectangle.y - pointBottom.position.y
        )

        val bitmapx = BitmapFactory.decodeResource(resources, R.drawable.trash_bin)
        canvas.drawBitmap(Bitmap.createScaledBitmap(bitmapx, 120, 120, false),100f,100f,paint)


//        canvas.drawRect(0f, topRectangle.y, width.toFloat(), bottomRectangle.y, paint)
        super.dispatchDraw(canvas)

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

}