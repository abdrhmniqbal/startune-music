package com.startune.music.presentation.components.scoped

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import com.startune.music.presentation.navigation.Screen
import com.startune.music.presentation.navigation.navigateSafelyReplacing
import com.startune.music.presentation.viewmodel.PlayerViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun PlayerPlaylistNavigationEffect(
    navController: NavHostController,
    sheetCollapsedTargetY: Float,
    sheetMotionController: SheetMotionController,
    playerViewModel: PlayerViewModel
) {
    val latestSheetCollapsedTargetY by rememberUpdatedState(sheetCollapsedTargetY)
    LaunchedEffect(navController) {
        playerViewModel.playlistNavigationRequests.collectLatest { playlistId ->
            sheetMotionController.snapCollapsed(latestSheetCollapsedTargetY)
            playerViewModel.collapsePlayerSheet()

            navController.navigateSafelyReplacing(
                route = Screen.PlaylistDetail.createRoute(playlistId),
                patternToPop = Screen.PlaylistDetail.route
            )
        }
    }
}
