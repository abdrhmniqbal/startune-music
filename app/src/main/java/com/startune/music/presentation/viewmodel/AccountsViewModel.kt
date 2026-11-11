package com.startune.music.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.jellyfin.JellyfinRepository
import com.startune.music.data.listenbrainz.InvalidServerUrlException
import com.startune.music.data.listenbrainz.ListenBrainzProfileStats
import com.startune.music.data.lastfm.LastFmLoginResult
import com.startune.music.data.lastfm.LastFmRepository
import com.startune.music.data.listenbrainz.ListenBrainzRepository
import com.startune.music.data.navidrome.NavidromeRepository
import com.startune.music.data.preferences.ListenBrainzPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private const val PROFILE_STATS_POLL_MS = 20_000L

enum class ExternalServiceAccount {
    JELLYFIN,
    NAVIDROME,
    LISTENBRAINZ,
    LASTFM
}

data class ExternalAccountUiModel(
    val service: ExternalServiceAccount,
    val title: String,
    val accountLabel: String,
    val syncedContentLabel: String,
    val isLoggingOut: Boolean
)

data class ListenBrainzUiModel(
    val userName: String?,
    val pendingCount: Int,
    val needsReauth: Boolean,
    val serverUrl: String?,
    val statsState: ListenBrainzStatsUiState,
    val scrobbleLocal: Boolean,
    val scrobbleJellyfin: Boolean
)

sealed interface ListenBrainzStatsUiState {
    /** First fetch is in flight — show skeleton placeholders. */
    data object Loading : ListenBrainzStatsUiState

    /** The connected server doesn't expose profile endpoints (Maloja) — show nothing. */
    data object Unavailable : ListenBrainzStatsUiState

    data class Loaded(val stats: ListenBrainzProfileStats) : ListenBrainzStatsUiState
}

sealed interface ListenBrainzConnectState {
    data object Idle : ListenBrainzConnectState
    data object Connecting : ListenBrainzConnectState
    data object Success : ListenBrainzConnectState
    data class Failed(val invalidUrl: Boolean = false) : ListenBrainzConnectState
}

data class LastFmUiModel(
    val userName: String?,
    val pendingCount: Int,
    val needsReauth: Boolean,
    val scrobbleEnabled: Boolean,
    val nowPlayingEnabled: Boolean,
    val minimumTrackDurationSeconds: Int,
    val maxScrobbleDelaySeconds: Int
)

sealed interface LastFmConnectState {
    data object Idle : LastFmConnectState
    data object Connecting : LastFmConnectState
    data object Success : LastFmConnectState
    data class Failed(val invalidCredentials: Boolean = false) : LastFmConnectState
}

