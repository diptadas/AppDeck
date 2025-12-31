package com.appdeck.app

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.UserHandle
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

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

    var editMode by mutableStateOf(false)
        private set

    init {
        loadInstalledApps()
    }

    fun toggleEdit() { 
        editMode = !editMode 
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
}