package com.devson.vedlink.data.local.dao

import androidx.room.*
import com.devson.vedlink.data.local.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getAllFolders(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: Int): FolderEntity?

    @Query("SELECT * FROM folders WHERE parentId = :parentId ORDER BY name ASC")
    fun getSubfolders(parentId: Int): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentId IS NULL ORDER BY name ASC")
    fun getRootFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun deleteFolder(folder: FolderEntity)
}
