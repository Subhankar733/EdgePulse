package com.edgepulse.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Audio Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Audio Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    // রিয়েল-টাইম স্টেট ট্র্যাকিং
    private var currentSpeed by mutableStateOf(2.5f)
    private var currentThickness by mutableStateOf(12f)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        speed = currentSpeed,
                        thickness = currentThickness,
                        onSpeedChange = { 
                            currentSpeed = it
                            sendUpdateToService()
                        },
                        onThicknessChange = { 
                            currentThickness = it
                            sendUpdateToService()
                        },
                        onOverlayClick = { checkAndRequestOverlayPermission() },
                        onAudioClick = { requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                        onStartService = { startEdgeService() },
                        onStopService = { stopEdgeService() }
                    )
                }
            }
        }
    }

    private fun checkAndRequestOverlayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            Toast.makeText(this, "Overlay Permission Already Granted!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startEdgeService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant Overlay permission first!", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, EdgeLightingService::class.java).apply {
            putExtra("EXTRA_SPEED", currentSpeed)
            putExtra("EXTRA_THICKNESS", currentThickness)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "EdgePulse Started!", Toast.LENGTH_SHORT).show()
    }

    private fun stopEdgeService() {
        val intent = Intent(this, EdgeLightingService::class.java)
        stopService(intent)
        Toast.makeText(this, "EdgePulse Stopped!", Toast.LENGTH_SHORT).show()
    }

    // স্লাইডার নাড়ালে সাথে সাথে সার্ভিসকে নতুন ডাটা পুশ করার মেথড
    private fun sendUpdateToService() {
        if (Settings.canDrawOverlays(this)) {
            val intent = Intent(this, EdgeLightingService::class.java).apply {
                putExtra("EXTRA_SPEED", currentSpeed)
                putExtra("EXTRA_THICKNESS", currentThickness)
            }
            startService(intent)
        }
    }
}

@Composable
fun MainScreen(
    speed: Float,
    thickness: Float,
    onSpeedChange: (Float) -> Unit,
    onThicknessChange: (Float) -> Unit,
    onOverlayClick: () -> Unit,
    onAudioClick: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "EdgePulse Premium", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onOverlayClick, modifier = Modifier.fillMaxWidth()) {
            Text("1. Allow Draw Over Apps")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = onAudioClick, modifier = Modifier.fillMaxWidth()) {
            Text("2. Allow Audio Record")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // গতি নিয়ন্ত্রক স্লাইডার (Speed)
        Text(text = "Lighting Speed: ${String.format("%.1f", speed)}")
        Slider(
            value = speed,
            onValueChange = onSpeedChange,
            valueRange = 0.5f..10.0f,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // ঘনত্ব নিয়ন্ত্রক স্লাইডার (Thickness)
        Text(text = "Border Thickness: ${thickness.toInt()}px")
        Slider(
            value = thickness,
            onValueChange = onThicknessChange,
            valueRange = 4f..40f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = onStartService, modifier = Modifier.fillMaxWidth()) {
            Text("Start Edge Lighting")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onStopService, 
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Text("Stop Edge Lighting")
        }
    }
}
