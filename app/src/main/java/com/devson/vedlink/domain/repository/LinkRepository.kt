package com.devson.vedlink.domain.repository

import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.MetadataState
import kotlinx.coroutines.flow.Flow

interface LinkRepository {
    fun getAllLinks(): Flow<List<Link>>
    fun getFavoriteLinks(): Flow<List<Link>>
    fun getLinksByFolder(folderId: Int?): Flow<List<Link>>
    fun searchLinks(query: String): Flow<List<Link>>
    fun getLinksByTag(tag: String): Flow<List<Link>>
    fun getAllTags(): Flow<List<String>>
    suspend fun getLinkById(id: Int): Link?
    suspend fun getLinkByUrl(url: String): Link?
    suspend fun getLinkByCanonicalUrl(canonicalUrl: String): Link?
    suspend fun insertLink(link: Link): Long
    suspend fun updateLink(link: Link)
    suspend fun deleteLink(link: Link)
    suspend fun deleteLinkById(id: Int)
    suspend fun toggleFavorite(id: Int, isFavorite: Boolean)
    suspend fun updateMetadataState(id: Int, state: MetadataState)
    suspend fun getLinksCount(): Int
}
