package com.devson.vedlink.domain.usecase

import com.devson.vedlink.domain.model.Folder
import com.devson.vedlink.domain.repository.FolderRepository
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(
    private val repository: FolderRepository
) {
    suspend operator fun invoke(name: String, parentId: Int? = null): Long {
        if (name.isBlank()) throw IllegalArgumentException("Folder name cannot be blank")
        val folder = Folder(name = name.trim(), parentId = parentId)
        return repository.insertFolder(folder)
    }
}
