package com.maker.surface

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import com.airbnb.lottie.BuildConfig
import com.airbnb.lottie.LottieDrawable
import com.maker.drawer.StoryBaseDrawer
import com.maker.drawer.background.IStoryBackgroundComposer
import com.maker.drawer.background.StoryBackgroundDrawer
import com.maker.filter.gpu.StoryGpuFilterInterface
import kotlinx.coroutines.currentCoroutineContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.concurrent.atomic.AtomicReference
import javax.microedition.khronos.egl.EGLConfig
import kotlin.math.roundToInt

class StoryRenderer(
    private val mCallback: StoryRenderListener?,
    private val isExport: Boolean = false
) :
    GlFrameBufferObjectRenderer() {

    //forceUpdateTex when surfaceChange while state is Pause
    private var forceUpdateTex = false


    companion object {
        @JvmStatic
        var USE_BETTER_LOTTIE_GL = false
        const val targetFPS = 60

    }


    //    val CUBE = floatArrayOf(
//        -1.0f, 1.0f, 0.0f,
//        1.0f, 1.0f, 0.0f,
//        -1.0f, -1.0f, 0.0f,
//        1.0f, -1.0f, 0.0f,
//    )
//    private val TEXTURE_NO_ROTATION = floatArrayOf(
//        0.0f, 1.0f,
//        1.0f, 1.0f,
//        0.0f, 0.0f,
//        1.0f, 0.0f
//    )
    val CUBE = floatArrayOf(
        -1.0f, -1.0f,
        1f, -1.0f,
        -1.0f, 1f,
        1f, 1f
    )
    private val TEXTURE_NO_ROTATION = floatArrayOf(
        0.0f, 0f,
        1f, 0f,
        0.0f, 1.0f,
        1f, 1.0f
    )

    private var mGLCubeBuffer: FloatBuffer = ByteBuffer.allocateDirect(CUBE.size * 4)
        .order(
            ByteOrder.nativeOrder()
        ).asFloatBuffer()

    private var mGLTextureBuffer =
        ByteBuffer.allocateDirect(TEXTURE_NO_ROTATION.size * 4)
            .order(
                ByteOrder.nativeOrder()
            ).asFloatBuffer()

    init {
        mGLCubeBuffer.put(CUBE).position(0)
        mGLTextureBuffer.put(TEXTURE_NO_ROTATION).position(0)
    }

    enum class State {
        NONE,
        PLAYING,
        COMPLETE,
        SEEK,
        PAUSE,
        STOP,
        DESTROY
    }

    val progress = AtomicReference<Float>(0.0f)
    var seekProgress = 0f
    private var animatingFrame = -1
    private var animatingProgress = -1f

    private var isNewFilter = false
    private var width = 1080
    private var height = 1080


    private var bitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val matrix = Matrix()


    //gl drawer object
    private var storyDrawer: StoryBaseDrawer? = null
    private var backgroundDrawer = StoryBackgroundDrawer()


    //gl framebuffer object
    private val mFilterFramebufferObject = GlFramebufferObject()

    private var mFilter: StoryGpuFilterInterface? = null

    var lottieDrawable: LottieDrawable? = null
    var betterLottieGL: BetterLottieGL? = null
    fun setupBetterLottieGL(listener: BetterLottieGL.OnLottieGLListener) {
        lottieDrawable?.let {
            Log.d("tttt", "setup BetterLottieGL width = $width, height = $height")
            betterLottieGL = BetterLottieGL(width, height, it, matrix, listener).apply {
            }

        }
    }

    fun startBetterLottieGL() {
        betterLottieGL?.start()
    }

    fun stopBetterLottieGL() {
        try {
            betterLottieGL?.stop()
        } catch (_: Exception) {

        }
    }

    private val state = AtomicReference<State>(State.NONE)
    private val prevState = AtomicReference<State>(State.NONE)

    fun setFilter(filter: StoryGpuFilterInterface?) {
        mFilter?.release()
        mFilter = filter
        isNewFilter = true
    }


    override fun onSurfaceCreated(config: EGLConfig?) {
//        mFilter = GlTvShopFilter()
//        isNewFilter = true
        GLES20.glClearColor(0f, 0f, 0f, 0f)
//
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        if (true) Log.d("dwww", "onSurfaceChanged $width x $height")


        if (this.width == width && this.height == height) {
            storyDrawer?.let {
                return
            }
        }

        //forceUpdateTex when surfaceChange while state is Pause
        synchronized(this) {
            forceUpdateTex = true
        }

        this.width = width
        this.height = height



        backgroundDrawer.prepare()
        mFilterFramebufferObject.setup(width, height)
        backgroundDrawer.updateRes(Pair(width, height))
        mFilter?.let {
            isNewFilter = true
        }

        GLES20.glViewport(0, 0, width, height)
        calcMatrix()
        bitmap?.let {
            if (it.isRecycled.not())
                it.recycle()
        }
        bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            canvas = Canvas(this)
        }



        prepareStory()
    }


    fun calcMatrix() {
        this.lottieDrawable?.let {
            if (it.intrinsicWidth != 0 && it.intrinsicHeight != 0) {
                if (BuildConfig.DEBUG) Log.d(
                    "dwww",
                    "calcMatrix ${it.intrinsicWidth} x ${it.intrinsicHeight}"
                )
                if (BuildConfig.DEBUG) Log.d("dwww", "calcMatrix ${width} x ${height}")
                matrix.reset()
                matrix.preScale(
                    1f * width / it.intrinsicWidth,
                    1f * height / it.intrinsicHeight
                )
            }
        }

    }

    private fun prepareStory() {
        bitmap?.eraseColor(Color.TRANSPARENT)
        storyDrawer = StoryBaseDrawer()
        storyDrawer?.prepare(bitmap)
    }


    @Synchronized
    override fun onDrawFrame(fbo: GlFramebufferObject?) {
        val startTime = System.currentTimeMillis()
        var isNewFrame = false
//        if (state.get() == State.SEEK || state.get() == State.PLAYING)
//            if (BuildConfig.DEBUG) Log.d("dwww", "onDrawFrame ${state.get()} $progress")
//        if (BuildConfig.DEBUG) Log.d("dwww", "onDrawFrame " + progress)

        if (state.get() == State.SEEK) {
//            if (BuildConfig.DEBUG) Log.d("dwww2", "seek $seekProgress")
            if (true) return
//            progress.set(seekProgress)
//            state.set(State.PLAYING)
        }
        GLES20.glViewport(0, 0, width, height)
        if (isNewFilter) {

            mFilter?.let {
                it.setup(fbo)
                GLES20.glUseProgram(it.getProgramDraw())
                it.setFrameSize(fbo!!.width, fbo.height)
                it.rotationSelf()
            }

            isNewFilter = false
        }

        mFilter?.let {
            mFilterFramebufferObject.enable()
            GLES20.glViewport(0, 0, mFilterFramebufferObject.width, mFilterFramebufferObject.height)
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        backgroundDrawer.let {
            it.drawFrame()
            GLES20.glEnable(GLES20.GL_BLEND)
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        }




        if (state.get() in listOf(State.PLAYING, State.COMPLETE)) {
            if (USE_BETTER_LOTTIE_GL) {

                val curFrame =
                    (progress.get() * (betterLottieGL?.drawable?.maxFrame ?: 0f)).roundToInt()
//                Log.d("dwww3", "curFrame $curFrame animatingFrame $animatingFrame")
                if (animatingFrame != curFrame) {
//                    Log.d("dwww3", "curFrame $curFrame animatingFrame $animatingFrame")
                    val isPop = true
                    val tmpBmp = betterLottieGL?.getBitmap(progress.get(), isPop)?.apply {
                        isNewFrame = true
                        animatingFrame = curFrame
                        storyDrawer?.updateTex(this)
                    }
                    if (isPop) {
                        tmpBmp?.recycle()
                    }
                }
            } else {
                lottieDrawable?.let {
                    val curFrame = (progress.get() * it.maxFrame).roundToInt()
//                    Log.d("dwww3", "curFrame $curFrame animatingFrame $animatingFrame")
                    if (animatingFrame != curFrame) {
//                    if (animatingProgress != progress.get()) {
                        isNewFrame = true
                        animatingFrame = curFrame
                        animatingProgress = progress.get()
                        drawableToBitmap(it)
                        storyDrawer?.updateTex(bitmap)
                    }
                }
            }
//            state.set(State.PLAYING)
//            if (state.get() == State.SEEK)
//                state.set(prevState.get())
        } else if (forceUpdateTex) {
            //forceUpdateTex when surfaceChange while state is Pause
            if (USE_BETTER_LOTTIE_GL) {
                val curFrame =
                    (progress.get() * (betterLottieGL?.drawable?.maxFrame ?: 0f)).roundToInt()
                val isPop = true
                val tmpBmp = betterLottieGL?.getBitmap(progress.get(), isPop)?.apply {
                    isNewFrame = true
                    animatingFrame = curFrame
                    storyDrawer?.updateTex(this)
                }
                if (isPop) {
                    tmpBmp?.recycle()
                }
            } else {
                lottieDrawable?.let {
                    animatingProgress = progress.get()
                    drawableToBitmap(it)
                    storyDrawer?.updateTex(bitmap)
                }
            }
            forceUpdateTex = false
        }


        storyDrawer?.drawFrame()

        mFilter?.let {
            fbo?.enable()
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            mFilter?.glDraw(
                mFilterFramebufferObject.texName,
                this.mGLCubeBuffer,
                this.mGLTextureBuffer
            )

        }

        if (state.get() in listOf(State.PLAYING, State.COMPLETE)) {
            if (isNewFrame)
                mCallback?.onProgress(progress.get())
        }

        if (state.get() == State.PAUSE || state.get() == State.COMPLETE) try {
            Thread.sleep(66)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val diff = System.currentTimeMillis() - startTime
        if (diff.toFloat() >= 1000f / targetFPS) {
//            if (BuildConfig.DEBUG) Log.d("ttttt", "time exe ${diff}, fps = ${1000 / diff}")
        } else {
            try {
                val sleepTime: Long = ((1000f / targetFPS - diff) * 0.9f).toLong()
                Thread.sleep(sleepTime)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }


    fun release() {
        mFilter?.release()
        backgroundDrawer.release()
        storyDrawer?.release()
        progress.set(0f);
        animatingProgress = -1f
        animatingFrame = -1

        stopBetterLottieGL()
    }

    fun drawExport(
        renderFrame: Int,
        drawable: LottieDrawable,
        fbo: GlFramebufferObject
    ) {

        if (isNewFilter) {
            mFilter?.let {
                it.setup(fbo)
                it.setFrameSize(fbo.width, fbo.height)
                it.rotationSelf()
            }
            isNewFilter = false
        }

        mFilter?.let {
            mFilterFramebufferObject.enable()
            GLES20.glViewport(0, 0, mFilterFramebufferObject.width, mFilterFramebufferObject.height)
        }
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        backgroundDrawer.let {
            it.drawFrame()
            GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        }



        if (state.get() == State.PLAYING) {
            drawableToBitmap(drawable, renderFrame)
        }

        storyDrawer?.updateTex(bitmap)
        GLES20.glViewport(0, 0, width, height)
        storyDrawer?.drawFrame()
//        val bmp = convertToBitmap(width, height);
//        bmp?.recycle()

        mFilter?.let {
            fbo.enable()
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            mFilter?.glDraw(
                mFilterFramebufferObject.texName,
                this.mGLCubeBuffer,
                this.mGLTextureBuffer
            )

        }

        mCallback?.onProgress(1f * (renderFrame + 1) / ((lottieDrawable?.maxFrame ?: 1f) + 1))

    }


    private fun convertToBitmap(mOutputWidth: Int, mOutputHeight: Int): Bitmap? {
        var mBitmap: Bitmap?
        val sdkString = Build.VERSION.RELEASE
        val ib = IntBuffer.wrap(IntArray(mOutputWidth * mOutputHeight))
        ib.position(0)
        try {
            if (sdkString == "2.3.6") {
                GLES20.glPixelStorei(3333, 4)
            }
            GLES20.glReadPixels(0, 0, mOutputWidth, mOutputHeight, 6408, 5121, ib)
        } catch (e: java.lang.Exception) {
            print(e.message)
            mBitmap = null
        } catch (e2: Throwable) {
            print(e2.message)
            mBitmap = null
        }
        try {
            mBitmap =
                Bitmap.createBitmap(mOutputWidth, mOutputHeight, Bitmap.Config.ARGB_8888)
            mBitmap.copyPixelsFromBuffer(ib)
        } catch (th: Throwable) {
            mBitmap = null
        }
        ib.clear()
        return mBitmap
    }

    private fun drawableToBitmap(lottieDrawable: LottieDrawable, frameRender: Int = 0) {
        bitmap?.let { bmp ->
            bmp.eraseColor(Color.TRANSPARENT)

            if (isExport) {
                lottieDrawable.frame = frameRender
            } else {
                lottieDrawable.progress = animatingProgress
            }

            lottieDrawable.draw(canvas, matrix)
        }
    }

    fun setState(newState: State) {
        prevState.set(state.get())
        state.set(newState)
    }

    fun getState(): State {
        return state.get()
    }

    fun updateBg(bg: IStoryBackgroundComposer) {
        backgroundDrawer.update(bg)
    }

    fun resetBetterLottie() {
        betterLottieGL?.reset()
    }

    interface StoryRenderListener {
        fun onProgress(progress: Float)
    }

}