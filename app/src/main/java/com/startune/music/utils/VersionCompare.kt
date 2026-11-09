package com.startune.music.utils

/**
 * Pure semver comparison, ported from the old app's
 * core/config/version-compare.ts so update checks and the changelog agree
 * with the release history. A release without a prerelease suffix is newer
 * than the same version with one (2.0.0 > 2.0.0-rc.2).
 */
object VersionCompare {
    private val PREVIEW_PATTERN =
        Regex("(?:^|[-.])(alpha|beta|rc|preview)(?:$|[-.\\d])", RegexOption.IGNORE_CASE)

    fun isPreviewReleaseVersion(version: String): Boolean =
        PREVIEW_PATTERN.containsMatchIn(version)

    fun normalizeVersion(value: String): String =
        value.trim()
            .replace(Regex("^[^0-9]+"), "")
            .replace(Regex("-dev(?:\\.\\d+)?$", RegexOption.IGNORE_CASE), "")

    private data class ParsedVersion(
        val mainParts: List<Int>,
        val prereleaseParts: List<String>
    )

    private fun parseVersion(value: String): ParsedVersion {
        val normalized = normalizeVersion(value)
        val dashIndex = normalized.indexOf('-')
        val main = if (dashIndex >= 0) normalized.substring(0, dashIndex) else normalized
        val prerelease = if (dashIndex >= 0) normalized.substring(dashIndex + 1) else ""
        return ParsedVersion(
            mainParts = main.split(".").map { it.toIntOrNull() ?: 0 },
            prereleaseParts = if (prerelease.isNotEmpty()) prerelease.split(".") else emptyList()
        )
    }

    private fun comparePrereleasePart(left: String, right: String): Int {
        val leftNumber = left.toIntOrNull()
        val rightNumber = right.toIntOrNull()
        val leftIsNumber = leftNumber != null && leftNumber.toString() == left
        val rightIsNumber = rightNumber != null && rightNumber.toString() == right
        if (leftIsNumber && rightIsNumber) return leftNumber!! - rightNumber!!
        if (leftIsNumber) return -1
        if (rightIsNumber) return 1
        return left.compareTo(right)
    }

    fun compareVersions(left: String, right: String): Int {
        val leftVersion = parseVersion(left)
        val rightVersion = parseVersion(right)
        val maxLength = maxOf(leftVersion.mainParts.size, rightVersion.mainParts.size)
        for (index in 0 until maxLength) {
            val leftPart = leftVersion.mainParts.getOrElse(index) { 0 }
            val rightPart = rightVersion.mainParts.getOrElse(index) { 0 }
            if (leftPart != rightPart) return leftPart - rightPart
        }
        if (leftVersion.prereleaseParts.isEmpty() && rightVersion.prereleaseParts.isEmpty()) return 0
        if (leftVersion.prereleaseParts.isEmpty()) return 1
        if (rightVersion.prereleaseParts.isEmpty()) return -1
        val maxPrereleaseLength =
            maxOf(leftVersion.prereleaseParts.size, rightVersion.prereleaseParts.size)
        for (index in 0 until maxPrereleaseLength) {
            val leftPart = leftVersion.prereleaseParts.getOrNull(index) ?: return -1
            val rightPart = rightVersion.prereleaseParts.getOrNull(index) ?: return 1
            val comparison = comparePrereleasePart(leftPart, rightPart)
            if (comparison != 0) return comparison
        }
        return 0
    }

    fun isNewerVersion(candidate: String, current: String): Boolean =
        compareVersions(candidate, current) > 0
}
