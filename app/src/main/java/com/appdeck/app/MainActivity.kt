package com.appdeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.appdeck.app.AppDeckTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<FolderViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppDeckTheme {
                val folders by viewModel.folders.collectAsState()
                val apps by viewModel.apps.collectAsState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("AppDeck") }
                        )
                    }
                ) { paddingValues ->
                    AppListScreen(folders, apps, viewModel, paddingValues)
                }
            }
        }
    }
}