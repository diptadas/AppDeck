package com.appdeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Shuffle
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
                var showCreateFolderDialog by remember { mutableStateOf(false) }
                var showReorderDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("AppDeck") },
                            actions = {
                                if (folders.isNotEmpty()) {
                                    IconButton(onClick = { showReorderDialog = true }) {
                                        Icon(Icons.Default.Shuffle, contentDescription = "Reorder folders")
                                    }
                                }
                                IconButton(onClick = { showCreateFolderDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = "Create folder")
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    AppListScreen(folders, apps, viewModel, paddingValues, showCreateFolderDialog, showReorderDialog,
                        onCreateFolderDismiss = { showCreateFolderDialog = false },
                        onReorderDismiss = { showReorderDialog = false })
                }
            }
        }
    }
}