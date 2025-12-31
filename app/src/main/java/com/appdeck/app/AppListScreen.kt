@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.appdeck.app

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun AppListScreen(
    folders: List<FolderEntity>,
    apps: List<AppInfo>,
    viewModel: FolderViewModel,
    paddingValues: PaddingValues,
    showCreateFolderDialog: Boolean,
    showReorderDialog: Boolean,
    onCreateFolderDismiss: () -> Unit,
    onReorderDismiss: () -> Unit
) {
    val uncategorizedApps by viewModel.uncategorizedApps.collectAsState()
    var selectedFolder by remember { mutableStateOf<FolderEntity?>(null) }
    var appToMove by remember { mutableStateOf<AppInfo?>(null) }
    var folderToManage by remember { mutableStateOf<FolderEntity?>(null) }
    var isClosingFolder by remember { mutableStateOf(false) }
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
            // Empty item for spacing if needed
        }
        
        // Combined grid for folders and apps
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height(((folders.size + uncategorizedApps.size + 3) / 4 * 120).dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Folders first
                items(folders) { folder ->
                    val folderApps = allApps.filter { it.folderId == folder.id }.take(3)
                    FolderGridItem(
                        folder = folder,
                        apps = folderApps,
                        onClick = { selectedFolder = folder },
                        onLongPress = { folderToManage = folder }
                    )
                }
                
                // Then uncategorized apps
                items(uncategorizedApps) { app ->
                    AppGridItem(
                        app = app,
                        onLongPress = { appToMove = app }
                    )
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
            isClosing = isClosingFolder,
            onDismiss = { 
                isClosingFolder = true
                // Delay to allow animation
                kotlinx.coroutines.MainScope().launch {
                    kotlinx.coroutines.delay(200)
                    selectedFolder = null
                    isClosingFolder = false
                }
            }
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
                onCreateFolderDismiss()
            },
            onDismiss = onCreateFolderDismiss
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
    
    // Reorder folders dialog
    if (showReorderDialog) {
        ReorderFoldersDialog(
            folders = folders,
            onReorder = { reorderedFolders ->
                viewModel.reorderFolders(reorderedFolders)
                onReorderDismiss()
            },
            onDismiss = onReorderDismiss
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
    onLongPress: () -> Unit = {}
) {
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
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        launchIntent?.let { context.startActivity(it) }
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
    }
}

@Composable
fun FolderPopup(
    folder: FolderEntity,
    viewModel: FolderViewModel,
    isClosing: Boolean,
    onDismiss: () -> Unit
) {
    val folderApps by viewModel.getAppsInFolder(folder.id).collectAsState(initial = emptyList())
    val folders by viewModel.folders.collectAsState()
    var appToMove by remember { mutableStateOf<AppInfo?>(null) }
    
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
    
    LaunchedEffect(isClosing) {
        if (isClosing) {
            scale.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessHigh
                )
            )
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
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
                    modifier = Modifier.heightIn(max = 400.dp),
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReorderFoldersDialog(
    folders: List<FolderEntity>,
    onReorder: (List<FolderEntity>) -> Unit,
    onDismiss: () -> Unit
) {
    var reorderedFolders by remember { mutableStateOf(folders) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Reorder Folders", style = MaterialTheme.typography.headlineSmall)
                Text("Use up/down buttons to reorder", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(reorderedFolders, key = { _, folder -> folder.id }) { index, folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    MaterialTheme.shapes.small
                                )
                                .padding(12.dp)
                                .animateItemPlacement(),
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
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val newList = reorderedFolders.toMutableList()
                                        val temp = newList[index]
                                        newList[index] = newList[index - 1]
                                        newList[index - 1] = temp
                                        reorderedFolders = newList
                                    }
                                },
                                enabled = index > 0
                            ) {
                                Text("↑")
                            }
                            IconButton(
                                onClick = {
                                    if (index < reorderedFolders.size - 1) {
                                        val newList = reorderedFolders.toMutableList()
                                        val temp = newList[index]
                                        newList[index] = newList[index + 1]
                                        newList[index + 1] = temp
                                        reorderedFolders = newList
                                    }
                                },
                                enabled = index < reorderedFolders.size - 1
                            ) {
                                Text("↓")
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    TextButton(onClick = { onReorder(reorderedFolders) }) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
