package com.appdeck.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM apps ORDER BY appName ASC")
    fun getAllApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE folderId IS NULL ORDER BY appName ASC")
    fun getUncategorizedApps(): Flow<List<AppEntity>>

    @Query("SELECT * FROM apps WHERE folderId = :folderId ORDER BY appName ASC")
    fun getAppsInFolder(folderId: Long): Flow<List<AppEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(apps: List<AppEntity>)

    @Update
    suspend fun updateApp(app: AppEntity)

    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)
}