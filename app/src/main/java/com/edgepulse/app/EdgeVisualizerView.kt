package com.edgepulse.app

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class EdgeVisualizerView(context: Context) : View(context) {
    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var audioData: ByteArray? = null
    private var rotationAngle = 0f
    private var animPhase = 0f

    // ডাইনামিক কন্ট্রোল ভেরিয়েবল
    private var currentSpeed = 2.5f
    private var baseThickness = 12f

    private val rainbowColors = intArrayOf(
        Color.parseColor("#FF0055"),
        Color.parseColor("#00E5FF"),
        Color.parseColor("#00FF66"),
        Color.parseColor("#FFCC00"),
        Color.parseColor("#9900FF"),
        Color.parseColor("#FF0055")
    )
    
    private var gradientShader: SweepGradient? = null
    private val gradientMatrix = Matrix()

    fun updateVisualizer(bytes: ByteArray) {
        this.audioData = bytes
    }

    // UI থেকে স্পিড ও থিকনেস আপডেট করার মেথড
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
            gradientShader = SweepGradient(w / 2f, h / 2f, rainbowColors, null)
            paint.shader = gradientShader
        }

        // স্লাইডারের স্পিড অনুযায়ী রোটেশন হ্যান্ডেল করা
        rotationAngle = (rotationAngle + currentSpeed) % 360f
        gradientMatrix.setRotate(rotationAngle, w / 2f, h / 2f)
        gradientShader?.setLocalMatrix(gradientMatrix)

        var amplitude = 0f
        val data = audioData
        if (data != null && data.isNotEmpty()) {
            var count = 0
            for (i in 0 until data.size step 4) {
                amplitude += abs(data[i].toInt() - 128)
                count++
            }
            if (count > 0) amplitude /= count
        } else {
            animPhase += (currentSpeed * 0.015f).coerceAtLeast(0.01f)
            amplitude = (sin(animPhase) * 12f) + 12f
        }

        // স্লাইডারের থিকনেস অনুযায়ী বর্ডার সাইজ নির্ধারণ
        val stroke = baseThickness + (amplitude * 0.5f).coerceAtMost(20f)
        paint.strokeWidth = stroke

        val inset = stroke / 2f + 2f
        val rect = RectF(inset, inset, w - inset, h - inset)

        val cornerRadius = 75f 
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        postInvalidateDelayed(16)
    }
}
