package com.metrotube.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.*
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.activity.compose.setContent
import androidx.compose.ui.viewinterop.AndroidView
import com.metrotube.client.auth.AuthenticationManager
import com.metrotube.client.ui.theme.MetroTubeClientTheme

class LoginActivity : ComponentActivity() {

    private lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthenticationManager(this)

        setContent {
            MetroTubeClientTheme {
                LoginScreen(
                    onLoginSuccess = {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    },
                    authManager = authManager
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    authManager: AuthenticationManager
) {
    var isLoading by remember { mutableStateOf(false) }
    var showAdvancedLogin by remember { mutableStateOf(false) }
    var tokenInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "MetroTube Login",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (showAdvancedLogin) {
            // Advanced Token Login
            OutlinedTextField(
                value = tokenInput,
                onValueChange = { tokenInput = it },
                label = { Text("Enter Token") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Paste your authentication token") }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Button(
                    onClick = {
                        if (tokenInput.isNotBlank()) {
                            authManager.saveAdvancedToken(tokenInput)
                            onLoginSuccess()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Login with Token")
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = { showAdvancedLogin = false },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Back")
                }
            }
        } else {
            // WebView Login Button
            Button(
                onClick = {
                    isLoading = true
                    // This will be handled by WebView
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Login with Google")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { showAdvancedLogin = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Advanced Token Login")
            }

            Spacer(modifier = Modifier.height(32.dp))

            // WebView for authentication
            if (isLoading) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            setupWebViewForAuth(authManager, onLoginSuccess)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

private fun WebView.setupWebViewForAuth(
    authManager: AuthenticationManager,
    onLoginSuccess: () -> Unit
) {
    // Configure WebView settings
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        cacheMode = WebSettings.LOAD_NO_CACHE
        userAgentString = getUserAgentString()
    }

    // Add JavaScript interface for token extraction
    addJavascriptInterface(object {
        @JavascriptInterface
        fun onRetrieveVisitorData(newVisitorData: String?) {
            Log.d("LoginAuth", "Visitor Data: $newVisitorData")
            if (newVisitorData != null) {
                authManager.saveVisitorData(newVisitorData)
            }
        }

        @JavascriptInterface
        fun onRetrieveDataSyncId(newDataSyncId: String?) {
            Log.d("LoginAuth", "DataSync ID: $newDataSyncId")
            if (newDataSyncId != null) {
                authManager.saveDataSyncId(newDataSyncId)
            }
        }

        @JavascriptInterface
        fun onAccountInfo(accountName: String?, accountEmail: String?, channelHandle: String?) {
            Log.d("LoginAuth", "Account Info - Name: $accountName, Email: $accountEmail")
            authManager.saveAccountInfo(accountName, accountEmail, channelHandle)
        }
    }, "Android")

    webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d("LoginAuth", "Page finished: $url")

            // Extract authentication data when on YouTube Music
            if (url?.contains("music.youtube.com") == true) {
                // Extract visitor data and sync ID
                view?.loadUrl("javascript:Android.onRetrieveVisitorData(window.yt?.config_?.VISITOR_DATA)")
                view?.loadUrl("javascript:Android.onRetrieveDataSyncId(window.yt?.config_?.DATASYNC_ID)")

                // Extract cookies
                val cookieManager = CookieManager.getInstance()
                val cookies = cookieManager.getCookie(url)
                if (cookies != null) {
                    authManager.saveCookies(cookies)
                }

                // Try to extract account information
                view?.loadUrl("""
                    javascript:
                    try {
                        var accountInfo = window.yt?.config_?.ACCOUNT_INFO;
                        if (accountInfo) {
                            Android.onAccountInfo(
                                accountInfo.name || null,
                                accountInfo.email || null, 
                                accountInfo.channelHandle || null
                            );
                        }
                    } catch(e) {
                        console.log('Error extracting account info:', e);
                    }
                """.trimIndent())

                // Mark as authenticated and navigate
                authManager.setAuthenticated(true)
                onLoginSuccess()
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d("LoginAuth", "Page started: $url")
        }
    }

    webChromeClient = WebChromeClient()

    // Load login page
    val loginUrl = "https://accounts.google.com/ServiceLogin?service=youtube&continue=https://music.youtube.com/"
    loadUrl(loginUrl)
}

private fun getUserAgentString(): String {
    return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
}