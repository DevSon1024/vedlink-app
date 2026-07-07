package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.repository.FolderRepository
import javax.inject.Inject

class DeleteFolderUseCase @Inject constructor(
    private val repository: FolderRepository
) {
    suspend operator fun invoke(folder: Folder) {
        repository.deleteFolder(folder)
    }
}
