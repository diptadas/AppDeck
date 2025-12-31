@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.appdeck.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.IntSize

@Composable
fun AppListScreen(
    folders: List<FolderEntity>,
    apps: List<AppInfo>,
    viewModel: FolderViewModel,
    paddingValues: PaddingValues
) {
    val uncategorizedApps by viewModel.uncategorizedApps.collectAsState()
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var appToMove by remember { mutableStateOf<AppInfo?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToManage by remember { mutableStateOf<FolderEntity?>(null) }
    val allApps by viewModel.apps.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Folders section
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Folders",
                    style = MaterialTheme.typography.headlineSmall
                )
                IconButton(onClick = { showCreateFolderDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Create folder")
                }
            }
        }
        
        if (folders.isNotEmpty()) {
            
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(((folders.size + 4) / 5 * 120).dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(folders) { folder ->
                        val folderApps = allApps.filter { it.folderId == folder.id }.take(3)
                        FolderGridItem(
                            folder = folder,
                            apps = folderApps,
                            onClick = { selectedFolder = folder },
                            onLongPress = { folderToManage = folder }
                        )
                    }
                }
            }
        }
        
        // Uncategorized apps section
        if (uncategorizedApps.isNotEmpty()) {
            item {
                Text(
                    text = "Apps",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.height(((uncategorizedApps.size + 4) / 5 * 120).dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uncategorizedApps) { app ->
                        AppGridItem(
                            app = app,
                            folders = folders,
                            editMode = viewModel.editMode,
                            onFolderAssign = { folderId -> 
                                viewModel.assignAppToFolder(app, folderId)
                            },
                            onLongPress = { appToMove = app }
                        )
                    }
                }
            }
        }
        
        // Show message if no apps loaded yet
        if (apps.isEmpty()) {
            item {
                Text(
                    text = "Loading apps...",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
    
    // Folder popup
    selectedFolder?.let { folder ->
        FolderPopup(
            folder = folder,
            viewModel = viewModel,
            onDismiss = { selectedFolder = null }
        )
    }
    
    // Folder selection popup
    appToMove?.let { app ->
        FolderSelectionPopup(
            app = app,
            folders = folders,
            currentFolderId = app.folderId,
            onFolderSelected = { folderId ->
                viewModel.assignAppToFolder(app, folderId)
                appToMove = null
            },
            onDismiss = { appToMove = null }
        )
    }
    
    // Create folder dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onCreateFolder = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            },
            onDismiss = { showCreateFolderDialog = false }
        )
    }
    
    // Manage folder dialog
    folderToManage?.let { folder ->
        ManageFolderDialog(
            folder = folder,
            onRename = { newName ->
                viewModel.renameFolder(folder.id, newName)
                folderToManage = null
            },
            onDelete = {
                viewModel.deleteFolder(folder.id)
                folderToManage = null
            },
            onDismiss = { folderToManage = null }
        )
    }
}

@Composable
fun FolderGridItem(
    folder: FolderEntity,
    apps: List<AppInfo>,
    onClick: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(64.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            )
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (apps.size) {
                0 -> Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                1 -> AsyncImage(
                    model = apps[0].icon,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp)
                )
                2 -> Row {
                    AsyncImage(
                        model = apps[0].icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    AsyncImage(
                        model = apps[1].icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                else -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = apps[0].icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Row {
                        AsyncImage(
                            model = apps[1].icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        AsyncImage(
                            model = apps[2].icon,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppGridItem(
    app: AppInfo,
    folders: List<FolderEntity>,
    editMode: Boolean,
    onFolderAssign: (Long?) -> Unit,
    onLongPress: () -> Unit = {}
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(64.dp)
    ) {
        AsyncImage(
            model = app.icon,
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .combinedClickable(
                    onClick = {
                        if (!editMode) {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            launchIntent?.let { context.startActivity(it) }
                        }
                    },
                    onLongClick = { onLongPress() }
                )
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = app.appName,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (editMode) {
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = folders.find { it.id == app.folderId }?.name ?: "None",
                    onValueChange = { },
                    readOnly = true,
                    textStyle = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .menuAnchor()
                        .width(70.dp)
                        .height(40.dp)
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
fun FolderPopup(
    folder: FolderEntity,
    viewModel: FolderViewModel,
    onDismiss: () -> Unit
) {
    val folderApps by viewModel.getAppsInFolder(folder.id!!).collectAsState(initial = emptyList())
    val folders by viewModel.folders.collectAsState()
    var appToMove by remember { mutableStateOf<AppInfo?>(null) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folderApps) { app ->
                        val context = LocalContext.current
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            AsyncImage(
                                model = app.icon,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .combinedClickable(
                                        onClick = {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                            launchIntent?.let { context.startActivity(it) }
                                        },
                                        onLongClick = { appToMove = app }
                                    )
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
    
    // Folder selection popup for apps in this folder
    appToMove?.let { app ->
        FolderSelectionPopup(
            app = app,
            folders = folders,
            currentFolderId = folder.id,
            onFolderSelected = { folderId ->
                viewModel.assignAppToFolder(app, folderId)
                appToMove = null
            },
            onDismiss = { appToMove = null }
        )
    }
}

@Composable
fun FolderSelectionPopup(
    app: AppInfo,
    folders: List<FolderEntity>,
    currentFolderId: Long?,
    onFolderSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Move ${app.appName}",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show other folders (not current folder)
                    items(folders.filter { it.id != currentFolderId }) { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderSelected(folder.id) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (currentFolderId != null) {
                        TextButton(
                            onClick = { onFolderSelected(null) },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Remove")
                        }
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFolderDialog(
    onCreateFolder: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Create Folder", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(
                        onClick = { 
                            if (folderName.isNotBlank()) onCreateFolder(folderName.trim())
                        },
                        enabled = folderName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                }
            }
        }
    }
}

@Composable
fun ManageFolderDialog(
    folder: FolderEntity,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var showRename by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf(folder.name) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                if (showRename) {
                    Text("Rename Folder", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Folder name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                    ) {
                        TextButton(onClick = { showRename = false }) {
                            Text("Cancel")
                        }
                        TextButton(
                            onClick = { 
                                if (newName.isNotBlank()) onRename(newName.trim())
                            },
                            enabled = newName.isNotBlank()
                        ) {
                            Text("Rename")
                        }
                    }
                } else {
                    Text("Manage \"${folder.name}\"", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = { showRename = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Rename")
                    }
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}
