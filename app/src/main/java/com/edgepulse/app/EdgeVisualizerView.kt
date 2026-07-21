package com.edgepulse.app

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin
import kotlin.math.sqrt

class EdgeVisualizerView(context: Context) : View(context) {

    private val paintCore = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private val paintGlow = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var fftData: ByteArray? = null
    private var rotationAngle = 0f
    private var animPhase = 0f

    private var smoothedBass = 0f
    private var currentSpeed = 2.5f
    private var baseThickness = 12f

    private val premiumColors = intArrayOf(
        Color.parseColor("#FF0055"),
        Color.parseColor("#00E5FF"),
        Color.parseColor("#7209B7"),
        Color.parseColor("#00FF66"),
        Color.parseColor("#FF0055")
    )

    private var gradientShader: SweepGradient? = null
    private val gradientMatrix = Matrix()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        paintGlow.maskFilter = BlurMaskFilter(20f, BlurMaskFilter.Blur.NORMAL)
    }

    fun updateFft(bytes: ByteArray) {
        this.fftData = bytes
    }

    fun updateConfig(speed: Float, thickness: Float) {
        this.currentSpeed = speed
        this.baseThickness = thickness
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        if (gradientShader == null) {
            gradientShader = SweepGradient(w / 2f, h / 2f, premiumColors, null)
            paintCore.shader = gradientShader
            paintGlow.shader = gradientShader
        }

        var bassMagnitude = 0f
        var isAudioActive = false
        val fft = fftData

        if (fft != null && fft.isNotEmpty()) {
            var bassSum = 0f
            var bassCount = 0
            // ২ থেকে ১৬ নম্বর ফ্রিকোয়েন্সি বিন (বেস অ্যান্ড সাব-বেস)
            for (i in 2..16 step 2) {
                if (i + 1 < fft.size) {
                    val r = fft[i].toFloat()
                    val im = fft[i + 1].toFloat()
                    val mag = sqrt(r * r + im * im)
                    bassSum += mag
                    bassCount++
                }
            }
            if (bassCount > 0) {
                val rawBass = bassSum / bassCount
                if (rawBass > 1f) {
                    isAudioActive = true
                    // ৪x গেইন বুস্ট - যাতে বিটগুলো সচ্ছলভাবে রিয়্যাক্ট করে
                    bassMagnitude = rawBass * 4.0f 
                }
            }
        }

        if (!isAudioActive) {
            animPhase += (currentSpeed * 0.012f).coerceAtLeast(0.005f)
            bassMagnitude = (sin(animPhase) * 5f) + 5f
        }

        smoothedBass = smoothedBass * 0.7f + bassMagnitude * 0.3f

        val speedFactor = 1f + (smoothedBass / 20f).coerceAtMost(3.0f)
        rotationAngle = (rotationAngle + (currentSpeed * speedFactor)) % 360f
        gradientMatrix.setRotate(rotationAngle, w / 2f, h / 2f)
        gradientShader?.setLocalMatrix(gradientMatrix)

        val coreStroke = baseThickness + (smoothedBass * 0.8f).coerceAtMost(35f)
        val glowStroke = coreStroke + 15f + (smoothedBass * 1.0f).coerceAtMost(40f)

        val cornerRadius = 85f

        // গ্লো লেয়ার
        paintGlow.strokeWidth = glowStroke
        paintGlow.alpha = (100 + (smoothedBass * 6f).toInt()).coerceIn(80, 255)
        val insetGlow = glowStroke / 2f
        val rectGlow = RectF(insetGlow, insetGlow, w - insetGlow, h - insetGlow)
        canvas.drawRoundRect(rectGlow, cornerRadius, cornerRadius, paintGlow)

        // মেইন সলিড কোর লেয়ার (একদম ডিসপ্লের আউটার এডজ স্পর্শ করে আঁকা)
        paintCore.strokeWidth = coreStroke
        paintCore.alpha = 255
        val insetCore = coreStroke / 2f
        val rectCore = RectF(insetCore, insetCore, w - insetCore, h - insetCore)
        canvas.drawRoundRect(rectCore, cornerRadius, cornerRadius, paintCore)

        postInvalidateDelayed(16)
    }
}
