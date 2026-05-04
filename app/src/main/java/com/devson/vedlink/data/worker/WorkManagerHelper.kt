package com.devson.vedlink.data.worker

import android.content.Context
import androidx.work.*
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Enqueues a metadata fetch worker for the given URL and link ID.
     */
    fun enqueueMetadataFetch(url: String, linkId: Int, isForcedRefresh: Boolean = false) {
        val inputData = Data.Builder()
            .putString(MetadataFetchWorker.KEY_URL, url)
            .putInt(MetadataFetchWorker.KEY_LINK_ID, linkId)
            .putBoolean(MetadataFetchWorker.KEY_IS_FORCED_REFRESH, isForcedRefresh)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<MetadataFetchWorker>()
            .setInputData(inputData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                10,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            "${MetadataFetchWorker.WORK_NAME}_$linkId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    fun cancelMetadataFetch(linkId: Int) {
        workManager.cancelUniqueWork("${MetadataFetchWorker.WORK_NAME}_$linkId")
    }
}
