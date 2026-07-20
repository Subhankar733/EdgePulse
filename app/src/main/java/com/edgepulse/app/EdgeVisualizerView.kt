package com.edgepulse.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.abs
import kotlin.math.sin

class EdgeVisualizerView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.parseColor("#00E5FF") // নিয়ন সায়ান কালার (Neon Cyan)
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var audioData: ByteArray? = null
    private var animPhase = 0f

    // সার্ভিস থেকে অডিও ডাটা পুশ করার মেথড
    fun updateVisualizer(bytes: ByteArray) {
        this.audioData = bytes
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        if (w == 0f || h == 0f) return

        var amplitude = 0f
        val data = audioData

        // যদি অডিও ডাটা সাকসেসফুলি পাওয়া যায়
        if (data != null && data.isNotEmpty()) {
            var count = 0
            for (i in 0 until data.size step 4) {
                amplitude += abs(data[i].toInt() - 128)
                count++
            }
            if (count > 0) amplitude /= count
        } else {
            // ফলব্যাক: সিস্টেম অডিও ব্লক করলে নিজে থেকেই একটি স্মুথ পালস/ব্রিদিং অ্যানিমেশন তৈরি করবে
            animPhase += 0.05f
            amplitude = (sin(animPhase) * 15f) + 15f // ০ থেকে ৩০ এর মধ্যে পালস করবে
        }

        // বিটের তীব্রতার ওপর ভিত্তি করে লাইনের মোটা-চিকন হওয়া নির্ধারণ (মিনিমাম ৮px)
        val stroke = 8f + (amplitude * 0.6f).coerceAtMost(25f)
        paint.strokeWidth = stroke

        // লাইন যেন বেজেলের নিচে না লুকায়, সেজন্য সামান্য ভেতরের দিকে ইনসেট করা হলো
        val inset = stroke / 2f + 4f
        val rect = RectF(inset, inset, w - inset, h - inset)
        
        // ফোনের চারপাশের বর্ডারে এজ লাইটিং ড্র করা
        canvas.drawRect(rect, paint)

        // অ্যানিমেশন লুপ সচল রাখতে প্রতি ১৬ মিলিসেকেন্ডে (~60 FPS) ভিউ রিফ্রেশ করা
        postInvalidateDelayed(16)
    }
}
