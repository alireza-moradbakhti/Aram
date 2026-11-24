package com.aram.core.data.outbox

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aram.core.domain.repository.OutboxSync
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncOutboxWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val outboxSync: OutboxSync,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        if (outboxSync.flush()) Result.success() else Result.retry()
}