data class AccountsUiState(
    val connectedAccounts: List<ExternalAccountUiModel> = emptyList(),
    val disconnectedServices: ImmutableList<ExternalServiceAccount> = persistentListOf(),
    val listenBrainz: ListenBrainzUiModel? = null,
    val lastFm: LastFmUiModel? = null
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val jellyfinRepository: JellyfinRepository,
    private val navidromeRepository: NavidromeRepository,
    private val listenBrainzRepository: ListenBrainzRepository,
    private val listenBrainzPreferences: ListenBrainzPreferencesRepository,
    private val lastFmRepository: LastFmRepository
) : ViewModel() {

    private val loggingOutServices = MutableStateFlow<Set<ExternalServiceAccount>>(emptySet())

    private val jellyfinStateFlow = combine(
        jellyfinRepository.isLoggedInFlow,
        jellyfinRepository.getPlaylists().map { it.size }
    ) { connected, playlistCount ->
        connected to playlistCount
    }

    private val navidromeStateFlow = combine(
        navidromeRepository.isLoggedInFlow,
        navidromeRepository.getPlaylists().map { it.size }
    ) { connected, playlistCount ->
        connected to playlistCount
    }

    /**
     * Re-fetched on a fixed cadence, but only while something downstream collects [uiState],
     * so the polling stops as soon as the Accounts screen goes away.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val listenBrainzStatsFlow = listenBrainzRepository.accountState
        .map { it.isConnected to it.userName }
        .distinctUntilChanged()
        .flatMapLatest { (connected, userName) ->
            if (!connected || userName.isNullOrBlank()) {
                flowOf(ListenBrainzStatsUiState.Unavailable)
            } else {
                flow<ListenBrainzStatsUiState> {
                    emit(ListenBrainzStatsUiState.Loading)
                    var lastLoaded: ListenBrainzProfileStats? = null
                    while (true) {
                        // A transient poll failure keeps showing the last good numbers
                        // instead of collapsing the section.
                        val stats = listenBrainzRepository.fetchProfileStats() ?: lastLoaded
                        if (stats != null) {
                            lastLoaded = stats
                            emit(ListenBrainzStatsUiState.Loaded(stats))
                        } else {
                            emit(ListenBrainzStatsUiState.Unavailable)
                        }
                        delay(PROFILE_STATS_POLL_MS)
                    }
                }
            }
        }

    private val listenBrainzStateFlow = combine(
        listenBrainzRepository.accountState,
        listenBrainzRepository.pendingListenCount,
        listenBrainzStatsFlow,
        listenBrainzPreferences.scrobbleLocalFlow,
        listenBrainzPreferences.scrobbleJellyfinFlow
    ) { account, pendingCount, statsState, local, scrobbleJellyfin ->
        if (account.isConnected) {
            ListenBrainzUiModel(
                userName = account.userName,
                pendingCount = pendingCount,
                needsReauth = account.needsReauth,
                serverUrl = account.serverUrl,
                statsState = statsState,
                scrobbleLocal = local,
                scrobbleJellyfin = scrobbleJellyfin
            )
        } else {
            null
        }
    }

    private val _listenBrainzConnectState =
        MutableStateFlow<ListenBrainzConnectState>(ListenBrainzConnectState.Idle)
    val listenBrainzConnectState: StateFlow<ListenBrainzConnectState> =
        _listenBrainzConnectState.asStateFlow()

    private val lastFmStateFlow = combine(
        lastFmRepository.accountState,
        lastFmRepository.pendingScrobbleCount
    ) { account, pendingCount ->
        if (account.isConnected) {
            LastFmUiModel(
                userName = account.userName,
                pendingCount = pendingCount,
                needsReauth = account.needsReauth,
                scrobbleEnabled = account.config.isEnabled,
                nowPlayingEnabled = account.config.nowPlayingEnabled,
                minimumTrackDurationSeconds = account.config.minimumTrackDurationSeconds,
                maxScrobbleDelaySeconds = account.config.maxScrobbleDelaySeconds
            )
        } else {
            null
        }
    }

    private val _lastFmConnectState =
        MutableStateFlow<LastFmConnectState>(LastFmConnectState.Idle)
    val lastFmConnectState: StateFlow<LastFmConnectState> =
        _lastFmConnectState.asStateFlow()

    val uiState: StateFlow<AccountsUiState> = combine(
        combine(
            listOf(
                jellyfinStateFlow,
                navidromeStateFlow
            )
        ) { it.toList() },
        listenBrainzStateFlow,
        lastFmStateFlow,
        loggingOutServices
    ) { states, listenBrainz, lastFm, activeLogouts ->
        val (jellyfinConnected, jellyfinPlaylistCount) = states[0]
        val (navidromeConnected, navidromePlaylistCount) = states[1]

        val connectedAccounts = buildList {
            if (jellyfinConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.JELLYFIN,
                        title = "Jellyfin",
                        accountLabel = jellyfinRepository.username
                            ?.takeIf { it.isNotBlank() }
                            ?: "Jellyfin account connected",
                        syncedContentLabel = formatCount(
                            count = jellyfinPlaylistCount,
                            singular = "synced playlist",
                            plural = "synced playlists"
                        ),
                        isLoggingOut = ExternalServiceAccount.JELLYFIN in activeLogouts
                    )
                )
            }
            if (navidromeConnected) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.NAVIDROME,
                        title = "Navidrome",
                        accountLabel = navidromeRepository.username
                            ?.takeIf { it.isNotBlank() }
                            ?: "Navidrome account connected",
                        syncedContentLabel = formatCount(
                            count = navidromePlaylistCount,
                            singular = "synced playlist",
                            plural = "synced playlists"
                        ),
                        isLoggingOut = ExternalServiceAccount.NAVIDROME in activeLogouts
                    )
                )
            }
            if (listenBrainz != null) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.LISTENBRAINZ,
                        title = "ListenBrainz",
                        accountLabel = listenBrainz.userName
                            ?.takeIf { it.isNotBlank() }
                            ?: "ListenBrainz account connected",
                        syncedContentLabel = if (listenBrainz.pendingCount > 0) {
                            formatCount(
                                count = listenBrainz.pendingCount,
                                singular = "queued listen",
                                plural = "queued listens"
                            )
                        } else {
                            "Scrobbling listens as you play"
                        },
                        isLoggingOut = ExternalServiceAccount.LISTENBRAINZ in activeLogouts
                    )
                )
            }
            if (lastFm != null) {
                add(
                    ExternalAccountUiModel(
                        service = ExternalServiceAccount.LASTFM,
                        title = "Last.fm",
                        accountLabel = lastFm.userName
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "Logged in as $it" }
                            ?: "Last.fm account connected",
                        syncedContentLabel = if (lastFm.pendingCount > 0) {
                            formatCount(
                                count = lastFm.pendingCount,
                                singular = "queued scrobble",
                                plural = "queued scrobbles"
                            )
                        } else {
                            "Scrobbling listens as you play"
                        },
                        isLoggingOut = ExternalServiceAccount.LASTFM in activeLogouts
                    )
                )
            }
        }

        val disconnectedServices = buildList {
            if (!jellyfinConnected) add(ExternalServiceAccount.JELLYFIN)
            if (!navidromeConnected) add(ExternalServiceAccount.NAVIDROME)
            if (listenBrainz == null) add(ExternalServiceAccount.LISTENBRAINZ)
            if (lastFm == null) add(ExternalServiceAccount.LASTFM)
        }

        AccountsUiState(
            connectedAccounts = connectedAccounts,
            disconnectedServices = disconnectedServices.toImmutableList(),
            listenBrainz = listenBrainz,
            lastFm = lastFm
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AccountsUiState())

    fun logout(service: ExternalServiceAccount) {
        if (service in loggingOutServices.value) return

        viewModelScope.launch {
            loggingOutServices.update { it + service }
            try {
                runCatching {
                    when (service) {
                        ExternalServiceAccount.JELLYFIN -> jellyfinRepository.logout()
                        ExternalServiceAccount.NAVIDROME -> navidromeRepository.logout()
                        ExternalServiceAccount.LISTENBRAINZ -> listenBrainzRepository.disconnect()
                        ExternalServiceAccount.LASTFM -> lastFmRepository.disconnect()
                    }
                }
            } finally {
                loggingOutServices.update { it - service }
            }
        }
    }

    fun connectListenBrainz(token: String, serverUrl: String) {
        if (_listenBrainzConnectState.value == ListenBrainzConnectState.Connecting) return
        viewModelScope.launch {
            _listenBrainzConnectState.value = ListenBrainzConnectState.Connecting
            val result = listenBrainzRepository.connect(token, serverUrl)
            _listenBrainzConnectState.value = if (result.isSuccess) {
                ListenBrainzConnectState.Success
            } else {
                ListenBrainzConnectState.Failed(
                    invalidUrl = result.exceptionOrNull() is InvalidServerUrlException
                )
            }
        }
    }

    fun resetListenBrainzConnectState() {
        _listenBrainzConnectState.value = ListenBrainzConnectState.Idle
    }

    fun connectLastFm(username: String, password: String) {
        if (_lastFmConnectState.value == LastFmConnectState.Connecting) return
        viewModelScope.launch {
            _lastFmConnectState.value = LastFmConnectState.Connecting
            _lastFmConnectState.value = when (lastFmRepository.login(username, password)) {
                is LastFmLoginResult.Success -> LastFmConnectState.Success
                LastFmLoginResult.InvalidCredentials -> LastFmConnectState.Failed(invalidCredentials = true)
                LastFmLoginResult.TransientError -> LastFmConnectState.Failed(invalidCredentials = false)
            }
        }
    }

    fun resetLastFmConnectState() {
        _lastFmConnectState.value = LastFmConnectState.Idle
    }

    fun setLastFmScrobbleEnabled(enabled: Boolean) {
        lastFmRepository.setScrobbleEnabled(enabled)
    }

    fun setLastFmNowPlayingEnabled(enabled: Boolean) {
        lastFmRepository.setNowPlayingEnabled(enabled)
    }

    fun setLastFmScrobbleThresholds(
        minimumTrackDurationSeconds: Int,
        maxScrobbleDelaySeconds: Int
    ) {
        lastFmRepository.setScrobbleThresholds(
            minimumTrackDurationSeconds,
            maxScrobbleDelaySeconds
        )
    }

    fun setListenBrainzScrobbleLocal(enabled: Boolean) {
        viewModelScope.launch { listenBrainzPreferences.setScrobbleLocal(enabled) }
    }

    fun setListenBrainzScrobbleJellyfin(enabled: Boolean) {
        viewModelScope.launch { listenBrainzPreferences.setScrobbleJellyfin(enabled) }
    }

    private fun formatCount(count: Int, singular: String, plural: String): String {
        return if (count == 1) {
            "1 $singular"
        } else {
            "$count $plural"
        }
    }
}
