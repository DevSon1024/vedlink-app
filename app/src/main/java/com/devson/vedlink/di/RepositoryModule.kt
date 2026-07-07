package com.devson.vedlink.di

import com.devson.vedlink.data.repository.LinkRepositoryImpl
import com.devson.vedlink.data.repository.FolderRepositoryImpl
import com.devson.vedlink.domain.repository.LinkRepository
import com.devson.vedlink.domain.repository.FolderRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLinkRepository(
        linkRepositoryImpl: LinkRepositoryImpl
    ): LinkRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(
        folderRepositoryImpl: FolderRepositoryImpl
    ): FolderRepository
}
