package com.edgepulse.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.media.audiofx.Visualizer
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat

class EdgeLightingService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var edgeVisualizerView: EdgeVisualizerView? = null
    private var visualizer: Visualizer? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        // ফোরগ্রাউন্ড সার্ভিসের নোটিফিকেশন চ্যানেল
        createNotificationChannel()
        startForeground(1, createNotification())

        // ভিউ ইন্টিগ্রেশন সহ ওভারলে উইন্ডো চালু
        showEdgeLightingOverlay()

        // অ디오 ট্র্যাকিং শুরু
        try {
            initVisualizer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "edge_lighting_channel",
                "Edge Lighting Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "edge_lighting_channel")
            .setContentTitle("EdgePulse Active")
            .setContentText("Listening to audio and rendering effects...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showEdgeLightingOverlay() {
        overlayView = FrameLayout(this)
        
        // আমাদের কাস্টম ভিজ্যুয়ালাইজার ভিউকে ওভারলে লেআউটে যুক্ত করা হচ্ছে
        edgeVisualizerView = EdgeVisualizerView(this)
        overlayView?.addView(edgeVisualizerView)
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY 
            else 
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or 
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager?.addView(overlayView, layoutParams)
    }

    private fun initVisualizer() {
        // Audio Session 0 দিয়ে গ্লোবাল অডিও স্ট্রিম ক্যাপচার
        visualizer = Visualizer(0).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                    // সংগৃহীত অডিও ডেটা সরাসরি কাস্টম ভিউতে পুশ করা হচ্ছে
                    waveform?.let {
                        edgeVisualizerView?.updateVisualizer(it)
                    }
                }

                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    // ফ্রিকোয়েন্সি অ্যানিমেশনের প্রয়োজন হলে ভবিষ্যতে এখানে কাজ করা যাবে
                }
            }, Visualizer.getMaxCaptureRate() / 2, true, true)
            enabled = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (overlayView != null) {
            windowManager?.removeView(overlayView)
        }
    }
}
