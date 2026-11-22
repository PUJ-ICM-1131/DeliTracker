package com.delitracker.app

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
    private var notificationTarget by androidx.compose.runtime.mutableStateOf<com.delitracker.app.notifications.NotificationTarget?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationTarget = com.delitracker.app.notifications.DeliNotifications.target(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationTarget = com.delitracker.app.notifications.DeliNotifications.target(intent)
        enableEdgeToEdge()
        setContent {
            DeliTrackerRoot(notificationTarget) {
                notificationTarget = null
                intent.removeExtra("notification_account")
                intent.removeExtra("notification_restaurant")
            }
        }
    }
}

@Composable
fun DeliTrackerRoot(notificationTarget: com.delitracker.app.notifications.NotificationTarget? = null, onHandled: () -> Unit = {}) {
    DeliTrackerTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            DeliTrackerApp(notificationTarget, onHandled)
        }
    }
}
