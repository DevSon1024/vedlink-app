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
     * Schedules a metadata fetch for the given link.
     *
     * @param linkId         The database ID of the link to fetch metadata for.
     * @param isForcedRefresh When `true`, the Worker will bypass the local cache check and
     *                        always make a fresh network request. Pass `true` only when the
     *                        user explicitly taps "Refresh Link". Defaults to `false` for
     *                        the normal Add Link flow so cached data is reused.
     */
    fun enqueueLinkMetadataFetch(linkId: Int, isForcedRefresh: Boolean = false) {
        val inputData = Data.Builder()
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

    fun cancelLinkMetadataFetch(linkId: Int) {
        workManager.cancelUniqueWork("${MetadataFetchWorker.WORK_NAME}_$linkId")
    }
}
