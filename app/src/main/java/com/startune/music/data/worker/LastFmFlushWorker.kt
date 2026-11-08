package com.startune.music.data.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.startune.music.data.database.LastFmDao
import com.startune.music.data.lastfm.LastFmRepository
import com.startune.music.data.lastfm.LastFmSubmitResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Drains the Last.fm pending-scrobble queue, oldest first.
 *
 * The proxy submits one scrobble per call, so each row is submitted individually:
 * success and permanent rejection delete the row; an invalid session pauses flushing
 * entirely (the queue survives until the user reconnects); network failures retry
 * with backoff.
 */
@HiltWorker
class LastFmFlushWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val lastFmDao: LastFmDao,
    private val repository: LastFmRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!repository.isAuthorized()) return Result.success()

        repeat(MAX_BATCHES_PER_RUN) {
            val batch = lastFmDao.oldestPending(BATCH_SIZE)
            if (batch.isEmpty()) return Result.success()

            var transientFailure = false
            for (row in batch) {
                when (repository.submitScrobble(row)) {
                    LastFmSubmitResult.Success -> {
                        lastFmDao.deleteByIds(listOf(row.id))
                    }
                    LastFmSubmitResult.InvalidPayload -> {
                        lastFmDao.deleteByIds(listOf(row.id))
                    }
                    LastFmSubmitResult.AuthFailed -> {
                        Timber.w("Last.fm session rejected; pausing scrobble flush")
                        return Result.success()
                    }
                    is LastFmSubmitResult.TransientError -> {
                        lastFmDao.incrementAttempts(listOf(row.id))
                        transientFailure = true
                    }
                }
            }
            if (transientFailure) return Result.retry()
        }
        // Batch budget exhausted; remaining rows flush on the next enqueue or app start.
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "lastfm_scrobble_flush"
        private const val BATCH_SIZE = 20
        private const val MAX_BATCHES_PER_RUN = 10

        fun request(initialDelaySeconds: Long = 0L): OneTimeWorkRequest {
            val builder = OneTimeWorkRequestBuilder<LastFmFlushWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            if (initialDelaySeconds > 0L) {
                builder.setInitialDelay(initialDelaySeconds, TimeUnit.SECONDS)
            }
            return builder.build()
        }
    }
}
