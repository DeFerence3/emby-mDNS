package com.deference.mdns

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.deference.mdns.ui.theme.MDNSTheme

class MainActivity : ComponentActivity() {
	
	lateinit var startBroadcasting: () -> Unit
	lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
		context = this
		startBroadcasting = {
			MdnsForegroundService.start(this)
		}
        enableEdgeToEdge()
        setContent {
			var showOpenSettingsWhenPermissionDenied by remember { mutableStateOf(false) }
			var deniedPermissions by remember { mutableStateOf("") }

			val permissionsLauncher =
				rememberLauncherForActivityResult(
					ActivityResultContracts.RequestMultiplePermissions()
				) { permissions ->
					val allGranted = permissions.entries.all { it.value }
					if (!allGranted) {
						deniedPermissions = permissions.filter { !it.value }.keys.joinToString(", ") {
							it.substringAfterLast(".")
						}
						showOpenSettingsWhenPermissionDenied = true
					}
					// Start broadcasting regardless, the service will handle or fail gracefully
					// if critical permissions are missing.
					startBroadcasting()
				}

			LaunchedEffect(Unit) {
				val permissionsToRequest = mutableListOf<String>()
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
					if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
						permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
					}
					if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
						permissionsToRequest.add(Manifest.permission.NEARBY_WIFI_DEVICES)
					}
				}
				
				// ACCESS_LOCAL_NETWORK is required for Android 17 (API 37)+
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CINNAMON_BUN) {
					val accessLocalNetwork = "android.permission.ACCESS_LOCAL_NETWORK"
					if (ContextCompat.checkSelfPermission(context, accessLocalNetwork) != PackageManager.PERMISSION_GRANTED) {
						permissionsToRequest.add(accessLocalNetwork)
					}
				}

				if (permissionsToRequest.isNotEmpty()) {
					permissionsLauncher.launch(permissionsToRequest.toTypedArray())
				} else {
					startBroadcasting()
				}
			}
			
            MDNSTheme {
				if (showOpenSettingsWhenPermissionDenied) {
					PermissionRequestDialog(
						onDenied = { showOpenSettingsWhenPermissionDenied = false },
						onGrand = {
							showOpenSettingsWhenPermissionDenied = false
							val intent = Intent(
								Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
								Uri.fromParts("package", this.packageName, null)
							)
							this.startActivity(intent, null)
						},
						permissions = deniedPermissions
					)
				}
				
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    MdnsControls(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
	
	@Composable
	private fun MdnsControls(modifier: Modifier = Modifier) {
		Column(
			modifier = modifier,
			horizontalAlignment = Alignment.CenterHorizontally,
			verticalArrangement = Arrangement.spacedBy(
				space = 16.dp,
				alignment = Alignment.CenterVertically
			)
		) {
			Text("mDNS service: home-emby._emby._tcp")
			
			Text("Port: 8096")
			
			Button(
				onClick = {
					startBroadcasting()
				}
			) {
				Text("Start broadcasting")
			}
			
			OutlinedButton(
				onClick = {
					MdnsForegroundService.stop(context)
				}
			) {
				Text("Stop broadcasting")
			}
		}
	}
	
	@Composable
	fun PermissionRequestDialog(
		onDenied: () -> Unit,
		onGrand: () -> Unit,
		permissions: String
	) {
		AlertDialog(
			modifier = Modifier,
			onDismissRequest = onDenied,
			title = { Text(text = "Permission Request") },
			text = { Text(text = "The following permission is required to function this app appropriately\n\n$permissions\n\nPlease grand those in settings.") },
			confirmButton = {
				Button(
					onClick = onGrand
				) {
					Text(text = "Grand")
				}
			},
			dismissButton = {
				TextButton(
					onClick = onDenied
				) {
					Text(text = "Deny")
				}
			}
		)
	}
}