@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.startune.music.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.startune.music.R
import com.startune.music.data.listenbrainz.ListenBrainzProfileStats
import com.startune.music.presentation.components.CollapsibleCommonTopBar
import com.startune.music.presentation.components.MiniPlayerHeight
import com.startune.music.presentation.jellyfin.auth.JellyfinLoginActivity
import com.startune.music.presentation.viewmodel.AccountsViewModel
import com.startune.music.presentation.viewmodel.ExternalAccountUiModel
import com.startune.music.presentation.viewmodel.ExternalServiceAccount
import com.startune.music.presentation.viewmodel.ListenBrainzConnectState
import com.startune.music.presentation.viewmodel.ListenBrainzStatsUiState
import com.startune.music.presentation.viewmodel.LastFmConnectState
import com.startune.music.presentation.viewmodel.LastFmUiModel
import com.startune.music.presentation.viewmodel.ListenBrainzUiModel
import java.text.NumberFormat
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import racra.compose.smooth_corner_rect_library.AbsoluteSmoothCornerShape
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AccountsScreen(
    onBackClick: () -> Unit,
    onOpenJellyfinDashboard: () -> Unit = {},
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listenBrainzConnectState by viewModel.listenBrainzConnectState.collectAsStateWithLifecycle()
    var showListenBrainzDialog by remember { mutableStateOf(false) }
    val lastFmConnectState by viewModel.lastFmConnectState.collectAsStateWithLifecycle()
    var showLastFmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(listenBrainzConnectState) {
        if (listenBrainzConnectState == ListenBrainzConnectState.Success) {
            showListenBrainzDialog = false
            viewModel.resetListenBrainzConnectState()
        }
    }

    LaunchedEffect(lastFmConnectState) {
        if (lastFmConnectState == LastFmConnectState.Success) {
            showLastFmDialog = false
            viewModel.resetLastFmConnectState()
        }
    }

    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()

    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val minTopBarHeight = 64.dp + statusBarHeight
    val maxTopBarHeight = 180.dp
    val minTopBarHeightPx = with(density) { minTopBarHeight.toPx() }
    val maxTopBarHeightPx = with(density) { maxTopBarHeight.toPx() }
    val topBarHeight = remember { Animatable(maxTopBarHeightPx) }
    var collapseFraction by remember { mutableStateOf(0f) }

    LaunchedEffect(topBarHeight.value) {
        collapseFraction =
            1f - (
                (topBarHeight.value - minTopBarHeightPx) /
                    (maxTopBarHeightPx - minTopBarHeightPx)
                ).coerceIn(0f, 1f)
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val isScrollingDown = delta < 0

                if (!isScrollingDown &&
                    (
                        lazyListState.firstVisibleItemIndex > 0 ||
                            lazyListState.firstVisibleItemScrollOffset > 0
                        )
                ) {
                    return Offset.Zero
                }

                val previousHeight = topBarHeight.value
                val newHeight = (previousHeight + delta).coerceIn(minTopBarHeightPx, maxTopBarHeightPx)
                val consumed = newHeight - previousHeight

                if (consumed.roundToInt() != 0) {
                    coroutineScope.launch { topBarHeight.snapTo(newHeight) }
                }

                val canConsumeScroll = !(isScrollingDown && newHeight == minTopBarHeightPx)
                return if (canConsumeScroll) Offset(0f, consumed) else Offset.Zero
            }
        }
    }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (!lazyListState.isScrollInProgress) {
            val shouldExpand = topBarHeight.value > (minTopBarHeightPx + maxTopBarHeightPx) / 2
            val canExpand =
                lazyListState.firstVisibleItemIndex == 0 &&
                    lazyListState.firstVisibleItemScrollOffset == 0
            val targetValue = if (shouldExpand && canExpand) maxTopBarHeightPx else minTopBarHeightPx
            if (topBarHeight.value != targetValue) {
                coroutineScope.launch {
                    topBarHeight.animateTo(targetValue, spring(stiffness = Spring.StiffnessMedium))
                }
            }
        }
    }

    val onConnectService: (ExternalServiceAccount) -> Unit = { service ->
        if (service == ExternalServiceAccount.LISTENBRAINZ) {
            showListenBrainzDialog = true
        } else if (service == ExternalServiceAccount.LASTFM) {
            showLastFmDialog = true
        } else {
            openService(
                context = context,
                service = service,
                onOpenJellyfinDashboard = onOpenJellyfinDashboard,
                preferDashboard = false
            )
        }
    }

    Box(modifier = Modifier.nestedScroll(nestedScrollConnection).fillMaxSize()) {
        val currentTopBarHeightDp = with(density) { topBarHeight.value.toDp() }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = currentTopBarHeightDp + 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = MiniPlayerHeight + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (uiState.connectedAccounts.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.presentation_batch_b_accounts_linked_services),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                items(
                    items = uiState.connectedAccounts,
                    key = { it.service.name }
                ) { account ->
                    ConnectedAccountCard(
                        account = account,
                        onManage = {
                            openService(
                                context = context,
                                service = account.service,
                                onOpenJellyfinDashboard = onOpenJellyfinDashboard,
                                preferDashboard = true
                            )
                        },
                        onLogout = { viewModel.logout(account.service) },
                        painter = if (account.service == ExternalServiceAccount.JELLYFIN) {
                            painterResource(R.drawable.ic_jellyfin)
                        } else null,
                        extraContent = when (account.service) {
                            ExternalServiceAccount.LISTENBRAINZ -> {
                                {
                                    uiState.listenBrainz?.let { listenBrainz ->
                                        ListenBrainzCardExtras(
                                            model = listenBrainz,
                                            onToggleLocal = viewModel::setListenBrainzScrobbleLocal,
                                            onToggleJellyfin = viewModel::setListenBrainzScrobbleJellyfin,
                                            onReconnect = { showListenBrainzDialog = true }
                                        )
                                    }
                                }
                            }
                            ExternalServiceAccount.LASTFM -> {
                                {
                                    uiState.lastFm?.let { lastFm ->
                                        LastFmCardExtras(
                                            model = lastFm,
                                            onToggleScrobble = viewModel::setLastFmScrobbleEnabled,
                                            onToggleNowPlaying = viewModel::setLastFmNowPlayingEnabled,
                                            onThresholdsChanged = viewModel::setLastFmScrobbleThresholds,
                                            onReconnect = { showLastFmDialog = true }
                                        )
                                    }
                                }
                            }
                            else -> null
                        }
                    )
                }

                if (uiState.disconnectedServices.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.accounts_available_services),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                    item {
                        AvailableServicesCard(
                            disconnectedServices = uiState.disconnectedServices,
                            onConnect = onConnectService
                        )
                    }
                }
            } else {
                item {
                    EmptyAccountsCard(
                        disconnectedServices = uiState.disconnectedServices,
                        onConnect = onConnectService
                    )
                }
            }
        }

        CollapsibleCommonTopBar(
            title = stringResource(R.string.settings_accounts_row_title),
            collapseFraction = collapseFraction,
            headerHeight = currentTopBarHeightDp,
            onBackClick = onBackClick,
            expandedTitleStartPadding = 20.dp,
            collapsedTitleStartPadding = 68.dp
        )

        if (showListenBrainzDialog) {
            ListenBrainzTokenDialog(
                connectState = listenBrainzConnectState,
                initialServerUrl = uiState.listenBrainz?.serverUrl,
                onConnect = viewModel::connectListenBrainz,
                onDismiss = {
                    showListenBrainzDialog = false
                    viewModel.resetListenBrainzConnectState()
                }
            )
        }

        if (showLastFmDialog) {
            LastFmLoginDialog(
                connectState = lastFmConnectState,
                onConnect = viewModel::connectLastFm,
                onDismiss = {
                    showLastFmDialog = false
                    viewModel.resetLastFmConnectState()
                }
            )
        }
    }
}

