package com.appdeck.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY `order` ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Insert
    suspend fun insert(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)
}