package com.startune.music.data.backup.validation

import android.net.Uri
import android.content.Context
import com.startune.music.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.startune.music.data.backup.model.BackupManifest
import com.startune.music.data.backup.model.BackupSection
import com.startune.music.data.backup.model.BackupValidationResult
import com.startune.music.data.backup.model.Severity
import com.startune.music.data.backup.model.ValidationError
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ValidationPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileValidator: BackupFileValidator,
    private val manifestValidator: ManifestValidator,
    private val moduleSchemaValidator: ModuleSchemaValidator
) {
    /**
     * Full validation: file → manifest → checksums → per-module schemas.
     */
    fun validateFile(uri: Uri): BackupValidationResult {
        return fileValidator.validate(uri)
    }

    fun validateManifest(manifest: BackupManifest): BackupValidationResult {
        return manifestValidator.validate(manifest)
    }

    fun validateModulePayload(
        section: BackupSection,
        payload: String,
        manifest: BackupManifest? = null
    ): BackupValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (manifest != null && !manifestValidator.verifyChecksum(section.key, payload, manifest)) {
            errors.add(ValidationError(
                "CHECKSUM_MISMATCH",
                context.getString(R.string.backup_checksum_mismatch_format, context.getString(section.labelRes)),
                module = section.key
            ))
            return BackupValidationResult.Invalid(errors)
        }

        val schemaResult = moduleSchemaValidator.validate(section, payload)
        if (schemaResult is BackupValidationResult.Invalid) {
            errors.addAll(schemaResult.errors)
        }

        return if (errors.any { it.severity == Severity.ERROR }) {
            BackupValidationResult.Invalid(errors)
        } else if (errors.isNotEmpty()) {
            BackupValidationResult.Invalid(errors)
        } else {
            BackupValidationResult.Valid
        }
    }

    /**
     * Collect all warnings from validation results (non-fatal).
     */
    fun collectWarnings(vararg results: BackupValidationResult): List<ValidationError> {
        return results.flatMap { result ->
            when (result) {
                is BackupValidationResult.Invalid -> result.warnings
                is BackupValidationResult.Valid -> emptyList()
            }
        }
    }
}
