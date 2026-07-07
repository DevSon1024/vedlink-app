package com.devson.vedlink.data.repository

import com.devson.vedlink.data.local.dao.FolderDao
import com.devson.vedlink.data.local.entity.FolderEntity
import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val folderDao: FolderDao
) : FolderRepository {

    override fun getAllFolders(): Flow<List<Folder>> {
        return folderDao.getAllFolders().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getFolderById(id: Int): Folder? {
        return folderDao.getFolderById(id)?.toDomain()
    }

    override suspend fun insertFolder(folder: Folder): Long {
        return folderDao.insertFolder(folder.toEntity())
    }

    override suspend fun updateFolder(folder: Folder) {
        folderDao.updateFolder(folder.toEntity())
    }

    override suspend fun deleteFolder(folder: Folder) {
        folderDao.deleteFolder(folder.toEntity())
    }

    override fun getSubfolders(parentId: Int): Flow<List<Folder>> {
        return folderDao.getSubfolders(parentId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    // Mapper extension functions
    private fun Folder.toEntity() = FolderEntity(
        id = id,
        name = name,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun FolderEntity.toDomain() = Folder(
        id = id,
        name = name,
        parentId = parentId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
