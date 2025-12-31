package com.appdeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
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
                var showExportDialog by remember { mutableStateOf(false) }
                var showImportDialog by remember { mutableStateOf(false) }
                var showExportSuccess by remember { mutableStateOf(false) }
                var exportedFileName by remember { mutableStateOf("") }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("AppDeck") },
                            actions = {
                                IconButton(onClick = { showExportDialog = true }) {
                                    Icon(Icons.Default.FileDownload, contentDescription = "Export")
                                }
                                IconButton(onClick = { showImportDialog = true }) {
                                    Icon(Icons.Default.FileUpload, contentDescription = "Import")
                                }
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

                // Export confirmation dialog
                if (showExportDialog) {
                    val timestamp = remember { 
                        val now = java.time.LocalDateTime.now()
                        now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
                    }
                    val fileName = "AppDeck_$timestamp.json"
                    
                    AlertDialog(
                        onDismissRequest = { showExportDialog = false },
                        title = { Text("Export Configuration") },
                        text = { Text("Export your folders and app assignments?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.exportConfiguration(fileName) { success ->
                                        showExportDialog = false
                                        if (success) {
                                            exportedFileName = fileName
                                            showExportSuccess = true
                                        }
                                    }
                                }
                            ) {
                                Text("Export")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExportDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                // Export success dialog
                if (showExportSuccess) {
                    AlertDialog(
                        onDismissRequest = { showExportSuccess = false },
                        title = { Text("Export Successful") },
                        text = { Text("Configuration exported to:\n$exportedFileName\n\nFile saved to Downloads folder.") },
                        confirmButton = {
                            TextButton(onClick = { showExportSuccess = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                // Import file picker dialog
                if (showImportDialog) {
                    AlertDialog(
                        onDismissRequest = { showImportDialog = false },
                        title = { Text("Import Configuration") },
                        text = { Text("Select an AppDeck JSON file to import your folders and app assignments.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.importConfiguration { success ->
                                        showImportDialog = false
                                        // Show success/failure message
                                    }
                                }
                            ) {
                                Text("Select File")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showImportDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}