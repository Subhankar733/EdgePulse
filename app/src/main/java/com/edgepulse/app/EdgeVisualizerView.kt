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

    private var audioLevel = 0f
    private var smoothedLevel = 0f
    private var rotationAngle = 0f
    private var animPhase = 0f

    private var currentSpeed = 2.5f
    private var baseThickness = 12f

    private val premiumColors = intArrayOf(
        Color.parseColor("#FF0055"),
        Color.parseColor("#FF9900"),
        Color.parseColor("#00E5FF"),
        Color.parseColor("#7209B7"),
        Color.parseColor("#00FF66"),
        Color.parseColor("#FF0055")
    )

    private var gradientShader: SweepGradient? = null
    private val gradientMatrix = Matrix()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        paintGlow.maskFilter = BlurMaskFilter(28f, BlurMaskFilter.Blur.NORMAL)
    }

    fun updateWaveform(bytes: ByteArray) {
        var sum = 0.0
        for (i in bytes.indices) {
            val sample = (bytes[i].toInt() and 0xFF) - 128
            sum += (sample * sample).toDouble()
        }
        val rms = sqrt(sum / bytes.size)
        val norm = (rms / 35.0).toFloat().coerceIn(0f, 1f)
        this.audioLevel = norm
    }

    fun updateFft(bytes: ByteArray) {
        var bassSum = 0f
        var count = 0
        for (i in 2..16 step 2) {
            if (i + 1 < bytes.size) {
                val r = bytes[i].toFloat()
                val im = bytes[i + 1].toFloat()
                bassSum += sqrt(r * r + im * im)
                count++
            }
        }
        if (count > 0) {
            val bassNorm = (bassSum / count / 25f).coerceIn(0f, 1f)
            if (bassNorm > this.audioLevel) {
                this.audioLevel = bassNorm
            }
        }
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

        smoothedLevel = smoothedLevel * 0.7f + audioLevel * 0.3f

        var activePulse = smoothedLevel
        if (activePulse < 0.02f) {
            animPhase += 0.04f
            activePulse = (sin(animPhase) * 0.12f) + 0.12f
        }

        val spinSpeed = currentSpeed * (1f + activePulse * 3.5f)
        rotationAngle = (rotationAngle + spinSpeed) % 360f
        gradientMatrix.setRotate(rotationAngle, w / 2f, h / 2f)
        gradientShader?.setLocalMatrix(gradientMatrix)

        val pulseBoost = activePulse * 30f
        val coreStroke = baseThickness + pulseBoost
        val glowStroke = coreStroke + 18f + (activePulse * 28f)

        val cornerRadius = 90f

        paintGlow.strokeWidth = glowStroke
        paintGlow.alpha = (90 + (activePulse * 165f).toInt()).coerceIn(70, 255)
        val insetGlow = glowStroke / 2f
        val rectGlow = RectF(insetGlow, insetGlow, w - insetGlow, h - insetGlow)
        canvas.drawRoundRect(rectGlow, cornerRadius, cornerRadius, paintGlow)

        paintCore.strokeWidth = coreStroke
        paintCore.alpha = 255
        val insetCore = coreStroke / 2f
        val rectCore = RectF(insetCore, insetCore, w - insetCore, h - insetCore)
        canvas.drawRoundRect(rectCore, cornerRadius, cornerRadius, paintCore)

        postInvalidateDelayed(16)
    }
}
