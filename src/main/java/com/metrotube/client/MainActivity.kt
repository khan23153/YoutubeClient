package com.metrotube.client

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.metrotube.client.auth.AuthenticationManager
import com.metrotube.client.ui.screens.HomeScreen
import com.metrotube.client.ui.screens.HomeViewModel
import com.metrotube.client.ui.theme.MetroTubeClientTheme

class MainActivity : ComponentActivity() {

    private lateinit var authManager: AuthenticationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthenticationManager(this)

        // Check if user is authenticated
        if (!authManager.isAuthenticated()) {
            // Redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            MetroTubeClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContent()
                }
            }
        }
    }
}

@Composable
private fun MainContent() {
    val viewModel: HomeViewModel = viewModel()
    HomeScreen(viewModel = viewModel)
}