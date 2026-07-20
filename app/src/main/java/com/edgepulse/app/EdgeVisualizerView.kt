package com.edgepulse.app

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class EdgeVisualizerView(context: Context) : View(context) {
    private val paint = Paint().apply {
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
            gradientShader = SweepGradient(w / 2f, h / 2f, rainbowColors, null)
            paint.shader = gradientShader
        }

        // বেসের জোর অনুযায়ী আরজিবি লাইটের ঘূর্ণন গতিও একটু ডাইনামিকালি বুস্ট হবে
        val speedFactor = 1f + (smoothedBass / 20f).coerceAtMost(2.5f)
        rotationAngle = (rotationAngle + (currentSpeed * speedFactor)) % 360f
        gradientMatrix.setRotate(rotationAngle, w / 2f, h / 2f)
        gradientShader?.setLocalMatrix(gradientMatrix)

        var bassAmplitude = 0f
        val fft = fftData

        if (fft != null && fft.isNotEmpty()) {
            var bassSum = 0f
            // FFT অ্যারের ২ থেকে ১২ নম্বর বিনগুলো মূলত সাব-বেস এবং মেইন বেস ফ্রিকোয়েন্সি রিড করে
            val startBin = 2
            val endBin = 12
            var count = 0
            
            for (i in startBin..endBin step 2) {
                if (i + 1 < fft.size) {
                    val r = fft[i].toFloat()
                    val im = fft[i + 1].toFloat()
                    // রিয়েল এবং ইমাজিনারি পার্ট থেকে ফ্রিকোয়েন্সির তীব্রতা (Magnitude) বের করা
                    bassSum += sqrt(r * r + im * im)
                    count++
                }
            }
            if (count > 0) {
                bassAmplitude = bassSum / count
            }
        } else {
            // গান না চললে ব্যাকগ্রাউন্ডে একটা স্মুথ অটো-পালস অ্যানিমেশন চলবে
            animPhase += (currentSpeed * 0.015f).coerceAtLeast(0.01f)
            bassAmplitude = (sin(animPhase) * 8f) + 8f
        }

        // লাইট যেন খুব বেশি লাফালাফি না করে, তাই লো-পাস ফিল্টার দিয়ে এফেক্টটাকে মাখনের মতো স্মুথ করা হলো
        smoothedBass = smoothedBass * 0.75f + bassAmplitude * 0.25f

        // বেসের ড্রপ অনুযায়ী বর্ডারের থিকনেস ডাইনামিকালি ফ্লাকচুয়েট করবে
        val stroke = baseThickness + (smoothedBass * 0.9f).coerceAtMost(40f)
        paint.strokeWidth = stroke

        val inset = stroke / 2f + 2f
        val rect = RectF(inset, inset, w - inset, h - inset)

        val cornerRadius = 75f 
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        postInvalidateDelayed(16)
    }
}
