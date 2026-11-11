package com.startune.music.data.backup.restore

import android.net.Uri
import android.content.Context
import com.startune.music.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.startune.music.data.backup.format.BackupReader
import com.startune.music.data.backup.model.BackupOperationType
import com.startune.music.data.backup.model.BackupSection
import com.startune.music.data.backup.model.BackupTransferProgressUpdate
import com.startune.music.data.backup.model.BackupValidationResult
import com.startune.music.data.backup.model.RestorePlan
import com.startune.music.data.backup.model.RestoreResult
import com.startune.music.data.backup.module.BackupModuleHandler
import com.startune.music.data.backup.validation.ValidationPipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupReader: BackupReader,
    private val validationPipeline: ValidationPipeline,
    private val handlers: Map<BackupSection, @JvmSuppressWildcards BackupModuleHandler>
) {
    companion object {
        private const val TAG = "RestoreExecutor"
    }

    suspend fun execute(
        uri: Uri,
        plan: RestorePlan,
        onProgress: (BackupTransferProgressUpdate) -> Unit
    ): RestoreResult = withContext(Dispatchers.IO) {
        val selectedModules = plan.selectedModules.toList().sortedBy { it.key }
        val totalSteps = selectedModules.size * 2 + 3
        var step = 0

        reportProgress(onProgress, ++step, totalSteps, context.getString(R.string.backup_restore_snapshots_title), context.getString(R.string.backup_restore_snapshots_detail))

        val snapshots = mutableMapOf<BackupSection, String>()
        try {
            selectedModules.forEach { section ->
                val handler = handlers[section]
                if (handler != null) {
                    snapshots[section] = handler.snapshot()
                }
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Snapshot phase failed")
            return@withContext RestoreResult.TotalFailure(context.getString(R.string.backup_snapshot_failed_format, e.message))
        }

        reportProgress(onProgress, ++step, totalSteps, context.getString(R.string.backup_restore_preparing_title), context.getString(R.string.backup_restore_preparing_detail))

        val restoredModules = mutableListOf<BackupSection>()
        var currentSection: BackupSection? = null
        try {
            selectedModules.forEach { section ->
                currentSection = section
                val moduleInfo = plan.manifest.modules[section.key]
                if (moduleInfo != null &&
                    moduleInfo.sizeBytes > BackupReader.MAX_MODULE_PAYLOAD_BYTES
                ) {
                    throw IllegalStateException(
                        "Backup payload for ${section.key} is ${moduleInfo.sizeBytes / (1024 * 1024)}MB, " +
                            "which exceeds the ${BackupReader.MAX_MODULE_PAYLOAD_BYTES / (1024 * 1024)}MB restore safety limit."
                    )
                }

                val payload = backupReader.readModulePayload(uri, section.key).getOrThrow()

                val validationResult = validationPipeline.validateModulePayload(
                    section, payload, plan.manifest
                )
                if (validationResult is BackupValidationResult.Invalid && validationResult.fatalErrors.isNotEmpty()) {
                    throw IllegalStateException(
                        "Validation failed for ${section.key}: ${validationResult.fatalErrors.first().message}"
                    )
                }

                reportProgress(
                    onProgress, ++step, totalSteps,
                    context.getString(R.string.backup_restore_validated_title, context.getString(section.labelRes)),
                    context.getString(section.descriptionRes),
                    section
                )

                reportProgress(
                    onProgress, ++step, totalSteps,
                    context.getString(R.string.backup_restore_restoring_title, context.getString(section.labelRes)),
                    context.getString(section.descriptionRes),
                    section
                )

                val handler = handlers[section]
                    ?: throw IllegalStateException("No handler for module ${section.key}")
                handler.restore(payload)
                restoredModules.add(section)
            }
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Restore failed while processing backup module, rolling back")
            var rollbackSuccess = true
            val rollbackOrder = (restoredModules + listOfNotNull(currentSection)).distinct().asReversed()
            rollbackOrder.forEach { section ->
                try {
                    val snapshot = snapshots[section]
                    if (snapshot != null) {
                        handlers[section]?.rollback(snapshot)
                    }
                } catch (rollbackError: Exception) {
                    Timber.tag(TAG).e(rollbackError, "Rollback failed for ${section.key}")
                    rollbackSuccess = false
                }
            }

            val failedReason = e.message ?: "Unknown error"
            val failedSection = currentSection

            if (rollbackSuccess) {
                val failedLabel = failedSection?.let { context.getString(it.labelRes) } ?: context.getString(R.string.backup_unknown_module)
                return@withContext RestoreResult.TotalFailure(
                    context.getString(R.string.backup_restore_failed_format, failedLabel, failedReason)
                )
            }

            return@withContext RestoreResult.PartialFailure(
                succeeded = restoredModules.toSet(),
                failed = failedSection?.let { mapOf(it to failedReason) }.orEmpty(),
                rolledBack = false
            )
        }

        reportProgress(onProgress, ++step, totalSteps, context.getString(R.string.backup_restore_complete_title), context.getString(R.string.backup_restore_complete_detail))

        RestoreResult.Success
    }

    private fun reportProgress(
        onProgress: (BackupTransferProgressUpdate) -> Unit,
        step: Int,
        totalSteps: Int,
        title: String,
        detail: String,
        section: BackupSection? = null
    ) {
        onProgress(
            BackupTransferProgressUpdate(
                operation = BackupOperationType.IMPORT,
                step = step,
                totalSteps = totalSteps,
                title = title,
                detail = detail,
                section = section
            )
        )
    }
}
