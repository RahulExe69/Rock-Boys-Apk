package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.network.NetworkMonitor
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.webview.GameWebView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var isWebViewLoading = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        splashScreen.setKeepOnScreenCondition {
            isWebViewLoading
        }
        
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                GameWebView(
                    targetUrl = "https://rockboys.netlify.app",
                    onExitRequested = {
                        finish() // Properly tear down and exit the app
                    },
                    onPageLoaded = {
                        isWebViewLoading = false
                    }
                )
            }
        }
    }
}

