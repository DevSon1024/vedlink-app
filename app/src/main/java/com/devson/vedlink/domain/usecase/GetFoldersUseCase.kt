package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFoldersUseCase @Inject constructor(
    private val repository: FolderRepository
) {
    operator fun invoke(): Flow<List<Folder>> {
        return repository.getAllFolders()
    }
}
