package com.example.fakelocation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.fakelocation.ui.theme.FakeLocationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FakeLocationTheme {
                FakeLocationScreen()
            }
        }
    }
}

@Composable
fun FakeLocationScreen() {
    val context = LocalContext.current
    var lat by remember { mutableStateOf("5.3967307") }
    var lng by remember { mutableStateOf("-3.9913979") }
    var isRunning by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Service arrêté") }

    // Launcher pour demander la permission de localisation
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        statusMessage = if (granted) "Permission accordée ✓" else "Permission refusée ✗"
    }

    // Vérifie si la permission est déjà accordée
    val hasPermission = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📍 Fake GPS",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = lat,
                onValueChange = { lat = it },
                label = { Text("Latitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = lng,
                onValueChange = { lng = it },
                label = { Text("Longitude") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Bouton permission si nécessaire
            if (!hasPermission) {
                OutlinedButton(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Accorder la permission GPS")
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = {
                    val latDouble = lat.toDoubleOrNull()
                    val lngDouble = lng.toDoubleOrNull()

                    if (latDouble == null || lngDouble == null) {
                        statusMessage = "❌ Coordonnées invalides"
                        return@Button
                    }
                    if (latDouble !in -90.0..90.0 || lngDouble !in -180.0..180.0) {
                        statusMessage = "❌ Coordonnées hors limites"
                        return@Button
                    }

                    val intent = Intent(context, MockLocationService::class.java).apply {
                        putExtra(MockLocationService.EXTRA_LAT, latDouble)
                        putExtra(MockLocationService.EXTRA_LNG, lngDouble)
                    }
                    context.startForegroundService(intent)
                    isRunning = true
                    statusMessage = "✅ Mock GPS actif sur $latDouble, $lngDouble"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isRunning
            ) {
                Text("Démarrer")
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = {
                    context.stopService(Intent(context, MockLocationService::class.java))
                    isRunning = false
                    statusMessage = "🛑 Service arrêté"
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isRunning
            ) {
                Text("Arrêter")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}