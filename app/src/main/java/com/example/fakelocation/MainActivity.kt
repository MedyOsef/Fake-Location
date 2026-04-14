package com.example.fakelocation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.fakelocation.ui.theme.FakeLocationTheme

data class SavedLocation(val name: String, val lat: Double, val lng: Double)

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

    // État pour les positions enregistrées
    val savedLocations = remember {
        mutableStateListOf<SavedLocation>().apply {
            addAll(getLocations(context))
        }
    }
    var showSaveDialog by remember { mutableStateOf(false) }
    var newLocationName by remember { mutableStateOf("") }

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

    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Enregistrer la position") },
            text = {
                OutlinedTextField(
                    value = newLocationName,
                    onValueChange = { newLocationName = it },
                    label = { Text("Nom de l'emplacement") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val latDouble = lat.toDoubleOrNull()
                        val lngDouble = lng.toDoubleOrNull()
                        if (newLocationName.isNotBlank() && latDouble != null && lngDouble != null) {
                            val newLoc = SavedLocation(newLocationName, latDouble, lngDouble)
                            savedLocations.add(newLoc)
                            saveLocations(context, savedLocations)
                            showSaveDialog = false
                            newLocationName = ""
                        }
                    }
                ) {
                    Text("Enregistrer")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📍 Fake GPS",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                    modifier = Modifier.weight(1f),
                    enabled = !isRunning
                ) {
                    Text("Démarrer")
                }

                OutlinedButton(
                    onClick = {
                        context.stopService(Intent(context, MockLocationService::class.java))
                        isRunning = false
                        statusMessage = "🛑 Service arrêté"
                    },
                    modifier = Modifier.weight(1f),
                    enabled = isRunning
                ) {
                    Text("Arrêter")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { showSaveDialog = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = lat.isNotEmpty() && lng.isNotEmpty()
            ) {
                Icon(Icons.Default.Star, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Enregistrer cette position")
            }

            // Bouton permission si nécessaire
            if (!hasPermission) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Text("Accorder la permission GPS")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = statusMessage,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Positions enregistrées",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Start),
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(savedLocations) { location ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                lat = location.lat.toString()
                                lng = location.lng.toString()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = location.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "${location.lat}, ${location.lng}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(onClick = {
                                savedLocations.remove(location)
                                saveLocations(context, savedLocations)
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Supprimer")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun saveLocations(context: Context, locations: List<SavedLocation>) {
    val sharedPref = context.getSharedPreferences("saved_locations", Context.MODE_PRIVATE)
    val encoded = locations.joinToString(";") { "${it.name}|${it.lat}|${it.lng}" }
    sharedPref.edit().putString("locations_list", encoded).apply()
}

private fun getLocations(context: Context): List<SavedLocation> {
    val sharedPref = context.getSharedPreferences("saved_locations", Context.MODE_PRIVATE)
    val encoded = sharedPref.getString("locations_list", "") ?: ""
    if (encoded.isEmpty()) return emptyList()
    return encoded.split(";").mapNotNull {
        val parts = it.split("|")
        if (parts.size == 3) {
            try {
                SavedLocation(parts[0], parts[1].toDouble(), parts[2].toDouble())
            } catch (e: Exception) {
                null
            }
        } else null
    }
}
