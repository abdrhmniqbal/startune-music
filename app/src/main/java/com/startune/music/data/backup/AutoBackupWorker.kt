package com.startune.music.data.backup

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Runs the auto-backup check in the background on the configured interval.
 * A skipped check (disabled, or not due yet) is not a failure, so the work
 * always succeeds; the next periodic run checks again.
 */
@HiltWorker
class AutoBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val autoBackupManager: AutoBackupManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        autoBackupManager.runCheck()
        return Result.success()
    }
}
