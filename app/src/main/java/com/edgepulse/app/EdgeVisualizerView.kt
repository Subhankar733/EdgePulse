package com.edgepulse.app

import android.content.Context
import android.graphics.*
import android.view.View
import kotlin.math.sin
import kotlin.math.sqrt

class EdgeVisualizerView(context: Context) : View(context) {
    // ১. মেইন সলিড নিয়ন লাইনের পেইন্ট
    private val paintCore = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    // ২. পেছনের চওড়া ব্লার নিয়ন গ্লো এফেক্টের পেইন্ট
    private val paintGlow = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var fftData: ByteArray? = null
    private var rotationAngle = 0f
    private var animPhase = 0f
    
    // Muviz Edge এর মতো আল্ট্রা-স্মুথ এফেক্টের জন্য আলাদা ফ্রিকোয়েন্সি ট্র্যাকিং
    private var smoothedBass = 0f
    private var smoothedTreble = 0f

    private var currentSpeed = 2.5f
    private var baseThickness = 12f

    // Muviz Edge এর সিগনেচার নিয়ন কালার প্যালেট
    private val premiumColors = intArrayOf(
        Color.parseColor("#FF0055"), // সাইবার পাঙ্ক পিঙ্ক
        Color.parseColor("#00E5FF"), // ইলেকট্রিক সায়ান
        Color.parseColor("#7209B7"), // ডিপ পার্পল
        Color.parseColor("#00FF66"), // নিয়ন গ্রিন
        Color.parseColor("#FF0055")
    )
    
    private var gradientShader: SweepGradient? = null
    private val gradientMatrix = Matrix()

    init {
        // নিয়ন ব্লার এফেক্ট নিখুঁতভাবে রেন্ডার করার জন্য সফটওয়্যার লেয়ার অ্যাক্টিভেট করা হলো
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        paintGlow.maskFilter = BlurMaskFilter(25f, BlurMaskFilter.Blur.NORMAL)
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
        var trebleMagnitude = 0f
        val fft = fftData

        if (fft != null && fft.isNotEmpty()) {
            // ১. বেস ফ্রিকোয়েন্সি ক্যালকুলেশন (বিন ২ থেকে ১৪)
            var bassSum = 0f
            var bassCount = 0
            for (i in 2..14 step 2) {
                if (i + 1 < fft.size) {
                    val r = fft[i].toFloat()
                    val im = fft[i + 1].toFloat()
                    bassSum += sqrt(r * r + im * im)
                    bassCount++
                }
            }
            if (bassCount > 0) bassMagnitude = bassSum / bassCount

            // ২. ট্রিবল/হাই ফ্রিকোয়েন্সি ক্যালকুলেশন (স্পেকট্রামের শেষের দিকের ৫০% - ৯০% বিন)
            var trebleSum = 0f
            var trebleCount = 0
            val startTreble = (fft.size * 0.5).toInt()
            val endTreble = (fft.size * 0.9).toInt()
            for (i in startTreble..endTreble step 2) {
                if (i + 1 < fft.size) {
                    val r = fft[i].toFloat()
                    val im = fft[i + 1].toFloat()
                    trebleSum += sqrt(r * r + im * im)
                    trebleCount++
                }
            }
            if (trebleCount > 0) trebleMagnitude = trebleSum / trebleCount
        } else {
            // নো-মিউজিক মোডে আইডল সাইন-ওয়েভ অ্যানিমেশন
            animPhase += (currentSpeed * 0.012f).coerceAtLeast(0.005f)
            bassMagnitude = (sin(animPhase) * 6f) + 6f
            trebleMagnitude = (sin(animPhase * 1.5f) * 4f) + 4f
        }

        // এক্সপোনেনশিয়াল স্মুথিং ফিল্টার (যাতে ট্রানজিশনে কোনো ফ্রেম ড্রপ বা ঝটকা না লাগে)
        smoothedBass = smoothedBass * 0.8f + bassMagnitude * 0.2f
        smoothedTreble = smoothedTreble * 0.85f + trebleMagnitude * 0.15f

        // ট্রিবল এবং বেসের যৌথ শক্তিতে আরজিবি স্পিনের ডাইনামিক স্পিড কন্ট্রোল
        val speedFactor = 1f + (smoothedBass / 15f).coerceAtMost(2.0f) + (smoothedTreble / 25f).coerceAtMost(1.0f)
        rotationAngle = (rotationAngle + (currentSpeed * speedFactor)) % 360f
        gradientMatrix.setRotate(rotationAngle, w / 2f, h / 2f)
        gradientShader?.setLocalMatrix(gradientMatrix)

        // ডাইনামিক সাইজ ডিস্ট্রিবিউশন
        val coreStroke = baseThickness + (smoothedBass * 0.6f).coerceAtMost(25f)
        val glowStroke = coreStroke + 18f + (smoothedBass * 0.8f).coerceAtMost(35f)

        // আধুনিক ফ্ল্যাগশিপ ফোনের ডিসপ্লের কার্ভের সাথে ম্যাচ করার জন্য রেডিয়াস
        val cornerRadius = 85f 

        // স্তর ১: পেছনের নিয়ন গ্লো লেয়ার ড্র করা (বেসের তালে তালে এর উজ্জ্বলতা আপ-ডাউন করবে)
        paintGlow.strokeWidth = glowStroke
        paintGlow.alpha = (120 + (smoothedBass * 5f).toInt()).coerceIn(100, 255)
        val insetGlow = glowStroke / 2f + 2f
        val rectGlow = RectF(insetGlow, insetGlow, w - insetGlow, h - insetGlow)
        canvas.drawRoundRect(rectGlow, cornerRadius, cornerRadius, paintGlow)

        // স্তর ২: সামনের মেইন সলিড নিয়ন কোর লাইন ড্র করা
        paintCore.strokeWidth = coreStroke
        paintCore.alpha = 255
        val insetCore = coreStroke / 2f + 2f
        val rectCore = RectF(insetCore, insetCore, w - insetCore, h - insetCore)
        canvas.drawRoundRect(rectCore, cornerRadius, cornerRadius, paintCore)

        postInvalidateDelayed(16)
    }
}
