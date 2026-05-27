package com.example.ui.webview

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.CyberBlack
import com.example.ui.theme.CyberDark
import com.example.ui.theme.CyberGray
import com.example.ui.theme.CyberLine
import com.example.ui.theme.ToxicGreen
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.LaserRed
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameWebView(
    targetUrl: String = "https://rockboys.netlify.app",
    onExitRequested: () -> Unit,
    onPageLoaded: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // WebView reference and states
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var isPageLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0f) }
    var hasError by remember { mutableStateOf(false) }
    var isScrollAtTop by remember { mutableStateOf(true) }
    var canGoBackState by remember { mutableStateOf(false) }
    var canGoForwardState by remember { mutableStateOf(false) }
    var currentUrlState by remember { mutableStateOf(targetUrl) }
    
    // Exit validation confirmation dialog
    var showExitDialog by remember { mutableStateOf(false) }

    // File selection callback for uploading files
    var fileChooserCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // File chooser launcher
    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val result = if (uri != null) arrayOf(uri) else null
        fileChooserCallback?.onReceiveValue(result)
        fileChooserCallback = null
    }

    // Intercept hardware Android back button
    BackHandler {
        val webView = webViewInstance
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            showExitDialog = true
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(CyberBlack)
        ) {
            // Raw Android WebView container
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        
                        // Prevent the classic default white flash before/while loading web content
                        setBackgroundColor(android.graphics.Color.parseColor("#07090E"))
                        
                        // Performance and features configurations
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.setSupportZoom(false)
                        settings.setBuiltInZoomControls(false)
                        settings.setDisplayZoomControls(false)
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        
                        // Download integration support
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                            try {
                                val request = DownloadManager.Request(Uri.parse(url)).apply {
                                    setMimeType(mimetype)
                                    addRequestHeader("User-Agent", userAgent)
                                    setDescription("Downloading game file...")
                                    setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))
                                }
                                val downloadManager = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                                downloadManager.enqueue(request)
                                Toast.makeText(ctx, "Starting download...", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(ctx, "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }

                        // Customized clients
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                isPageLoading = true
                                currentUrlState = url ?: targetUrl
                                canGoBackState = view?.canGoBack() ?: false
                                canGoForwardState = view?.canGoForward() ?: false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isPageLoading = false
                                canGoBackState = view?.canGoBack() ?: false
                                canGoForwardState = view?.canGoForward() ?: false
                                onPageLoaded()
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val url = request?.url?.toString() ?: return false
                                
                                // Support internal web links
                                if (url.contains("rockboys.netlify.app") || url.contains("localhost") || url.startsWith("file://")) {
                                    return false
                                }

                                // Socials & Communications app redirects
                                if (url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("sms:")) {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse(url))
                                        ctx.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Ignore fail
                                    }
                                    return true
                                }

                                if (url.contains("discord.com") || url.contains("youtube.com") || url.contains("twitter.com") || url.contains("facebook.com")) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        ctx.startActivity(intent)
                                    } catch (e: Exception) {
                                        return false // Load in WebView if system doesn't resolve
                                    }
                                    return true
                                }

                                return false
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    hasError = true
                                    isPageLoading = false
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                            }

                            // Dynamic pick files for uploading elements
                            override fun onShowFileChooser(
                                webView: WebView?,
                                filePathCallback: ValueCallback<Array<Uri>>?,
                                fileChooserParams: FileChooserParams?
                            ): Boolean {
                                fileChooserCallback?.onReceiveValue(null)
                                fileChooserCallback = filePathCallback
                                
                                val mimeType = fileChooserParams?.acceptTypes?.firstOrNull() ?: "*/*"
                                try {
                                    fileChooserLauncher.launch(mimeType)
                                } catch (e: Exception) {
                                    fileChooserCallback?.onReceiveValue(null)
                                    fileChooserCallback = null
                                    return false
                                }
                                return true
                            }
                        }

                        // Listen to scroll to coordinate pull refresh action
                        setOnScrollChangeListener { _, _, scrollY, _, _ ->
                            isScrollAtTop = scrollY == 0
                        }

                        loadUrl(targetUrl)
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )



            // Beautiful Gaming Offline Retry Screen Overlay
            AnimatedVisibility(
                visible = hasError,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBlack),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .padding(32.dp)
                            .background(CyberDark, shape = RoundedCornerShape(16.dp))
                            .border(width = 1.dp, color = LaserRed.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Disconnection Warning",
                            tint = LaserRed,
                            modifier = Modifier.size(64.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "ESTABLISHING CONNECTION FAILED",
                            color = TextColorLaserRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            letterSpacing = 1.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "We are unable to communicate with RockBoys servers. Please verify that your system is online.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Button(
                            onClick = {
                                hasError = false
                                isPageLoading = true
                                webViewInstance?.reload()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = LaserRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "RETRY SENSORS",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // High-fidelity Gaming Exit Confirmation Overlay
            if (showExitDialog) {
                AlertDialog(
                    onDismissRequest = { showExitDialog = false },
                    title = {
                        Text(
                            text = "EXIT PORTAL?",
                            color = ToxicGreen,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    },
                    text = {
                        Text(
                            text = "Are you sure you want to disconnect and exit the RockBoys application?",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 14.sp
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showExitDialog = false
                                onExitRequested()
                            }
                        ) {
                            Text(
                                text = "DISCONNECT",
                                color = LaserRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitDialog = false }) {
                            Text(
                                text = "CANCEL",
                                color = CyberCyan,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    containerColor = CyberDark,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.border(width = 1.dp, color = CyberLine, shape = RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

// Inline fallback since LaserRed can be referenced directly
val TextColorLaserRed = Color(0xFFFF1744)