@Composable
private fun StatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ConnectedAccountCard(
    account: ExternalAccountUiModel,
    onManage: () -> Unit,
    onLogout: () -> Unit,
    painter: androidx.compose.ui.graphics.painter.Painter? = null,
    extraContent: (@Composable () -> Unit)? = null
) {
    val statusConnected = stringResource(R.string.presentation_batch_b_accounts_status_connected)
    val openService = stringResource(R.string.presentation_batch_b_accounts_open_service)
    val loggingOut = stringResource(R.string.presentation_batch_b_accounts_logging_out)
    val logOut = stringResource(R.string.cd_logout)
    val palette = servicePalette(account.service)
    val cardShape = AbsoluteSmoothCornerShape(28.dp, 60)

    Card(
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically
) {
        Surface(
            shape = AbsoluteSmoothCornerShape(16.dp, 60),
            color = palette.iconContainer
        ) {
            if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = palette.iconTint,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp)
                )
            } else {
                ServiceIcon(
                    service = account.service,
                    tint = palette.iconTint,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(20.dp)
                )
            }
        }

    Spacer(Modifier.size(12.dp))

    Column(modifier = Modifier.weight(1f)) {
        Text(
            text = account.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = account.accountLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }

    Surface(
        shape = AbsoluteSmoothCornerShape(12.dp, 60),
        color = palette.statusContainer
    ) {
        Text(
            text = statusConnected,
            style = MaterialTheme.typography.labelMedium,
            color = palette.statusTint,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

            Surface(
                shape = AbsoluteSmoothCornerShape(14.dp, 60),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Sync,
                        contentDescription = null,
                        tint = palette.iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = account.syncedContentLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            extraContent?.invoke()

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))

            FilledTonalButton(
                onClick = onManage,
                enabled = !account.isLoggingOut,
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = palette.primaryActionContainer,
                    contentColor = palette.primaryActionTint,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = openService,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = onLogout,
                enabled = !account.isLoggingOut,
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                border = BorderStroke(1.dp, palette.primaryActionTint.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                if (account.isLoggingOut) {
                    LoadingIndicator(
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Logout,
                        contentDescription = null
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (account.isLoggingOut) loggingOut else logOut,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun EmptyAccountsCard(
    disconnectedServices: ImmutableList<ExternalServiceAccount>,
    onConnect: (ExternalServiceAccount) -> Unit
) {
    val noLinkedTitle = stringResource(R.string.presentation_batch_b_accounts_no_linked_title)
    val noLinkedBody = stringResource(R.string.presentation_batch_b_accounts_no_linked_body)
    Card(
        shape = AbsoluteSmoothCornerShape(28.dp, 60),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = noLinkedTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = noLinkedBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            disconnectedServices.forEach { service ->
                ConnectServiceButton(service = service, onConnect = onConnect)
            }
        }
    }
}

private data class ServicePalette(
    val iconContainer: Color,
    val iconTint: Color,
    val statusContainer: Color,
    val statusTint: Color,
    val primaryActionContainer: Color,
    val primaryActionTint: Color
)

@Composable
private fun servicePalette(service: ExternalServiceAccount): ServicePalette {
    return when (service) {
        ExternalServiceAccount.JELLYFIN -> ServicePalette(
            iconContainer = Color(0xFF00A4DC),
            iconTint = Color.White,
            statusContainer = Color(0xFFE1F5FE),
            statusTint = Color(0xFF0277BD),
            primaryActionContainer = Color(0xFFE3F2FD),
            primaryActionTint = Color(0xFF1565C0)
        )
        ExternalServiceAccount.LISTENBRAINZ -> ServicePalette(
            iconContainer = Color(0xFFEB743B),
            iconTint = Color.White,
            statusContainer = Color(0xFFFFE8DC),
            statusTint = Color(0xFFA84A17),
            primaryActionContainer = Color(0xFFFFE3D3),
            primaryActionTint = Color(0xFFA84A17)
        )
        ExternalServiceAccount.LASTFM -> ServicePalette(
            iconContainer = Color(0xFFB90000),
            iconTint = Color.White,
            statusContainer = Color(0xFFFDE8E8),
            statusTint = Color(0xFF8F0000),
            primaryActionContainer = Color(0xFFFDECEC),
            primaryActionTint = Color(0xFF8F0000)
        )
    }
}

private fun accountIcon(service: ExternalServiceAccount): ImageVector {
    return when (service) {
        ExternalServiceAccount.JELLYFIN -> Icons.Rounded.CloudQueue
        ExternalServiceAccount.LISTENBRAINZ -> Icons.Rounded.GraphicEq
        ExternalServiceAccount.LASTFM -> Icons.Filled.MusicNote
    }
}

@Composable
private fun ServiceIcon(service: ExternalServiceAccount, tint: Color, modifier: Modifier = Modifier) {
    if (service == ExternalServiceAccount.JELLYFIN) {
        Icon(
            painter = painterResource(R.drawable.ic_jellyfin),
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = accountIcon(service),
            contentDescription = null,
            tint = tint,
            modifier = modifier
        )
    }
}

@Composable
private fun serviceDisplayName(service: ExternalServiceAccount): String {
    return when (service) {
        ExternalServiceAccount.JELLYFIN -> stringResource(R.string.auth_jellyfin_title)
        ExternalServiceAccount.LISTENBRAINZ -> stringResource(R.string.accounts_listenbrainz_title)
        ExternalServiceAccount.LASTFM -> stringResource(R.string.lastfm_integration)
    }
}

private fun openService(
    context: Context,
    service: ExternalServiceAccount,
    onOpenJellyfinDashboard: () -> Unit,
    preferDashboard: Boolean
) {
    when (service) {
        ExternalServiceAccount.JELLYFIN -> {
            if (preferDashboard) {
                onOpenJellyfinDashboard()
            } else {
                safeStartActivity(
                    context = context,
                    intent = Intent(context, JellyfinLoginActivity::class.java)
                )
            }
        }
        ExternalServiceAccount.LISTENBRAINZ -> {
            safeStartActivity(
                context = context,
                intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://listenbrainz.org"))
            )
        }
        ExternalServiceAccount.LASTFM -> {
            safeStartActivity(
                context = context,
                intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.last.fm"))
            )
        }
    }
}

private fun safeStartActivity(
    context: Context,
    intent: Intent
) {
    runCatching { context.startActivity(intent) }
        .onFailure {
            Toast.makeText(context, context.getString(R.string.accounts_unable_open_screen), Toast.LENGTH_SHORT).show()
        }
}

@Composable
private fun AvailableServicesCard(
    disconnectedServices: ImmutableList<ExternalServiceAccount>,
    onConnect: (ExternalServiceAccount) -> Unit
) {
    Card(
        shape = AbsoluteSmoothCornerShape(28.dp, 60),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            disconnectedServices.forEach { service ->
                ConnectServiceButton(service = service, onConnect = onConnect)
            }
        }
    }
}

@Composable
private fun ConnectServiceButton(
    service: ExternalServiceAccount,
    onConnect: (ExternalServiceAccount) -> Unit
) {
    val connectTemplate = stringResource(R.string.presentation_batch_b_accounts_connect_service)
    FilledTonalButton(
        onClick = { onConnect(service) },
        shape = AbsoluteSmoothCornerShape(18.dp, 60),
        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        when (service) {
            ExternalServiceAccount.JELLYFIN -> Icon(
                painter = painterResource(R.drawable.ic_jellyfin),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            ExternalServiceAccount.LISTENBRAINZ -> Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            ExternalServiceAccount.LASTFM -> Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = connectTemplate.format(serviceDisplayName(service))
        )
    }
}

@Composable
private fun ListenBrainzCardExtras(
    model: ListenBrainzUiModel,
    onToggleLocal: (Boolean) -> Unit,
    onToggleJellyfin: (Boolean) -> Unit,
    onReconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (model.statsState != ListenBrainzStatsUiState.Unavailable) {
            Crossfade(targetState = model.statsState, label = "listenBrainzStats") { state ->
                when (state) {
                    ListenBrainzStatsUiState.Loading -> ListenBrainzStatsSkeleton()
                    is ListenBrainzStatsUiState.Loaded -> ListenBrainzStatsContent(
                        stats = state.stats,
                        pendingCount = model.pendingCount
                    )
                    ListenBrainzStatsUiState.Unavailable -> {}
                }
            }
        }
        if (model.needsReauth) {
            Surface(
                shape = AbsoluteSmoothCornerShape(14.dp, 60),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.accounts_listenbrainz_needs_reauth),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onReconnect) {
                        Text(
                            text = stringResource(R.string.accounts_listenbrainz_reconnect),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        ScrobbleSourceToggleRow(
            label = stringResource(R.string.accounts_listenbrainz_scrobble_local),
            checked = model.scrobbleLocal,
            onCheckedChange = onToggleLocal
        )
        ScrobbleSourceToggleRow(
            label = stringResource(R.string.accounts_listenbrainz_scrobble_jellyfin),
            checked = model.scrobbleJellyfin,
            onCheckedChange = onToggleJellyfin
        )
        Text(
            text = stringResource(R.string.accounts_listenbrainz_toggle_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        model.serverUrl?.let { serverUrl ->
            Text(
                text = stringResource(R.string.accounts_listenbrainz_custom_server, serverUrl),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ListenBrainzStatsContent(
    stats: ListenBrainzProfileStats,
    pendingCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            stats.listenCount?.let { count ->
                StatTile(
                    title = stringResource(R.string.accounts_listenbrainz_stat_listens),
                    value = NumberFormat.getIntegerInstance().format(count),
                    modifier = Modifier.weight(1f)
                )
            }
            StatTile(
                title = stringResource(R.string.accounts_listenbrainz_stat_queued),
                value = pendingCount.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        if (stats.playingNowAvailable) {
            ListenBrainzNowPlayingRow(
                track = stats.nowPlayingTrack,
                artist = stats.nowPlayingArtist
            )
        }
    }
}

@Composable
private fun ListenBrainzStatsSkeleton() {
    val shimmer = rememberInfiniteTransition(label = "lbSkeleton")
    val progress = shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = LinearEasing)),
        label = "lbSkeletonSweep"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonBlock(
                progress = progress,
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                modifier = Modifier.weight(1f).height(70.dp)
            )
            SkeletonBlock(
                progress = progress,
                shape = AbsoluteSmoothCornerShape(18.dp, 60),
                modifier = Modifier.weight(1f).height(70.dp)
            )
        }
        SkeletonBlock(
            progress = progress,
            shape = AbsoluteSmoothCornerShape(14.dp, 60),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        )
    }
}

@Composable
private fun SkeletonBlock(
    progress: State<Float>,
    shape: Shape,
    modifier: Modifier = Modifier
) {
    val baseColor = MaterialTheme.colorScheme.surfaceContainerLow
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                drawRect(baseColor)
                val sweepCenter = size.width * (progress.value * 2f - 0.5f)
                val sweepRadius = size.width * 0.35f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, highlightColor, Color.Transparent),
                        start = Offset(sweepCenter - sweepRadius, 0f),
                        end = Offset(sweepCenter + sweepRadius, size.height)
                    )
                )
            }
    )
}

@Composable
private fun ListenBrainzNowPlayingRow(
    track: String?,
    artist: String?
) {
    val isPlaying = track != null
    val containerColor = if (isPlaying) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val contentColor = if (isPlaying) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(
        shape = AbsoluteSmoothCornerShape(14.dp, 60),
        color = containerColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconModifier = if (isPlaying) {
                val pulse = rememberInfiniteTransition(label = "scrobblePulse")
                val pulseAlpha by pulse.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
                    label = "scrobblePulseAlpha"
                )
                Modifier.size(16.dp).graphicsLayer { alpha = pulseAlpha }
            } else {
                Modifier.size(16.dp)
            }
            Icon(
                imageVector = Icons.Rounded.GraphicEq,
                contentDescription = null,
                tint = contentColor,
                modifier = iconModifier
            )
            Spacer(modifier = Modifier.size(8.dp))
            if (isPlaying) {
                Column {
                    Text(
                        text = stringResource(R.string.accounts_listenbrainz_now_playing_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor
                    )
                    Text(
                        text = if (artist.isNullOrBlank()) {
                            track.orEmpty()
                        } else {
                            stringResource(
                                R.string.accounts_listenbrainz_now_playing_format,
                                track.orEmpty(),
                                artist
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.accounts_listenbrainz_now_playing_idle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
private fun ScrobbleSourceToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ListenBrainzTokenDialog(
    connectState: ListenBrainzConnectState,
    initialServerUrl: String?,
    onConnect: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var token by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf(initialServerUrl.orEmpty()) }
    val failedState = connectState as? ListenBrainzConnectState.Failed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.accounts_listenbrainz_token_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.accounts_listenbrainz_token_dialog_body),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text(stringResource(R.string.accounts_listenbrainz_token_hint)) },
                    singleLine = true,
                    isError = failedState != null && !failedState.invalidUrl,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it },
                    label = { Text(stringResource(R.string.accounts_listenbrainz_server_hint)) },
                    supportingText = {
                        Text(stringResource(R.string.accounts_listenbrainz_server_supporting))
                    },
                    singleLine = true,
                    isError = failedState?.invalidUrl == true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth()
                )
                if (failedState != null) {
                    Text(
                        text = stringResource(
                            if (failedState.invalidUrl) {
                                R.string.accounts_listenbrainz_invalid_url
                            } else {
                                R.string.accounts_listenbrainz_connect_failed
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(
                    onClick = {
                        safeStartActivity(
                            context = context,
                            intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://listenbrainz.org/settings/"))
                        )
                    }
                ) {
                    Text(stringResource(R.string.accounts_listenbrainz_get_token))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(token, serverUrl) },
                enabled = token.isNotBlank() && connectState != ListenBrainzConnectState.Connecting
            ) {
                if (connectState == ListenBrainzConnectState.Connecting) {
                    LoadingIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(R.string.accounts_listenbrainz_connect))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}


@Composable
private fun LastFmCardExtras(
    model: LastFmUiModel,
    onToggleScrobble: (Boolean) -> Unit,
    onToggleNowPlaying: (Boolean) -> Unit,
    onThresholdsChanged: (Int, Int, Int) -> Unit,
    onReconnect: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (model.needsReauth) {
            Surface(
                shape = AbsoluteSmoothCornerShape(14.dp, 60),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Text(
                        text = stringResource(R.string.lastfm_not_logged_in),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    TextButton(onClick = onReconnect) {
                        Text(
                            text = stringResource(R.string.lastfm_action_login),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        ScrobbleSourceToggleRow(
            label = stringResource(R.string.lastfm_enable_scrobbling),
            checked = model.scrobbleEnabled,
            onCheckedChange = onToggleScrobble
        )
        Text(
            text = stringResource(R.string.lastfm_enable_scrobbling_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        ScrobbleSourceToggleRow(
            label = stringResource(R.string.lastfm_now_playing),
            checked = model.nowPlayingEnabled,
            onCheckedChange = onToggleNowPlaying
        )
        Text(
            text = stringResource(R.string.lastfm_now_playing_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.lastfm_scrobbling_configuration),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        LastFmThresholdSlider(
            label = stringResource(
                R.string.lastfm_scrobble_min_track_duration,
                model.minimumTrackDurationSeconds
            ),
            value = model.minimumTrackDurationSeconds,
            valueRange = 0f..120f,
            steps = 23,
            onValueChangeFinished = {
                onThresholdsChanged(it, model.scrobbleDelayPercent, model.maxScrobbleDelaySeconds)
            }
        )
        LastFmThresholdSlider(
            label = stringResource(
                R.string.lastfm_scrobble_delay_percent,
                model.scrobbleDelayPercent
            ),
            value = model.scrobbleDelayPercent,
            valueRange = 5f..100f,
            steps = 18,
            onValueChangeFinished = {
                onThresholdsChanged(model.minimumTrackDurationSeconds, it, model.maxScrobbleDelaySeconds)
            }
        )
        LastFmThresholdSlider(
            label = stringResource(
                R.string.lastfm_scrobble_delay_seconds,
                model.maxScrobbleDelaySeconds
            ),
            value = model.maxScrobbleDelaySeconds,
            valueRange = 30f..600f,
            steps = 18,
            onValueChangeFinished = {
                onThresholdsChanged(model.minimumTrackDurationSeconds, model.scrobbleDelayPercent, it)
            }
        )
    }
}

@Composable
private fun LastFmThresholdSlider(
    label: String,
    value: Int,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (Int) -> Unit
) {
    var sliderValue by remember(value) { mutableStateOf(value.toFloat()) }
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = { onValueChangeFinished(sliderValue.roundToInt()) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun LastFmLoginDialog(
    connectState: LastFmConnectState,
    onConnect: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val failedState = connectState as? LastFmConnectState.Failed

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.lastfm_login_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.lastfm_login_dialog_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.lastfm_username)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.lastfm_password)) },
                    singleLine = true,
                    isError = failedState != null,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                if (failedState != null) {
                    Text(
                        text = stringResource(
                            R.string.lastfm_login_failed,
                            if (failedState.invalidCredentials) {
                                "invalid username or password"
                            } else {
                                "network error, try again"
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConnect(username, password) },
                enabled = username.isNotBlank() && password.isNotBlank() &&
                    connectState != LastFmConnectState.Connecting
            ) {
                if (connectState == LastFmConnectState.Connecting) {
                    LoadingIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text(stringResource(R.string.lastfm_action_login))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
