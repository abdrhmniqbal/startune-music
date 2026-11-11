package com.startune.music.presentation.components

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.startune.music.R
import com.startune.music.presentation.jellyfin.auth.JellyfinLoginActivity
import com.startune.music.presentation.navidrome.auth.NavidromeLoginActivity
import com.startune.music.ui.theme.RoundedSans

/**
 * Bottom sheet that lets the user choose between streaming providers.
 * Uses a segmented Material 3 Expressive list that matches the other
 * bottom sheets in the app while keeping provider order and icon colors intact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamingProviderSheet(
    onDismissRequest: () -> Unit,
    isJellyfinLoggedIn: Boolean = false,
    onNavigateToJellyfinDashboard: () -> Unit = {},
    isNavidromeLoggedIn: Boolean = false,
    onNavigateToNavidromeDashboard: () -> Unit = {},
    sheetState: SheetState = rememberModalSheetState(
        skipPartiallyExpanded = true
    )
) {
    val context = LocalContext.current
    val providerSegmentContainerShape = RoundedCornerShape(20.dp)
    val providerSegmentItemShape = RoundedCornerShape(8.dp)

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.presentation_batch_g_streaming_title),
                style = MaterialTheme.typography.headlineSmall,
                fontFamily = RoundedSans,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.presentation_batch_g_streaming_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = RoundedSans,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = providerSegmentContainerShape,
                color = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                        .clip(providerSegmentContainerShape),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ProviderRow(
                        iconPainter = painterResource(R.drawable.ic_jellyfin),
                        iconTint = Color(0xFF00A4DC),
                        title = "Jellyfin",
                        subtitle = if (isJellyfinLoggedIn) stringResource(R.string.provider_connected) else stringResource(R.string.provider_connect_jellyfin),
                        shape = providerSegmentItemShape,
                        isConnected = isJellyfinLoggedIn,
                        onClick = {
                            if (isJellyfinLoggedIn) {
                                onNavigateToJellyfinDashboard()
                            } else {
                                context.startActivity(Intent(context, JellyfinLoginActivity::class.java))
                            }
                            onDismissRequest()
                        }
                    )
                    ProviderRow(
                        iconPainter = painterResource(R.drawable.ic_navidrome_md3),
                        iconTint = Color(0xFF1A9E75),
                        title = "Navidrome",
                        subtitle = if (isNavidromeLoggedIn) stringResource(R.string.provider_connected_navidrome) else stringResource(R.string.provider_connect_navidrome),
                        shape = providerSegmentItemShape,
                        isConnected = isNavidromeLoggedIn,
                        onClick = {
                            if (isNavidromeLoggedIn) {
                                onNavigateToNavidromeDashboard()
                            } else {
                                context.startActivity(Intent(context, NavidromeLoginActivity::class.java))
                            }
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}
