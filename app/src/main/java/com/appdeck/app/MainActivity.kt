package com.appdeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.appdeck.app.AppDeckTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<FolderViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AppDeckTheme {
                var currentScreen by remember { mutableStateOf("home") }
                var showFolderDialog by remember { mutableStateOf(false) }
                val drawerState = rememberDrawerState(DrawerValue.Closed)
                val scope = rememberCoroutineScope()
                
                val folders by viewModel.folders.collectAsState()
                val apps by viewModel.apps.collectAsState()

                when (currentScreen) {
                    "edit" -> EditAppsScreen(
                        folders = folders,
                        apps = apps,
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" }
                    )
                    "manage" -> ManageFoldersScreen(
                        folders = folders,
                        viewModel = viewModel,
                        onBack = { currentScreen = "home" }
                    )
                    else -> {
                        ModalNavigationDrawer(
                            drawerState = drawerState,
                            drawerContent = {
                                ModalDrawerSheet {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text("AppDeck", style = MaterialTheme.typography.headlineSmall)
                                        Spacer(modifier = Modifier.height(16.dp))
                                        
                                        NavigationDrawerItem(
                                            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                            label = { Text("Edit Apps") },
                                            selected = false,
                                            onClick = {
                                                currentScreen = "edit"
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                        
                                        NavigationDrawerItem(
                                            icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                            label = { Text("Manage Folders") },
                                            selected = false,
                                            onClick = {
                                                currentScreen = "manage"
                                                scope.launch { drawerState.close() }
                                            }
                                        )
                                    }
                                }
                            }
                        ) {
                            Scaffold(
                                topBar = {
                                    TopAppBar(
                                        title = { Text("AppDeck") },
                                        navigationIcon = {
                                            IconButton(onClick = { 
                                                scope.launch { drawerState.open() }
                                            }) {
                                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                                            }
                                        }
                                    )
                                }
                            ) { paddingValues ->
                                AppListScreen(folders, apps, viewModel, paddingValues)
                            }
                        }
                    }
                }

                if (showFolderDialog) {
                    FolderManagementDialog(
                        folders = folders,
                        onDismiss = { showFolderDialog = false },
                        onAddFolder = { name -> viewModel.addFolder(name) },
                        onDeleteFolder = { folder -> viewModel.deleteFolder(folder) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppsScreen(
    folders: List<FolderEntity>,
    apps: List<AppInfo>,
    viewModel: FolderViewModel,
    onBack: () -> Unit
) {
    val uncategorizedApps by viewModel.uncategorizedApps.collectAsState()
    val allApps = (uncategorizedApps + apps.filter { it.folderId != null }).sortedBy { it.appName }
    
    // Temporary state for changes
    var tempAssignments by remember { mutableStateOf(
        allApps.associate { it.packageName to it.folderId }
    ) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Apps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Text("Cancel")
                    }
                    TextButton(onClick = {
                        // Save all changes
                        tempAssignments.forEach { (packageName, folderId) ->
                            val app = allApps.find { it.packageName == packageName }
                            if (app != null) {
                                viewModel.assignAppToFolder(app, folderId)
                            }
                        }
                        onBack()
                    }) {
                        Text("Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Assign apps to folders",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            items(allApps) { app ->
                AppEditItem(
                    app = app,
                    folders = folders,
                    currentFolderId = tempAssignments[app.packageName],
                    onFolderAssign = { folderId -> 
                        tempAssignments = tempAssignments.toMutableMap().apply {
                            put(app.packageName, folderId)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageFoldersScreen(
    folders: List<FolderEntity>,
    viewModel: FolderViewModel,
    onBack: () -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = newFolderName,
                onValueChange = { newFolderName = it },
                label = { Text("New Folder Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (newFolderName.isNotBlank()) {
                        viewModel.addFolder(newFolderName)
                        newFolderName = ""
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Add Folder")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Existing Folders",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyColumn {
                items(folders) { folder ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            TextButton(onClick = { viewModel.deleteFolder(folder) }) {
                                Text("Delete", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppEditItem(
    app: AppInfo,
    folders: List<FolderEntity>,
    currentFolderId: Long?,
    onFolderAssign: (Long?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = app.icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Text(
                text = app.appName,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = folders.find { it.id == currentFolderId }?.name ?: "None",
                    onValueChange = { },
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .width(150.dp)
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None") },
                        onClick = {
                            onFolderAssign(null)
                            expanded = false
                        }
                    )
                    
                    folders.forEach { folder ->
                        DropdownMenuItem(
                            text = { Text(folder.name) },
                            onClick = {
                                onFolderAssign(folder.id)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderManagementDialog(
    folders: List<FolderEntity>,
    onDismiss: () -> Unit,
    onAddFolder: (String) -> Unit,
    onDeleteFolder: (FolderEntity) -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Folders") },
        text = {
            Column {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("New Folder Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                folders.forEach { folder ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(folder.name, modifier = Modifier.weight(1f))
                        TextButton(onClick = { onDeleteFolder(folder) }) {
                            Text("Delete")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newFolderName.isNotBlank()) {
                        onAddFolder(newFolderName)
                        newFolderName = ""
                    }
                }
            ) {
                Text("Add Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}