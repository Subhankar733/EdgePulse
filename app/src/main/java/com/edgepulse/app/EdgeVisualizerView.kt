package com.edgepulse.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.abs

class EdgeVisualizerView(context: Context) : View(context) {

    private val paint = Paint().apply {
        color = Color.parseColor("#00E5FF") // নিয়ন সায়ান কালার (Neon Cyan)
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }

    private var audioData: ByteArray? = null

    // সার্ভিস থেকে অডিও ওয়েভফর্ম ডাটা পুশ করার মেথড
    fun updateVisualizer(bytes: ByteArray) {
        this.audioData = bytes
        invalidate() // ভিউটি রিফ্রেশ করে অন-ড্র (onDraw) কল করবে
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val data = audioData ?: return
        if (data.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        // অডিওর গড় অ্যামপ্লিচিউড (বিট এনার্জি) বের করা
        var amplitude = 0f
        var count = 0
        for (i in 0 until data.size step 4) {
            if (i < data.size) {
                amplitude += abs(data[i].toInt() - 128)
                count++
            }
        }
        if (count > 0) amplitude /= count

        // মিউজিকের বিটের তীব্রতার ওপর ভিত্তি করে বর্ডারের লাইটিং লাইনের মোটা-চিকন হওয়া নির্ধারণ
        paint.strokeWidth = 6f + (amplitude * 0.6f).coerceAtMost(30f)

        // ফোনের চারপাশের বর্ডারে এজ লাইটিং ড্র করা
        // ১. বাম পাশের বর্ডার
        canvas.drawLine(0f, 0f, 0f, h, paint)
        // ২. ওপরের বর্ডার
        canvas.drawLine(0f, 0f, w, 0f, paint)
        // ৩. ডান পাশের বর্ডার
        canvas.drawLine(w, 0f, w, h, paint)
        // ৪. নিচের বর্ডার
        canvas.drawLine(0f, h, w, h, paint)
    }
}
