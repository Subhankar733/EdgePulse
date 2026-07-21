package com.edgepulse.app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast

class MainActivity : Activity() {

    private val PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions()

        val btnStart = findViewById<Button>(R.id.btnStart)
        val btnStop = findViewById<Button>(R.id.btnStop)
        val seekSpeed = findViewById<SeekBar>(R.id.seekSpeed)
        val seekThickness = findViewById<SeekBar>(R.id.seekThickness)

        btnStart?.setOnClickListener {
            if (checkPermissions()) {
                val speedVal = if (seekSpeed != null) (seekSpeed.progress + 1).toFloat() else 2.5f
                val thicknessVal = if (seekThickness != null) (seekThickness.progress + 5).toFloat() else 12f

                val intent = Intent(this, EdgeLightingService::class.java).apply {
                    putExtra("EXTRA_SPEED", speedVal)
                    putExtra("EXTRA_THICKNESS", thicknessVal)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(intent)
                } else {
                    startService(intent)
                }
                Toast.makeText(this, "EdgePulse Started", Toast.LENGTH_SHORT).show()
            }
        }

        btnStop?.setOnClickListener {
            stopService(Intent(this, EdgeLightingService::class.java))
            Toast.makeText(this, "EdgePulse Stopped", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions(): Boolean {
        var granted = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
                granted = false
            }
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                granted = false
            }
        }
        return granted
    }
}
