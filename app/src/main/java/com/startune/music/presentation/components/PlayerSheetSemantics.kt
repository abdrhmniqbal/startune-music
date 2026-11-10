package com.startune.music.presentation.components

import android.content.res.Resources
import androidx.annotation.StringRes
import com.startune.music.R
import com.startune.music.presentation.viewmodel.PlayerSheetState

/**
 * TalkBack description of the player sheet card, naming [songTitle] when there is one.
 *
 * Takes the caller's [resources] so it follows the in-app language. The English text starts with
 * "Startune player sheet collapsed/expanded".
 */
internal fun playerSheetContentDescription(
    resources: Resources,
    state: PlayerSheetState,
    songTitle: String?
): String {
    val title = songTitle?.takeIf { it.isNotBlank() }
    val descriptionRes = playerSheetContentDescriptionRes(state, hasTitle = title != null)
    return if (title != null) {
        resources.getString(descriptionRes, title)
    } else {
        resources.getString(descriptionRes)
    }
}

/** The variants with a title take it as their only format argument. */
@StringRes
internal fun playerSheetContentDescriptionRes(state: PlayerSheetState, hasTitle: Boolean): Int =
    when (state) {
        PlayerSheetState.COLLAPSED ->
            if (hasTitle) R.string.cd_player_sheet_collapsed else R.string.cd_player_sheet_collapsed_no_track
        PlayerSheetState.EXPANDED ->
            if (hasTitle) R.string.cd_player_sheet_expanded else R.string.cd_player_sheet_expanded_no_track
    }
