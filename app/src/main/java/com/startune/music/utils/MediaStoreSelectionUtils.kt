package com.startune.music.utils

import android.content.Context
import android.provider.MediaStore

/**
 * All MediaStore external volume names, primary first.
 *
 * A song's MediaStore _ID is unique across volumes (one `files` table, with a
 * `volume_name` column), but its content URI is per-volume. So every
 * MediaStore audio query must run once per volume and build the content URI
 * with that volume's name, or songs on an SD card / USB volume are invisible
 * to the library (the folders list showed internal storage only).
 */
fun audioVolumeNames(context: Context): List<String> {
    val names = try {
        MediaStore.getExternalVolumeNames(context)
    } catch (e: Exception) {
        emptySet()
    }
    val ordered = names.sortedWith(
        compareBy<String> { it != MediaStore.VOLUME_EXTERNAL_PRIMARY }.thenBy { it }
    )
    return if (ordered.isEmpty()) listOf(MediaStore.VOLUME_EXTERNAL_PRIMARY) else ordered
}

private val MIDI_MIME_SELECTION_ARGS = arrayOf(
    "audio/midi",
    "audio/x-midi",
    "audio/sp-midi",
    "audio/x-mid"
)
private val MIDI_EXTENSION_SELECTION_ARGS = arrayOf(
    "%.mid",
    "%.midi"
)

/**
 * Builds the baseline MediaStore selection for user-facing local audio.
 *
 * We intentionally do not rely on [MediaStore.Audio.Media.IS_MUSIC] here because some devices
 * and scanners leave valid songs flagged as non-music, which makes library sync and folder
 * browsing appear to "cap out" below the real file count for specific users.
 *
 * MIDI files may be indexed with incomplete duration metadata, so explicit MIDI MIME/path
 * matches bypass the duration floor and are left to playback capability checks.
 */
fun buildLocalAudioSelection(minDurationMs: Int): Pair<String, Array<String>> {
    val clampedMinDurationMs = minDurationMs.coerceAtLeast(0)
    val midiMimePlaceholders = MIDI_MIME_SELECTION_ARGS.joinToString(",") { "?" }
    val midiExtensionSelection = MIDI_EXTENSION_SELECTION_ARGS.joinToString(" OR ") {
        "LOWER(${MediaStore.Audio.Media.DATA}) LIKE ?"
    }
    val selection = buildString {
        append("(")
        append("${MediaStore.Audio.Media.DURATION} >= ?")
        append(" OR LOWER(COALESCE(${MediaStore.Audio.Media.MIME_TYPE}, '')) IN ($midiMimePlaceholders)")
        append(" OR $midiExtensionSelection")
        append(")")
        append(" AND COALESCE(${MediaStore.Audio.Media.TITLE}, '') != ''")
        append(" AND ${MediaStore.Audio.Media.DATA} IS NOT NULL")
    }
    return selection to arrayOf(clampedMinDurationMs.toString()) +
        MIDI_MIME_SELECTION_ARGS +
        MIDI_EXTENSION_SELECTION_ARGS
}
