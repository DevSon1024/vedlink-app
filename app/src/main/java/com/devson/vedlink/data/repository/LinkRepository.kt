package com.devson.vedlink.data.repository

import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.toDomain
import com.devson.vedlink.domain.model.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkRepository @Inject constructor(
    private val linkDao: LinkDao
) {
    fun getAllLinks(): Flow<List<Link>> {
        return linkDao.getAllLinks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getLinkById(id: Int): Link? {
        return linkDao.getLinkById(id)?.toDomain()
    }

    suspend fun getLinkByUrl(url: String): Link? {
        return linkDao.getLinkByUrl(url)?.toDomain()
    }

    fun getFavoriteLinks(): Flow<List<Link>> {
        return linkDao.getFavoriteLinks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchLinks(query: String): Flow<List<Link>> {
        return linkDao.searchLinks(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun insertLink(link: Link): Long {
        // Check if URL already exists
        val existing = linkDao.getLinkByUrl(link.url)
        if (existing != null) {
            return existing.id.toLong()
        }
        return linkDao.insertLink(link.toEntity())
    }

    suspend fun updateLink(link: Link) {
        linkDao.updateLink(link.toEntity())
    }

    suspend fun deleteLink(link: Link) {
        linkDao.deleteLink(link.toEntity())
    }

    suspend fun deleteLinkById(id: Int) {
        linkDao.deleteLinkById(id)
    }

    suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        linkDao.toggleFavorite(id, isFavorite)
    }

    suspend fun getLinksCount(): Int {
        return linkDao.getLinksCount()
    }
}