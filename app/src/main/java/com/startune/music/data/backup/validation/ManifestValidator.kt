package com.startune.music.data.backup.validation

import com.startune.music.data.backup.model.BackupManifest
import android.content.Context
import com.startune.music.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.startune.music.data.backup.model.BackupSection
import com.startune.music.data.backup.model.BackupValidationResult
import com.startune.music.data.backup.model.Severity
import com.startune.music.data.backup.model.ValidationError
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManifestValidator @Inject constructor(
    @ApplicationContext private val context: Context
) {

    fun validate(manifest: BackupManifest): BackupValidationResult {
        val errors = mutableListOf<ValidationError>()

        if (manifest.schemaVersion < BackupManifest.MIN_SUPPORTED_VERSION) {
            errors.add(ValidationError("SCHEMA_TOO_OLD", context.getString(R.string.backup_schema_too_old_format, manifest.schemaVersion)))
        }
        if (manifest.schemaVersion > BackupManifest.CURRENT_SCHEMA_VERSION) {
            errors.add(ValidationError(
                "SCHEMA_TOO_NEW",
                context.getString(R.string.backup_schema_too_new_format, manifest.schemaVersion),
                severity = Severity.WARNING
            ))
        }

        val now = System.currentTimeMillis()
        if (manifest.createdAt > now + 86_400_000) {
            errors.add(ValidationError("TIMESTAMP_FUTURE", context.getString(R.string.backup_timestamp_future), severity = Severity.WARNING))
        }
        if (manifest.createdAt < 1_700_000_000_000) {
            errors.add(ValidationError("TIMESTAMP_OLD", context.getString(R.string.backup_timestamp_old), severity = Severity.WARNING))
        }

        val knownKeys = BackupSection.entries.map { it.key }.toSet()
        manifest.modules.keys.forEach { key ->
            if (key !in knownKeys) {
                errors.add(ValidationError(
                    "UNKNOWN_MODULE",
                    context.getString(R.string.backup_unknown_module_format, key),
                    module = key,
                    severity = Severity.WARNING
                ))
            }
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
     * Verifies the checksum of a module payload against the manifest.
     *
     * Strict by design: both [com.startune.music.data.backup.format.BackupWriter]
     * and [com.startune.music.data.backup.format.LegacyPayloadAdapter] always emit
     * `sha256:` checksums, so a missing module entry, a blank checksum, or an unknown
     * algorithm prefix can only come from a corrupted or tampered archive — treat it as a
     * verification failure rather than silently skipping the check.
     */
    fun verifyChecksum(moduleKey: String, payload: String, manifest: BackupManifest): Boolean {
        val expectedChecksum = manifest.modules[moduleKey]?.checksum ?: return false
        if (!expectedChecksum.startsWith("sha256:")) return false

        val expectedHash = expectedChecksum.removePrefix("sha256:")
        val actualHash = sha256(payload.toByteArray(Charsets.UTF_8))
        return expectedHash.equals(actualHash, ignoreCase = true)
    }

    private fun sha256(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(data).joinToString("") { "%02x".format(it) }
    }
}
