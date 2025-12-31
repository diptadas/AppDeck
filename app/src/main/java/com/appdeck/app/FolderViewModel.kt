package com.appdeck.app

import android.app.Application
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File

class FolderViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val packageManager = application.packageManager
    private val launcherApps = application.getSystemService(LauncherApps::class.java)
    
    val folders = db.folderDao().getAllFolders().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val dbApps = db.appDao().getAllApps().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    private val dbUncategorizedApps = db.appDao().getUncategorizedApps().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // Convert database apps to UI apps with icons
    val apps = dbApps.map { appEntities ->
        appEntities.mapNotNull { entity ->
            try {
                val icon = packageManager.getApplicationIcon(entity.packageName)
                AppInfo(entity.packageName, entity.appName, icon, entity.folderId)
            } catch (e: PackageManager.NameNotFoundException) {
                null // App was uninstalled
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val uncategorizedApps = dbUncategorizedApps.map { appEntities ->
        appEntities.mapNotNull { entity ->
            try {
                val icon = packageManager.getApplicationIcon(entity.packageName)
                AppInfo(entity.packageName, entity.appName, icon, entity.folderId)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() = viewModelScope.launch {
        // Use LauncherApps API to get exactly what system launcher shows
        val userHandle = android.os.Process.myUserHandle()
        val launchableApps = launcherApps.getActivityList(null, userHandle)
            .map { launcherActivityInfo ->
                AppEntity(
                    packageName = launcherActivityInfo.applicationInfo.packageName,
                    appName = launcherActivityInfo.label.toString()
                )
            }
            .distinctBy { it.packageName }
        
        db.appDao().insertAll(launchableApps)
        
        // Clean up uninstalled apps
        val installedPackages = launchableApps.map { it.packageName }.toSet()
        dbApps.value.forEach { app ->
            if (app.packageName !in installedPackages) {
                db.appDao().deleteByPackageName(app.packageName)
            }
        }
    }

    fun assignAppToFolder(app: AppInfo, folderId: Long?) = viewModelScope.launch {
        val entity = AppEntity(app.packageName, app.appName, folderId)
        db.appDao().updateApp(entity)
    }

    fun addFolder(name: String) = viewModelScope.launch {
        val maxOrder = folders.value.maxOfOrNull { it.order } ?: 0
        db.folderDao().insert(FolderEntity(name = name, order = maxOrder + 1))
    }

    fun createFolder(name: String) = addFolder(name)

    fun renameFolder(folderId: Long, newName: String) = viewModelScope.launch {
        val folder = folders.value.find { it.id == folderId }
        folder?.let {
            db.folderDao().update(it.copy(name = newName))
        }
    }

    fun deleteFolder(folderId: Long) = viewModelScope.launch {
        val folder = folders.value.find { it.id == folderId }
        folder?.let { deleteFolder(it) }
    }

    fun reorderFolders(reorderedFolders: List<FolderEntity>) = viewModelScope.launch {
        reorderedFolders.forEachIndexed { index, folder ->
            db.folderDao().update(folder.copy(order = index))
        }
    }

    fun deleteFolder(folder: FolderEntity) = viewModelScope.launch {
        db.folderDao().delete(folder)
        // Move apps to uncategorized
        dbApps.value.filter { it.folderId == folder.id }.forEach { app ->
            db.appDao().updateApp(app.copy(folderId = null))
        }
    }

    fun getAppsInFolder(folderId: Long) = db.appDao().getAppsInFolder(folderId).map { appEntities ->
        appEntities.mapNotNull { entity ->
            try {
                val icon = packageManager.getApplicationIcon(entity.packageName)
                AppInfo(entity.packageName, entity.appName, icon, entity.folderId)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }

    @Serializable
    data class ExportData(
        val folders: List<FolderExport>,
        val apps: List<AppExport>
    )

    @Serializable
    data class FolderExport(
        val name: String,
        val order: Int
    )

    @Serializable
    data class AppExport(
        val packageName: String,
        val appName: String,
        val folderName: String?
    )

    fun exportConfiguration(fileName: String, onComplete: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            val currentFolders = folders.value
            val currentApps = dbApps.value
            
            val exportData = ExportData(
                folders = currentFolders.map { FolderExport(it.name, it.order) },
                apps = currentApps.map { app ->
                    val folderName = currentFolders.find { it.id == app.folderId }?.name
                    AppExport(app.packageName, app.appName, folderName)
                }
            )
            
            val json = Json { prettyPrint = true }.encodeToString(exportData)
            
            // Save to Downloads folder
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            file.writeText(json)
            
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        }
    }

    fun importConfiguration(uri: Uri, onComplete: (Boolean) -> Unit) = viewModelScope.launch {
        try {
            val inputStream = getApplication<Application>().contentResolver.openInputStream(uri)
            val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: return@launch onComplete(false)
            
            val importData = Json.decodeFromString<ExportData>(jsonString)
            
            // Clear existing data
            db.folderDao().deleteAll()
            db.appDao().deleteAll()
            
            // Import folders first and create name-to-id mapping
            val folderNameToId = mutableMapOf<String, Long>()
            importData.folders.forEach { folderData ->
                val folder = FolderEntity(
                    name = folderData.name,
                    order = folderData.order
                )
                val folderId = db.folderDao().insertAndGetId(folder)
                folderNameToId[folderData.name] = folderId
            }
            
            // Import apps
            importData.apps.forEach { appData ->
                val folderId = if (appData.folderName != null) {
                    folderNameToId[appData.folderName]
                } else null
                
                val app = AppEntity(
                    packageName = appData.packageName,
                    appName = appData.appName,
                    folderId = folderId
                )
                db.appDao().insert(app)
            }
            
            onComplete(true)
        } catch (e: Exception) {
            onComplete(false)
        }
    }
}