package com.devson.vedlink.domain.repository

import com.devson.vedlink.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun getFolderById(id: Int): Folder?
    suspend fun insertFolder(folder: Folder): Long
    suspend fun updateFolder(folder: Folder)
    suspend fun deleteFolder(folder: Folder)
    fun getSubfolders(parentId: Int): Flow<List<Folder>>
}
