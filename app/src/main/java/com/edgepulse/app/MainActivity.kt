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

    // অডিও রেকর্ড পারমিশন নেওয়ার জন্য লাঞ্চার
    private val requestAudioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(this, "Audio Permission Granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Audio Permission Denied!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        onOverlayClick = { checkAndRequestOverlayPermission() },
                        onAudioClick = { requestAudioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) },
                        onStartService = { startEdgeService() },
                        onStopService = { stopEdgeService() }
                    )
                }
            }
        }
    }

    // ড্র ওভার আদার অ্যাপস (Overlay) পারমিশন চেক ও রিকোয়েস্ট
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

    // এজ লাইটিং সার্ভিস চালু করা
    private fun startEdgeService() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant Overlay permission first!", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, EdgeLightingService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "EdgePulse Started!", Toast.LENGTH_SHORT).show()
    }

    // এজ লাইটিং সার্ভিস বন্ধ করা
    private fun stopEdgeService() {
        val intent = Intent(this, EdgeLightingService::class.java)
        stopService(intent)
        Toast.makeText(this, "EdgePulse Stopped!", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MainScreen(
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
        Text(
            text = "EdgePulse Control",
            style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        // পারমিশন বাটন সমূহ
        Button(onClick = onOverlayClick, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("1. Allow Draw Over Apps")
        }

        Button(onClick = onAudioClick, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Text("2. Allow Audio Record")
        }

        Spacer(modifier = Modifier.height(40.dp))

        // সার্ভিস কন্ট্রোল বাটন সমূহ
        Button(
            onClick = onStartService,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Start Edge Lighting", style = MaterialTheme.typography.bodyLarge)
        }

        Button(
            onClick = onStopService,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        ) {
            Text("Stop Edge Lighting", style = MaterialTheme.typography.bodyLarge)
        }
    }
}
