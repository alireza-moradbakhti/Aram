package com.aram.core.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aram.core.domain.repository.LedgerSync
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Push-then-pull cycle for the whole ledger, used by the periodic sync. */
@HiltWorker
class LedgerSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val ledgerSync: LedgerSync,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result =
        if (ledgerSync.syncNow()) Result.success() else Result.retry()
}
