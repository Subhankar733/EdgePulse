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

    // প্রিমিয়াম রেইনবো কালার প্যালেট (RGB)
    private val rainbowColors = intArrayOf(
        Color.parseColor("#FF0055"), // ম্যাজেন্টা
        Color.parseColor("#00E5FF"), // নিয়ন সায়ান
        Color.parseColor("#00FF66"), // নিয়ন গ্রিন
        Color.parseColor("#FFCC00"), // গোল্ডেন ইয়োলো
        Color.parseColor("#9900FF"), // পার্পল
        Color.parseColor("#FF0055")  // লুপ কমপ্লিট করার জন্য আবার ম্যাজেন্টা
    )
    
    private var gradientShader: SweepGradient? = null
    private val gradientMatrix = Matrix()

    fun updateVisualizer(bytes: ByteArray) {
        this.audioData = bytes
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        // স্ক্রিনের সেন্টারে গ্রাডিয়েন্ট শেডার তৈরি করা
        if (gradientShader == null) {
            gradientShader = SweepGradient(w / 2f, h / 2f, rainbowColors, null)
            paint.shader = gradientShader
        }

        // গ্রাডিয়েন্টটিকে অনবরত ঘোরানো (স্মুথ আরজিবি এফেক্ট)
        rotationAngle = (rotationAngle + 2.5f) % 360f
        gradientMatrix.setRotate(rotationAngle, w / 2f, h / 2f)
        gradientShader?.setLocalMatrix(gradientMatrix)

        // অডিও বিট ট্র্যাকিং ও ব্রিদিং ফলব্যাক লজিক
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
            animPhase += 0.04f
            amplitude = (sin(animPhase) * 12f) + 12f
        }

        // প্রিমিয়াম লুকের জন্য বর্ডারের থিকনেস (১২px থেকে শুরু)
        val stroke = 12f + (amplitude * 0.5f).coerceAtMost(20f)
        paint.strokeWidth = stroke

        // বর্ডার যেন ডিসপ্লের নিচে কেটে না যায় তার জন্য ইনসেট
        val inset = stroke / 2f + 2f
        val rect = RectF(inset, inset, w - inset, h - inset)

        // আধুনিক ফোনের ডিসপ্লের সাথে ম্যাচ করে রাউন্ডেড কর্নার (Curved Corners) দেওয়া হলো
        val cornerRadius = 75f 
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint)

        // ৬০ FPS-এ স্মুথ অ্যানিমেশন লুপ সচল রাখা
        postInvalidateDelayed(16)
    }
}
