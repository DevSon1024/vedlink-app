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

    fun enqueueLinkMetadataFetch(linkId: Int) {
        val workRequest = OneTimeWorkRequestBuilder<MetadataFetchWorker>()
            .setInputData(
                workDataOf(MetadataFetchWorker.KEY_LINK_ID to linkId)
            )
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
