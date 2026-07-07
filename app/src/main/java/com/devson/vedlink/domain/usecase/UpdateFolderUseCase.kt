package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.repository.FolderRepository
import javax.inject.Inject

class UpdateFolderUseCase @Inject constructor(
    private val repository: FolderRepository
) {
    suspend operator fun invoke(folder: Folder) {
        if (folder.name.isBlank()) throw IllegalArgumentException("Folder name cannot be blank")
        repository.updateFolder(folder)
    }
}
