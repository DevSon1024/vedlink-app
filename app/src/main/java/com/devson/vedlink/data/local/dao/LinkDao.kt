package com.devson.vedlink.data.local.dao

import androidx.room.*
import com.devson.vedlink.data.local.entity.LinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkDao {

    @Query("SELECT * FROM links ORDER BY createdAt DESC")
    fun getAllLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE id = :id")
    suspend fun getLinkById(id: Int): LinkEntity?

    @Query("SELECT * FROM links WHERE url = :url LIMIT 1")
    suspend fun getLinkByUrl(url: String): LinkEntity?

    @Query("SELECT * FROM links WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteLinks(): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getLinksByFolder(folderId: Int): Flow<List<LinkEntity>>

    @Query("SELECT * FROM links WHERE title LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchLinks(query: String): Flow<List<LinkEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLink(link: LinkEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(links: List<LinkEntity>)

    @Update
    suspend fun updateLink(link: LinkEntity)

    @Delete
    suspend fun deleteLink(link: LinkEntity)

    @Query("DELETE FROM links WHERE id = :id")
    suspend fun deleteLinkById(id: Int)

    @Query("DELETE FROM links")
    suspend fun deleteAllLinks()

    @Query("UPDATE links SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)

    @Query("SELECT COUNT(*) FROM links")
    suspend fun getLinksCount(): Int
}