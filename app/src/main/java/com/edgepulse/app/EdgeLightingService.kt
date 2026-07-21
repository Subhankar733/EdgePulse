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
        createNotificationChannel()
        startForeground(1, createNotification())
        showEdgeLightingOverlay()

        try {
            initVisualizer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val speed = it.getFloatExtra("EXTRA_SPEED", 2.5f)
            val thickness = it.getFloatExtra("EXTRA_THICKNESS", 12f)
            edgeVisualizerView?.updateConfig(speed, thickness)
        }
        return START_STICKY
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
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, // ফুল স্ক্রিন নোচ কভারেজ
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            // ক্যামেরা নোচের ওপর পর্যন্ত লাইট ছড়িয়ে দেওয়ার জন্য অ্যান্ড্রয়েড ৯+ ফ্ল্যাগ
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        windowManager?.addView(overlayView, layoutParams)
    }

    private fun initVisualizer() {
        visualizer = Visualizer(0).apply {
            captureSize = Visualizer.getCaptureSizeRange()[1]
            setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {}
                override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                    fft?.let {
                        edgeVisualizerView?.updateFft(it)
                    }
                }
            }, Visualizer.getMaxCaptureRate(), true, true)
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
