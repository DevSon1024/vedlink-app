package com.devson.vedlink.data.local.dao

import androidx.room.*
import com.devson.vedlink.data.local.entity.SearchTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SearchTopicDao {

    @Query("SELECT * FROM search_topics ORDER BY timestamp DESC")
    fun getAllSearchTopics(): Flow<List<SearchTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearchTopic(topic: SearchTopicEntity): Long

    @Update
    suspend fun updateSearchTopic(topic: SearchTopicEntity)

    @Delete
    suspend fun deleteSearchTopic(topic: SearchTopicEntity)
}
