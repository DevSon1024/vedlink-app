package com.devson.vedlink.data.repository

import com.devson.vedlink.data.local.dao.LinkDao
import com.devson.vedlink.domain.model.Link
import com.devson.vedlink.domain.model.MetadataState
import com.devson.vedlink.domain.model.toDomain
import com.devson.vedlink.domain.model.toEntity
import com.devson.vedlink.domain.repository.LinkRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LinkRepositoryImpl @Inject constructor(
    private val linkDao: LinkDao
) : LinkRepository {

    override fun getAllLinks(): Flow<List<Link>> {
        return linkDao.getAllLinks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getFavoriteLinks(): Flow<List<Link>> {
        return linkDao.getFavoriteLinks().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLinksByFolder(folderId: Int?): Flow<List<Link>> {
        return if (folderId != null) {
            linkDao.getLinksByFolder(folderId).map { entities ->
                entities.map { it.toDomain() }
            }
        } else {
            linkDao.getRootLinks().map { entities ->
                entities.map { it.toDomain() }
            }
        }
    }

    override fun searchLinks(query: String): Flow<List<Link>> {
        return linkDao.searchLinks(query).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getLinksByTag(tag: String): Flow<List<Link>> {
        return linkDao.getLinksByTag(tag).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllTags(): Flow<List<String>> {
        return linkDao.getAllTagsRaw().map { rawTagsList ->
            rawTagsList
                .flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
        }
    }

    override suspend fun getLinkById(id: Int): Link? {
        return linkDao.getLinkById(id)?.toDomain()
    }

    override suspend fun getLinkByUrl(url: String): Link? {
        return linkDao.getLinkByUrl(url)?.toDomain()
    }

    override suspend fun getLinkByCanonicalUrl(canonicalUrl: String): Link? {
        return linkDao.getLinkByCanonicalUrl(canonicalUrl)?.toDomain()
    }

    override suspend fun insertLink(link: Link): Long {
        return linkDao.insertLink(link.toEntity())
    }

    override suspend fun updateLink(link: Link) {
        linkDao.updateLink(link.toEntity())
    }

    override suspend fun deleteLink(link: Link) {
        linkDao.deleteLink(link.toEntity())
    }

    override suspend fun deleteLinkById(id: Int) {
        linkDao.deleteLinkById(id)
    }

    override suspend fun toggleFavorite(id: Int, isFavorite: Boolean) {
        linkDao.toggleFavorite(id, isFavorite)
    }

    override suspend fun updateMetadataState(id: Int, state: MetadataState) {
        linkDao.updateMetadataState(id, state.name)
    }

    override suspend fun getLinksCount(): Int {
        return linkDao.getLinksCount()
    }
}
