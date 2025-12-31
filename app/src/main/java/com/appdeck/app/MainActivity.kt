package com.appdeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
                var showImportProgress by remember { mutableStateOf(false) }
                var showExportSuccess by remember { mutableStateOf(false) }
                var showImportSuccess by remember { mutableStateOf(false) }
                var showImportError by remember { mutableStateOf(false) }
                var exportedFileName by remember { mutableStateOf("") }

                val filePickerLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.GetContent()
                ) { uri ->
                    uri?.let { 
                        showImportProgress = true
                        viewModel.importConfiguration(it) { success ->
                            showImportProgress = false
                            if (success) {
                                showImportSuccess = true
                            } else {
                                showImportError = true
                            }
                        }
                    }
                }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("AppDeck") },
                            actions = {
                                IconButton(onClick = { showExportDialog = true }) {
                                    Icon(Icons.Default.Save, contentDescription = "Export")
                                }
                                IconButton(onClick = { showImportDialog = true }) {
                                    Icon(Icons.Default.Restore, contentDescription = "Import")
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
                                    showImportDialog = false
                                    filePickerLauncher.launch("application/json")
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

                // Import progress dialog
                if (showImportProgress) {
                    AlertDialog(
                        onDismissRequest = { }, // Empty - makes it non-dismissible
                        title = { Text("Importing Configuration") },
                        text = { 
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(16.dp))
                                Text("Please wait while importing your configuration...")
                            }
                        },
                        confirmButton = { }, // Empty - no buttons
                        dismissButton = { }  // Empty - no buttons
                    )
                }

                // Import success dialog
                if (showImportSuccess) {
                    AlertDialog(
                        onDismissRequest = { showImportSuccess = false },
                        title = { Text("Import Successful") },
                        text = { Text("Configuration imported successfully. Your folders and app assignments have been updated.") },
                        confirmButton = {
                            TextButton(onClick = { showImportSuccess = false }) {
                                Text("OK")
                            }
                        }
                    )
                }

                // Import error dialog
                if (showImportError) {
                    AlertDialog(
                        onDismissRequest = { showImportError = false },
                        title = { Text("Import Failed") },
                        text = { Text("Failed to import configuration. Please check that the file is a valid AppDeck JSON export.") },
                        confirmButton = {
                            TextButton(onClick = { showImportError = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}