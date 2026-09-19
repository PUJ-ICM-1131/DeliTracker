package com.delitracker.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.delitracker.app.nav.DeliTrackerApp
import com.delitracker.app.ui.theme.DeliTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeliTrackerRoot()
        }
    }
}

@Composable
fun DeliTrackerRoot() {
    DeliTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DeliTrackerApp()
        }
    }
}
