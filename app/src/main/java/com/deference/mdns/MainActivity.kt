package com.deference.mdns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.deference.mdns.ui.theme.MDNSTheme

class MainActivity : ComponentActivity() {

    private lateinit var mdnBroadcaster: MdnsBroadcaster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mdnBroadcaster = MdnsBroadcaster(this)
        mdnBroadcaster.registerNginxService()
        enableEdgeToEdge()
        setContent {
            MDNSTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DNS broadcasting for port 8096",
                            modifier = Modifier
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        mdnBroadcaster.stopBroadcasting()
        super.onDestroy()
    }
}