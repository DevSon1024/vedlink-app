package com.devson.vedlink.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
            .addMigrations(MIGRATION_1_2)
            .fallbackToDestructiveMigration()
            .build()
    }

    /** Adds a unique index on the url column without destroying any existing data. */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_links_url ON links(url)"
            )
        }
    }

    @Provides
    @Singleton
    fun provideLinkDao(database: VedLinkDatabase): LinkDao {
        return database.linkDao()
    }
}
