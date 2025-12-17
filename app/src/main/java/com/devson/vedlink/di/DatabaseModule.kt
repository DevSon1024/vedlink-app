package com.devson.vedlink.di

import android.content.Context
import androidx.room.Room
import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.data.local.database.VedLinkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVedLinkDatabase(
        @ApplicationContext context: Context
    ): VedLinkDatabase {
        return Room.databaseBuilder(
            context,
            VedLinkDatabase::class.java,
            VedLinkDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideLinkDao(database: VedLinkDatabase): LinkDao {
        return database.linkDao()
    }
}
