package com.startune.music.presentation.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.startune.music.data.backup.BackupManager
import com.startune.music.data.backup.format.BackupEncryptedException
import com.startune.music.data.backup.format.BackupWrongPassphraseException
import com.startune.music.data.backup.model.BackupSection
import com.startune.music.data.database.MusicDao
import com.startune.music.data.stats.PlaybackStatsRepository
import com.startune.music.data.backup.model.BackupOperationType
import com.startune.music.data.backup.model.BackupTransferProgressUpdate
import com.startune.music.data.backup.model.BackupHistoryEntry
import com.startune.music.data.backup.model.RestorePlan
import com.startune.music.data.backup.model.RestoreResult
import com.startune.music.data.backup.model.ValidationError
import com.startune.music.data.model.AudioOutputMode
import com.startune.music.data.preferences.AppLanguage
import com.startune.music.data.preferences.PreviousButtonBehavior
import com.startune.music.data.preferences.AppThemeMode
import com.startune.music.data.preferences.CarouselStyle
import com.startune.music.data.preferences.LibraryNavigationMode
import com.startune.music.data.preferences.ThemePreference
import com.startune.music.data.preferences.UserPreferencesRepository
import com.startune.music.data.preferences.AlbumArtQuality
import com.startune.music.data.preferences.AlbumArtColorAccuracy
import com.startune.music.data.preferences.AlbumArtPaletteStyle
import com.startune.music.data.preferences.CollagePattern
import com.startune.music.data.preferences.FullPlayerLoadingTweaks
import com.startune.music.data.preferences.ThemePreferencesRepository
import com.startune.music.data.repository.LyricsRepository
import com.startune.music.data.repository.MusicRepository
import com.startune.music.data.model.LyricsSourcePreference
import com.startune.music.data.preferences.DEFAULT_LYRICS_FONT_SIZE_SP
import com.startune.music.data.worker.SyncManager
import com.startune.music.data.worker.SyncProgress
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.startune.music.R
import com.startune.music.data.preferences.NavBarStyle
import com.startune.music.data.preferences.LaunchTab
import com.startune.music.data.model.Song
import com.startune.music.data.service.player.HiFiCapabilityChecker
import com.startune.music.utils.AppLocaleManager
import java.io.File
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

data class SettingsUiState(
    val isLoadingDirectories: Boolean = false,
    val appLanguageTag: String = AppLanguage.SYSTEM.tag,
    val appThemeMode: String = AppThemeMode.FOLLOW_SYSTEM,
    val globalNowPlayingThemeEnabled: Boolean = false,
    val pitchBlackEnabled: Boolean = false,
    val showScrollbar: Boolean = true,
    val autoplayOnSkip: Boolean = true,
    val miniPlayerSwipeToSkip: Boolean = false,
    val previousButtonBehavior: PreviousButtonBehavior = PreviousButtonBehavior.DEFAULT,
    val playerThemePreference: String = ThemePreference.ALBUM_ART,
    val albumArtPaletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.default,
    val albumArtColorAccuracy: Int = AlbumArtColorAccuracy.DEFAULT,
    val mockGenresEnabled: Boolean = false,
    val navBarCornerRadius: Int = 32,
    val navBarStyle: String = NavBarStyle.DEFAULT,
    val navBarCompactMode: Boolean = false,
    val carouselStyle: String = CarouselStyle.NO_PEEK,
    val libraryNavigationMode: String = LibraryNavigationMode.TAB_ROW,
    val launchTab: String = LaunchTab.HOME,
    val keepPlayingInBackground: Boolean = true,
    val restoreLastPosition: Boolean = true,
    val resumeAfterCall: Boolean = true,
    val resumeOnStart: Boolean = false,
    val resumeOnReopen: Boolean = false,
    val ignoreShortFocusLoss: Boolean = false,
    val pauseInCall: Boolean = true,
    val resumeOnFocusGain: Boolean = true,
    val duckVolume: Boolean = true,
    val respectPermanentFocusLoss: Boolean = true,
    val queueAwareNext: Boolean = false,
    val appUpdateNotificationsEnabled: Boolean = true,
    val includePrereleases: Boolean = false,
    val indexerNotificationsEnabled: Boolean = true,
    val autoBackupEnabled: Boolean = false,
    val autoBackupIntervalHours: Int = 24,
    val autoBackupLastRunMs: Long = 0L,
    val autoBackupNotificationsEnabled: Boolean = true,
    val dataStorageRoot: String = "",
    val keepScreenAwakeWhilePlaying: Boolean = false,
    val resumeOnHeadsetReconnect: Boolean = false,
    val showQueueHistory: Boolean = true,
    val isCrossfadeEnabled: Boolean = false,
    val smartCrossfadeEnabled: Boolean = false,
    val audioOutputMode: AudioOutputMode = AudioOutputMode.SYSTEM_DEFAULT,
    val pcmFloatOutputSupported: Boolean = true,
    val crossfadeDuration: Int = 2000,
    val playedThresholdPercent: Int = 50,
    val persistentShuffleEnabled: Boolean = false,
    val folderBackGestureNavigation: Boolean = true,
    val lyricsSourcePreference: LyricsSourcePreference = LyricsSourcePreference.EMBEDDED_FIRST,
    val autoScanLrcFiles: Boolean = false,
    val autoScanOnLaunch: Boolean = true,
    val externalLyricsEnabled: Boolean = false,
    val externalArtistImagesEnabled: Boolean = false,
    val blockedDirectories: Set<String> = emptySet(),
    val appRebrandDialogShown: Boolean = false,
    val fullPlayerLoadingTweaks: FullPlayerLoadingTweaks = FullPlayerLoadingTweaks(),
    val showPlayerFileInfo: Boolean = true,
    val albumArtQuality: AlbumArtQuality = AlbumArtQuality.MEDIUM,
    val albumArtCacheLimitMb: Int = 200,
    val tapBackgroundClosesPlayer: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val immersiveLyricsEnabled: Boolean = false,
    val immersiveLyricsTimeout: Long = 4000L,
    val lyricsFontSizeSp: Float = DEFAULT_LYRICS_FONT_SIZE_SP,
    val useAnimatedLyrics: Boolean = false,
    val animatedLyricsBlurEnabled: Boolean = true,
    val animatedLyricsBlurStrength: Float = 2.5f,
    val backupInfoDismissed: Boolean = false,
    val isDataTransferInProgress: Boolean = false,
    val restorePlan: RestorePlan? = null,
    val backupHistory: ImmutableList<BackupHistoryEntry> = persistentListOf(),
    val backupValidationErrors: List<ValidationError> = emptyList(),
    val isInspectingBackup: Boolean = false,
    // Encrypted backup password prompt state
    val pendingEncryptedBackupUri: String? = null,
    val wrongBackupPassword: Boolean = false,
    val collagePattern: CollagePattern = CollagePattern.default,
    val collageAutoRotate: Boolean = false,
    val minSongDuration: Int = 10000,
    val minTracksPerAlbum: Int = 1,
    val replayGainEnabled: Boolean = false,
    val replayGainUseAlbumGain: Boolean = false
)

data class FailedSongInfo(
    val id: String,
    val title: String,
    val artist: String
)

data class LyricsRefreshProgress(
    val totalSongs: Int = 0,
    val currentCount: Int = 0,
    val savedCount: Int = 0,
    val notFoundCount: Int = 0,
    val skippedCount: Int = 0,
    val isComplete: Boolean = false,
    val failedSongs: List<FailedSongInfo> = emptyList()
) {
    val hasProgress: Boolean get() = totalSongs > 0
    val progress: Float get() = if (totalSongs > 0) currentCount.toFloat() / totalSongs else 0f
    val hasFailedSongs: Boolean get() = failedSongs.isNotEmpty()
}

private sealed interface SettingsUiUpdate {
    data class Group1(
        val appRebrandDialogShown: Boolean,
        val appThemeMode: String,
        val playerThemePreference: String,
        val albumArtPaletteStyle: AlbumArtPaletteStyle,
        val albumArtColorAccuracy: Int,
        val mockGenresEnabled: Boolean,
        val navBarCornerRadius: Int,
        val navBarStyle: String,
        val navBarCompactMode: Boolean,
        val libraryNavigationMode: String,
        val carouselStyle: String,
        val launchTab: String,
        val showPlayerFileInfo: Boolean,
        val globalNowPlayingThemeEnabled: Boolean,
        val pitchBlackEnabled: Boolean
    ) : SettingsUiUpdate
    
    data class Group2(
        val keepPlayingInBackground: Boolean,
        val resumeOnHeadsetReconnect: Boolean,
        val showQueueHistory: Boolean,
        val isCrossfadeEnabled: Boolean,
        val audioOutputMode: AudioOutputMode,
        val crossfadeDuration: Int,
        val persistentShuffleEnabled: Boolean,
        val folderBackGestureNavigation: Boolean,
        val lyricsSourcePreference: LyricsSourcePreference,
        val autoScanLrcFiles: Boolean,
        val blockedDirectories: Set<String>,
        val hapticsEnabled: Boolean,
        val immersiveLyricsEnabled: Boolean,
        val immersiveLyricsTimeout: Long,
        val animatedLyricsBlurEnabled: Boolean,
        val animatedLyricsBlurStrength: Float,
        val playedThresholdPercent: Int,
        val restoreLastPosition: Boolean,
        val resumeAfterCall: Boolean,
        val resumeOnStart: Boolean,
        val resumeOnReopen: Boolean,
        val ignoreShortFocusLoss: Boolean,
        val pauseInCall: Boolean,
        val resumeOnFocusGain: Boolean,
        val duckVolume: Boolean,
        val respectPermanentFocusLoss: Boolean,
        val queueAwareNext: Boolean,
        val appUpdateNotificationsEnabled: Boolean,
        val includePrereleases: Boolean,
        val indexerNotificationsEnabled: Boolean,
        val autoBackupEnabled: Boolean,
        val autoBackupIntervalHours: Int,
        val autoBackupLastRunMs: Long,
        val dataStorageRoot: String,
        val autoScanOnLaunch: Boolean,
        val lyricsFontSizeSp: Float,
        val autoBackupNotificationsEnabled: Boolean
    ) : SettingsUiUpdate
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val themePreferencesRepository: ThemePreferencesRepository,
    private val colorSchemeProcessor: ColorSchemeProcessor,
    private val syncManager: SyncManager,
    private val lyricsRepository: LyricsRepository,
    private val musicRepository: MusicRepository,
    private val backupManager: BackupManager,
    private val autoBackupManager: com.startune.music.data.backup.AutoBackupManager,
    private val searchStateHolder: SearchStateHolder,
    private val playbackStatsRepository: PlaybackStatsRepository,
    private val musicDao: MusicDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    private val pcmFloatOutputSupported = HiFiCapabilityChecker.isSupported()

    private val fileExplorerStateHolder = FileExplorerStateHolder(userPreferencesRepository, viewModelScope, context)

    val currentPath = fileExplorerStateHolder.currentPath
    val currentDirectoryChildren = fileExplorerStateHolder.currentDirectoryChildren
    val allowedDirectories = fileExplorerStateHolder.allowedDirectories
    val blockedDirectories = fileExplorerStateHolder.blockedDirectories
    val isWhitelist: StateFlow<Boolean> = combine(
        userPreferencesRepository.folderFilterModeFlow,
        fileExplorerStateHolder.allowedDirectories
    ) { mode, allowed -> mode ?: allowed.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    val availableStorages = fileExplorerStateHolder.availableStorages
    val selectedStorageIndex = fileExplorerStateHolder.selectedStorageIndex
    val isLoadingDirectories = fileExplorerStateHolder.isLoading
    val isExplorerPriming = fileExplorerStateHolder.isPrimingExplorer
    val isExplorerReady = fileExplorerStateHolder.isExplorerReady
    val isCurrentDirectoryResolved = fileExplorerStateHolder.isCurrentDirectoryResolved

    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val syncProgress: StateFlow<SyncProgress> = syncManager.syncProgress
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SyncProgress()
        )

    private val _dataTransferEvents = Channel<String>(Channel.BUFFERED)
    val dataTransferEvents: Flow<String> = _dataTransferEvents.receiveAsFlow()

    init {
        viewModelScope.launch {
            backupManager.getBackupHistory().collect { history ->
                _uiState.update { it.copy(backupHistory = history.toImmutableList()) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.collagePatternFlow.collect { pattern ->
                _uiState.update { it.copy(collagePattern = pattern) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.collageAutoRotateFlow.collect { autoRotate ->
                _uiState.update { it.copy(collageAutoRotate = autoRotate) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.autoplayOnSkipFlow.collect { enabled ->
                _uiState.update { it.copy(autoplayOnSkip = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.miniPlayerSwipeToSkipFlow.collect { enabled ->
                _uiState.update { it.copy(miniPlayerSwipeToSkip = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.previousButtonBehaviorFlow.collect { behavior ->
                _uiState.update { it.copy(previousButtonBehavior = behavior) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.showScrollbarFlow.collect { enabled ->
                _uiState.update { it.copy(showScrollbar = enabled) }
            }
        }
    }

    private val _dataTransferProgress = MutableStateFlow<BackupTransferProgressUpdate?>(null)
    val dataTransferProgress: StateFlow<BackupTransferProgressUpdate?> = _dataTransferProgress.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                pcmFloatOutputSupported = pcmFloatOutputSupported,
                appLanguageTag = AppLocaleManager.currentLanguageTag(context)
            )
        }

        viewModelScope.launch {
            combine<Any?, SettingsUiUpdate.Group1>(
                userPreferencesRepository.appRebrandDialogShownFlow,
                themePreferencesRepository.appThemeModeFlow,
                themePreferencesRepository.playerThemePreferenceFlow,
                themePreferencesRepository.albumArtPaletteStyleFlow,
                themePreferencesRepository.albumArtColorAccuracyFlow,
                userPreferencesRepository.mockGenresEnabledFlow,
                userPreferencesRepository.navBarCornerRadiusFlow,
                userPreferencesRepository.navBarStyleFlow,
                userPreferencesRepository.navBarCompactModeFlow,
                userPreferencesRepository.libraryNavigationModeFlow,
                userPreferencesRepository.carouselStyleFlow,
                userPreferencesRepository.launchTabFlow,
                userPreferencesRepository.showPlayerFileInfoFlow,
                themePreferencesRepository.globalNowPlayingThemeEnabledFlow,
                themePreferencesRepository.pitchBlackEnabledFlow
            ) { values ->
                SettingsUiUpdate.Group1(
                    appRebrandDialogShown = values[0] as Boolean,
                    appThemeMode = values[1] as String,
                    playerThemePreference = values[2] as String,
                    albumArtPaletteStyle = values[3] as AlbumArtPaletteStyle,
                    albumArtColorAccuracy = values[4] as Int,
                    mockGenresEnabled = values[5] as Boolean,
                    navBarCornerRadius = values[6] as Int,
                    navBarStyle = values[7] as String,
                    navBarCompactMode = values[8] as Boolean,
                    libraryNavigationMode = values[9] as String,
                    carouselStyle = values[10] as String,
                    launchTab = values[11] as String,
                    showPlayerFileInfo = values[12] as Boolean,
                    globalNowPlayingThemeEnabled = values[13] as Boolean,
                    pitchBlackEnabled = values[14] as Boolean
                )
            }.collect { update ->
                _uiState.update { state ->
                    state.copy(
                        appRebrandDialogShown = update.appRebrandDialogShown,
                        appThemeMode = update.appThemeMode,
                        playerThemePreference = update.playerThemePreference,
                        albumArtPaletteStyle = update.albumArtPaletteStyle,
                        albumArtColorAccuracy = update.albumArtColorAccuracy,
                        mockGenresEnabled = update.mockGenresEnabled,
                        navBarCornerRadius = update.navBarCornerRadius,
                        navBarStyle = update.navBarStyle,
                        navBarCompactMode = update.navBarCompactMode,
                        libraryNavigationMode = update.libraryNavigationMode,
                        carouselStyle = update.carouselStyle,
                        launchTab = update.launchTab,
                        showPlayerFileInfo = update.showPlayerFileInfo,
                        globalNowPlayingThemeEnabled = update.globalNowPlayingThemeEnabled,
                        pitchBlackEnabled = update.pitchBlackEnabled
                    )
                }
            }
        }
        
        viewModelScope.launch {
            combine<Any?, SettingsUiUpdate.Group2>(
                userPreferencesRepository.keepPlayingInBackgroundFlow,
                userPreferencesRepository.resumeOnHeadsetReconnectFlow,
                userPreferencesRepository.showQueueHistoryFlow,
                userPreferencesRepository.isCrossfadeEnabledFlow,
                userPreferencesRepository.audioOutputModeFlow,
                userPreferencesRepository.crossfadeDurationFlow,
                userPreferencesRepository.persistentShuffleEnabledFlow,
                userPreferencesRepository.folderBackGestureNavigationFlow,
                userPreferencesRepository.lyricsSourcePreferenceFlow,
                userPreferencesRepository.autoScanLrcFilesFlow,
                userPreferencesRepository.blockedDirectoriesFlow,
                userPreferencesRepository.hapticsEnabledFlow,
                userPreferencesRepository.immersiveLyricsEnabledFlow,
                userPreferencesRepository.immersiveLyricsTimeoutFlow,
                userPreferencesRepository.animatedLyricsBlurEnabledFlow,
                userPreferencesRepository.animatedLyricsBlurStrengthFlow,
                userPreferencesRepository.playedThresholdPercentFlow,
                userPreferencesRepository.restoreLastPositionFlow,
                userPreferencesRepository.resumeAfterCallFlow,
                userPreferencesRepository.resumeOnStartFlow,
                userPreferencesRepository.resumeOnReopenFlow,
                userPreferencesRepository.ignoreShortFocusLossFlow,
                userPreferencesRepository.pauseInCallFlow,
                userPreferencesRepository.resumeOnFocusGainFlow,
                userPreferencesRepository.duckVolumeFlow,
                userPreferencesRepository.respectPermanentFocusLossFlow,
                userPreferencesRepository.queueAwareNextFlow,
                userPreferencesRepository.appUpdateNotificationsEnabledFlow,
                userPreferencesRepository.includePrereleasesFlow,
                userPreferencesRepository.indexerNotificationsEnabledFlow,
                userPreferencesRepository.autoBackupEnabledFlow,
                userPreferencesRepository.autoBackupIntervalHoursFlow,
                userPreferencesRepository.autoBackupLastRunMsFlow,
                userPreferencesRepository.dataStorageRootFlow,
                userPreferencesRepository.autoScanOnLaunchFlow,
                userPreferencesRepository.lyricsFontSizeFlow,
                userPreferencesRepository.autoBackupNotificationsEnabledFlow
            ) { values ->
                val requestedAudioOutputMode = values[4] as AudioOutputMode
                SettingsUiUpdate.Group2(
                    keepPlayingInBackground = values[0] as Boolean,
                    resumeOnHeadsetReconnect = values[1] as Boolean,
                    showQueueHistory = values[2] as Boolean,
                    isCrossfadeEnabled = values[3] as Boolean,
                    audioOutputMode = if (
                        requestedAudioOutputMode == AudioOutputMode.PCM_FLOAT &&
                        !pcmFloatOutputSupported
                    ) {
                        AudioOutputMode.SYSTEM_DEFAULT
                    } else {
                        requestedAudioOutputMode
                    },
                    crossfadeDuration = values[5] as Int,
                    persistentShuffleEnabled = values[6] as Boolean,
                    folderBackGestureNavigation = values[7] as Boolean,
                    lyricsSourcePreference = values[8] as LyricsSourcePreference,
                    autoScanLrcFiles = values[9] as Boolean,
                    blockedDirectories = @Suppress("UNCHECKED_CAST") (values[10] as Set<String>),
                    hapticsEnabled = values[11] as Boolean,
                    immersiveLyricsEnabled = values[12] as Boolean,
                    immersiveLyricsTimeout = values[13] as Long,
                    animatedLyricsBlurEnabled = values[14] as Boolean,
                    animatedLyricsBlurStrength = values[15] as Float,
                    playedThresholdPercent = values[16] as Int,
                    restoreLastPosition = values[17] as Boolean,
                    resumeAfterCall = values[18] as Boolean,
                    resumeOnStart = values[19] as Boolean,
                    resumeOnReopen = values[20] as Boolean,
                    ignoreShortFocusLoss = values[21] as Boolean,
                    pauseInCall = values[22] as Boolean,
                    resumeOnFocusGain = values[23] as Boolean,
                    duckVolume = values[24] as Boolean,
                    respectPermanentFocusLoss = values[25] as Boolean,
                    queueAwareNext = values[26] as Boolean,
                    appUpdateNotificationsEnabled = values[27] as Boolean,
                    includePrereleases = values[28] as Boolean,
                    indexerNotificationsEnabled = values[29] as Boolean,
                    autoBackupEnabled = values[30] as Boolean,
                    autoBackupIntervalHours = values[31] as Int,
                    autoBackupLastRunMs = values[32] as Long,
                    dataStorageRoot = values[33] as String,
                    autoScanOnLaunch = values[34] as Boolean,
                    lyricsFontSizeSp = values[35] as Float,
                    autoBackupNotificationsEnabled = values[36] as Boolean
                )
            }.collect { update ->
                _uiState.update { state ->
                    state.copy(
                        keepPlayingInBackground = update.keepPlayingInBackground,
                        resumeOnHeadsetReconnect = update.resumeOnHeadsetReconnect,
                        showQueueHistory = update.showQueueHistory,
                        isCrossfadeEnabled = update.isCrossfadeEnabled,
                        audioOutputMode = update.audioOutputMode,
                        crossfadeDuration = update.crossfadeDuration,
                        persistentShuffleEnabled = update.persistentShuffleEnabled,
                        folderBackGestureNavigation = update.folderBackGestureNavigation,
                        lyricsSourcePreference = update.lyricsSourcePreference,
                        autoScanLrcFiles = update.autoScanLrcFiles,
                        blockedDirectories = update.blockedDirectories,
                        hapticsEnabled = update.hapticsEnabled,
                        immersiveLyricsEnabled = update.immersiveLyricsEnabled,
                        immersiveLyricsTimeout = update.immersiveLyricsTimeout,
                        lyricsFontSizeSp = update.lyricsFontSizeSp,
                        animatedLyricsBlurEnabled = update.animatedLyricsBlurEnabled,
                        animatedLyricsBlurStrength = update.animatedLyricsBlurStrength,
                        playedThresholdPercent = update.playedThresholdPercent,
                        restoreLastPosition = update.restoreLastPosition,
                        resumeAfterCall = update.resumeAfterCall,
                        resumeOnStart = update.resumeOnStart,
                        resumeOnReopen = update.resumeOnReopen,
                        ignoreShortFocusLoss = update.ignoreShortFocusLoss,
                        pauseInCall = update.pauseInCall,
                        resumeOnFocusGain = update.resumeOnFocusGain,
                        duckVolume = update.duckVolume,
                        respectPermanentFocusLoss = update.respectPermanentFocusLoss,
                        queueAwareNext = update.queueAwareNext,
                        appUpdateNotificationsEnabled = update.appUpdateNotificationsEnabled,
                        includePrereleases = update.includePrereleases,
                        indexerNotificationsEnabled = update.indexerNotificationsEnabled,
                        autoBackupEnabled = update.autoBackupEnabled,
                        autoBackupIntervalHours = update.autoBackupIntervalHours,
                        autoBackupLastRunMs = update.autoBackupLastRunMs,
                        autoBackupNotificationsEnabled = update.autoBackupNotificationsEnabled,
                        dataStorageRoot = update.dataStorageRoot,
                        autoScanOnLaunch = update.autoScanOnLaunch,
                    )
                }
            }
        }
        
        viewModelScope.launch {
            userPreferencesRepository.smartCrossfadeEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(smartCrossfadeEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.keepScreenAwakeWhilePlayingFlow.collect { enabled ->
                _uiState.update { it.copy(keepScreenAwakeWhilePlaying = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.fullPlayerLoadingTweaksFlow.collect { tweaks ->
                _uiState.update { it.copy(fullPlayerLoadingTweaks = tweaks) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.useAnimatedLyricsFlow.collect { enabled ->
                _uiState.update { it.copy(useAnimatedLyrics = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.externalLyricsEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(externalLyricsEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.externalArtistImagesEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(externalArtistImagesEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.backupInfoDismissedFlow.collect { dismissed ->
                _uiState.update { it.copy(backupInfoDismissed = dismissed) }
            }
        }

        viewModelScope.launch {
            fileExplorerStateHolder.isLoading.collect { loading ->
                _uiState.update { it.copy(isLoadingDirectories = loading) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.albumArtQualityFlow.collect { quality ->
                _uiState.update { it.copy(albumArtQuality = quality) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.albumArtCacheLimitMbFlow.collect { limitMb ->
                _uiState.update { it.copy(albumArtCacheLimitMb = limitMb) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.tapBackgroundClosesPlayerFlow.collect { enabled ->
                _uiState.update { it.copy(tapBackgroundClosesPlayer = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.minSongDurationFlow.collect { duration ->
                _uiState.update { it.copy(minSongDuration = duration) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.minTracksPerAlbumFlow.collect { minTracks ->
                _uiState.update { it.copy(minTracksPerAlbum = minTracks) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.replayGainEnabledFlow.collect { enabled ->
                _uiState.update { it.copy(replayGainEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            userPreferencesRepository.replayGainUseAlbumGainFlow.collect { useAlbum ->
                _uiState.update { it.copy(replayGainUseAlbumGain = useAlbum) }
            }
        }
    }

    fun setAppRebrandDialogShown(wasShown: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAppRebrandDialogShown(wasShown)
        }
    }

    fun addFolder(path: String) {
        viewModelScope.launch {
            val normalized = File(path).absolutePath
            val allowed = userPreferencesRepository.allowedDirectoriesFlow.first()
            val blocked = userPreferencesRepository.blockedDirectoriesFlow.first()
            val whitelist = userPreferencesRepository.folderFilterModeFlow.first() ?: allowed.isNotEmpty()
            if (whitelist) {
                if (normalized in allowed) return@launch
                fileExplorerStateHolder.noteDirectorySelections(allowed + normalized, blocked - normalized)
                userPreferencesRepository.updateDirectorySelections(allowed + normalized, blocked - normalized)
            } else {
                if (normalized in blocked) return@launch
                fileExplorerStateHolder.noteDirectorySelections(allowed - normalized, blocked + normalized)
                userPreferencesRepository.updateDirectorySelections(allowed - normalized, blocked + normalized)
            }
        }
    }

    fun removeFolder(path: String) {
        viewModelScope.launch {
            val allowed = userPreferencesRepository.allowedDirectoriesFlow.first()
            val blocked = userPreferencesRepository.blockedDirectoriesFlow.first()
            if (path !in allowed && path !in blocked) return@launch
            fileExplorerStateHolder.noteDirectorySelections(allowed - path, blocked - path)
            userPreferencesRepository.updateDirectorySelections(allowed - path, blocked - path)
        }
    }

    fun setFolderFilterMode(whitelist: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFolderFilterMode(whitelist)
            val allowed = userPreferencesRepository.allowedDirectoriesFlow.first()
            val blocked = userPreferencesRepository.blockedDirectoriesFlow.first()
            val all = allowed + blocked
            if (whitelist) {
                fileExplorerStateHolder.noteDirectorySelections(all, emptySet())
                userPreferencesRepository.updateDirectorySelections(all, emptySet())
            } else {
                fileExplorerStateHolder.noteDirectorySelections(emptySet(), all)
                userPreferencesRepository.updateDirectorySelections(emptySet(), all)
            }
        }
    }

    fun loadDirectory(file: File) {
        fileExplorerStateHolder.loadDirectory(file)
    }

    fun primeExplorer() {
        fileExplorerStateHolder.primeExplorerRoot()
    }

    fun openExplorer() {
        fileExplorerStateHolder.openExplorerRoot()
    }

    fun navigateUp() {
        fileExplorerStateHolder.navigateUp()
    }

    fun refreshExplorer() {
        fileExplorerStateHolder.refreshCurrentDirectory()
    }

    fun selectStorage(index: Int) {
        fileExplorerStateHolder.selectStorage(index)
    }

    fun refreshAvailableStorages() {
        fileExplorerStateHolder.refreshAvailableStorages()
    }

    fun isAtRoot(): Boolean = fileExplorerStateHolder.isAtRoot()

    fun explorerRoot(): File = fileExplorerStateHolder.rootDirectory()

    fun setPlayerThemePreference(preference: String) {
        viewModelScope.launch {
            themePreferencesRepository.setPlayerThemePreference(preference)
        }
    }

    fun setGlobalNowPlayingThemeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themePreferencesRepository.setGlobalNowPlayingThemeEnabled(enabled)
        }
    }

    fun setPitchBlackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            themePreferencesRepository.setPitchBlackEnabled(enabled)
        }
    }

    fun setAlbumArtPaletteStyle(style: AlbumArtPaletteStyle) {
        viewModelScope.launch {
            themePreferencesRepository.setAlbumArtPaletteStyle(style)
        }
    }

    fun setAlbumArtPaletteSettings(
        style: AlbumArtPaletteStyle,
        accuracyLevel: Int
    ) {
        viewModelScope.launch {
            themePreferencesRepository.setAlbumArtPaletteSettings(style, accuracyLevel)
        }
    }

    suspend fun getAlbumArtPalettePreview(
        uriString: String,
        style: AlbumArtPaletteStyle,
        accuracyLevel: Int
    ): ColorSchemePair? {
        return colorSchemeProcessor.getPreviewColorScheme(
            albumArtUri = uriString,
            paletteStyle = style,
            colorAccuracyLevel = accuracyLevel
        )
    }

    fun setCollagePattern(pattern: CollagePattern) {
        viewModelScope.launch {
            userPreferencesRepository.setCollagePattern(pattern)
        }
    }

    fun setCollageAutoRotate(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCollageAutoRotate(enabled)
        }
    }

    fun setAppLanguage(languageTag: String) {
        val normalized = AppLanguage.normalize(languageTag)
        AppLocaleManager.applyLanguage(context, normalized)
        _uiState.update { it.copy(appLanguageTag = normalized) }
    }

    fun setAppThemeMode(mode: String) {
        viewModelScope.launch {
            themePreferencesRepository.setAppThemeMode(mode)
        }
    }

    fun setNavBarStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.setNavBarStyle(style)
        }
    }

    fun setNavBarCompactMode(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setNavBarCompactMode(enabled)
        }
    }

    fun setLibraryNavigationMode(mode: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLibraryNavigationMode(mode)
        }
    }

    fun setCarouselStyle(style: String) {
        viewModelScope.launch {
            userPreferencesRepository.setCarouselStyle(style)
        }
    }

    fun setShowPlayerFileInfo(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowPlayerFileInfo(show)
        }
    }

    fun setLaunchTab(tab: String) {
        viewModelScope.launch {
            userPreferencesRepository.setLaunchTab(tab)
        }
    }

    fun setKeepPlayingInBackground(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setKeepPlayingInBackground(enabled)
        }
    }

    fun setRestoreLastPosition(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRestoreLastPosition(enabled)
        }
    }

    fun setResumeAfterCall(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumeAfterCall(enabled)
        }
    }

    fun setResumeOnStart(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumeOnStart(enabled)
        }
    }

    fun setResumeOnReopen(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumeOnReopen(enabled)
        }
    }

    fun setIgnoreShortFocusLoss(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setIgnoreShortFocusLoss(enabled)
        }
    }

    fun setPauseInCall(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPauseInCall(enabled)
        }
    }

    fun setResumeOnFocusGain(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumeOnFocusGain(enabled)
        }
    }

    fun setDuckVolume(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDuckVolume(enabled)
        }
    }

    fun setRespectPermanentFocusLoss(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setRespectPermanentFocusLoss(enabled)
        }
    }

    fun setQueueAwareNext(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setQueueAwareNext(enabled)
        }
    }

    fun setAppUpdateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAppUpdateNotificationsEnabled(enabled)
        }
    }

    fun setIncludePrereleases(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setIncludePrereleases(enabled)
        }
    }

    fun setIndexerNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setIndexerNotificationsEnabled(enabled)
        }
    }

    fun setAutoBackupInterval(intervalHours: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupEnabled(intervalHours > 0)
            if (intervalHours > 0) {
                userPreferencesRepository.setAutoBackupIntervalHours(intervalHours)
            }
            autoBackupManager.ensureScheduled()
        }
    }

    fun setAutoBackupNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoBackupNotificationsEnabled(enabled)
        }
    }

    fun setDataStorageRoot(path: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDataStorageRoot(path)
        }
    }

    fun setKeepScreenAwakeWhilePlaying(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setKeepScreenAwakeWhilePlaying(enabled)
        }
    }

    fun setResumeOnHeadsetReconnect(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setResumeOnHeadsetReconnect(enabled)
        }
    }

    fun setAudioOutputMode(mode: AudioOutputMode) {
        viewModelScope.launch {
            val supportedMode = if (
                mode == AudioOutputMode.PCM_FLOAT && !pcmFloatOutputSupported
            ) {
                AudioOutputMode.SYSTEM_DEFAULT
            } else {
                mode
            }
            userPreferencesRepository.setAudioOutputMode(supportedMode)
        }
    }

    fun setShowQueueHistory(show: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowQueueHistory(show)
        }
    }

    fun setCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setCrossfadeEnabled(enabled)
        }
    }

    fun setCrossfadeDuration(duration: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setCrossfadeDuration(duration)
        }
    }

    fun setPlayedThresholdPercent(percent: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setPlayedThresholdPercent(percent)
        }
    }

    fun setSmartCrossfadeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSmartCrossfadeEnabled(enabled)
        }
    }

    val playbackSpeed: StateFlow<Float> = userPreferencesRepository.playbackSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    fun setShowScrollbar(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setShowScrollbar(enabled)
        }
    }

    fun setAutoplayOnSkip(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoplayOnSkip(enabled)
        }
    }

    fun setMiniPlayerSwipeToSkip(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setMiniPlayerSwipeToSkip(enabled)
        }
    }

    fun setPreviousButtonBehavior(behavior: PreviousButtonBehavior) {
        viewModelScope.launch {
            userPreferencesRepository.setPreviousButtonBehavior(behavior)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setPlaybackSpeed(speed)
        }
    }

    fun setPersistentShuffleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setPersistentShuffleEnabled(enabled)
        }
    }

    fun setFolderBackGestureNavigation(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFolderBackGestureNavigation(enabled)
        }
    }

    fun setLyricsSourcePreference(preference: LyricsSourcePreference) {
        viewModelScope.launch {
            userPreferencesRepository.setLyricsSourcePreference(preference)
        }
    }

    fun setAutoScanLrcFiles(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoScanLrcFiles(enabled)
        }
    }

    fun setAutoScanOnLaunch(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAutoScanOnLaunch(enabled)
        }
    }

    fun setExternalLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setExternalLyricsEnabled(enabled)
        }
    }

    fun setExternalArtistImagesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setExternalArtistImagesEnabled(enabled)
        }
    }

    fun setDelayAllFullPlayerContent(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayAllFullPlayerContent(enabled)
        }
    }

    fun setDelayAlbumCarousel(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayAlbumCarousel(enabled)
        }
    }

    fun setDelaySongMetadata(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelaySongMetadata(enabled)
        }
    }

    fun setDelayProgressBar(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayProgressBar(enabled)
        }
    }

    fun setDelayControls(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setDelayControls(enabled)
        }
    }

    fun setFullPlayerPlaceholders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerPlaceholders(enabled)
            if (!enabled) {
                userPreferencesRepository.setTransparentPlaceholders(false)
            }
        }
    }

    fun setTransparentPlaceholders(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTransparentPlaceholders(enabled)
        }
    }

    fun setFullPlayerPlaceholdersOnClose(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerPlaceholdersOnClose(enabled)
        }
    }

    fun setFullPlayerSwitchOnDragRelease(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerSwitchOnDragRelease(enabled)
        }
    }

    fun setFullPlayerAppearThreshold(thresholdPercent: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerAppearThreshold(thresholdPercent)
        }
    }

    fun setFullPlayerCloseThreshold(thresholdPercent: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setFullPlayerCloseThreshold(thresholdPercent)
        }
    }

    fun setUseAnimatedLyrics(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseAnimatedLyrics(enabled)
        }
    }

    fun setAnimatedLyricsBlurEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setAnimatedLyricsBlurEnabled(enabled)
        }
    }

    fun setAnimatedLyricsBlurStrength(strength: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setAnimatedLyricsBlurStrength(strength)
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch {
            if (isSyncing.value) return@launch
            syncManager.forceRefresh()
        }
    }




    /**
     * Performs a full library rescan - rescans all files from scratch.
     * Use when songs are missing or metadata is incorrect.
     */
    fun fullSyncLibrary() {
        viewModelScope.launch {
            if (isSyncing.value) return@launch
            syncManager.fullSync()
        }
    }

    fun setMinSongDuration(durationMs: Int) {
        viewModelScope.launch {
            if (durationMs == _uiState.value.minSongDuration) return@launch
            userPreferencesRepository.setMinSongDuration(durationMs)
            syncManager.fullSync(deepScan = false)
        }
    }

    fun setMinTracksPerAlbum(minTracks: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setMinTracksPerAlbum(minTracks)
        }
    }

    fun setReplayGainEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setReplayGainEnabled(enabled)
        }
    }

    fun setReplayGainUseAlbumGain(useAlbumGain: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setReplayGainUseAlbumGain(useAlbumGain)
        }
    }

    fun setImmersiveLyricsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setImmersiveLyricsEnabled(enabled)
        }
    }

    fun setLyricsFontSize(fontSizeSp: Float) {
        viewModelScope.launch {
            userPreferencesRepository.setLyricsFontSize(fontSizeSp)
        }
    }

    fun setImmersiveLyricsTimeout(timeout: Long) {
        viewModelScope.launch {
            userPreferencesRepository.setImmersiveLyricsTimeout(timeout)
        }
    }

    /**
     * Rebuilds local MediaStore-backed songs from scratch while preserving cloud sources.
     * Local imported lyrics, favorites, and user metadata edits are removed for rebuilt songs.
     * Use when local library data is corrupted or as a last resort.
     */
    fun rebuildDatabase() {
        viewModelScope.launch {
            if (isSyncing.value) return@launch
            syncManager.rebuildDatabase()
        }
    }

    fun setNavBarCornerRadius(radius: Int) {
        viewModelScope.launch { userPreferencesRepository.setNavBarCornerRadius(radius) }
    }
    /**
     * Triggers a test crash to verify the crash handler is working correctly.
     * This should only be used for testing in Developer Options.
     */
    fun triggerTestCrash() {
        throw RuntimeException(context.getString(R.string.dev_test_crash_message))
    }

    fun resetSetupFlow() {
        viewModelScope.launch {
            userPreferencesRepository.setInitialSetupDone(false)
        }
    }

    fun clearSearchHistory() {
        searchStateHolder.clearSearchHistory()
    }

    fun clearListeningHistory() {
        viewModelScope.launch {
            runCatching {
                playbackStatsRepository.clearHistory()
                musicDao.resetAllPlayCounts()
            }
        }
    }

    val albumArtQuality: StateFlow<AlbumArtQuality> = userPreferencesRepository.albumArtQualityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumArtQuality.MEDIUM)

    val useSmoothCorners: StateFlow<Boolean> = userPreferencesRepository.useSmoothCornersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val tapBackgroundClosesPlayer: StateFlow<Boolean> = userPreferencesRepository.tapBackgroundClosesPlayerFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setAlbumArtQuality(quality: AlbumArtQuality) {
        viewModelScope.launch {
            userPreferencesRepository.setAlbumArtQuality(quality)
        }
    }

    fun setAlbumArtCacheLimitMb(limitMb: Int) {
        viewModelScope.launch {
            userPreferencesRepository.setAlbumArtCacheLimitMb(limitMb)
            com.startune.music.utils.AlbumArtCacheManager.configuredCacheLimitMb = limitMb.toLong()
        }
    }

    fun setUseSmoothCorners(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setUseSmoothCorners(enabled)
        }
    }

    fun setTapBackgroundClosesPlayer(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setTapBackgroundClosesPlayer(enabled)
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setHapticsEnabled(enabled)
        }
    }

    fun setBackupInfoDismissed(dismissed: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setBackupInfoDismissed(dismissed)
        }
    }

    fun exportAppData(uri: Uri, sections: Set<BackupSection>, passphrase: String? = null) {
        if (sections.isEmpty() || _uiState.value.isDataTransferInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDataTransferInProgress = true) }
            _dataTransferProgress.value = BackupTransferProgressUpdate(
                operation = BackupOperationType.EXPORT,
                step = 0,
                totalSteps = 1,
                title = context.getString(R.string.backup_progress_preparing_backup),
                detail = context.getString(R.string.backup_progress_starting_backup_task),
            )
            val result = backupManager.export(uri, sections, passphrase) { progress ->
                _dataTransferProgress.value = progress
            }
            result.fold(
                onSuccess = { _dataTransferEvents.send(context.getString(R.string.data_exported_successfully)) },
                onFailure = {
                    _dataTransferEvents.send(
                        context.getString(
                            R.string.export_failed_format,
                            it.localizedMessage ?: context.getString(R.string.error_unknown),
                        ),
                    )
                },
            )
            delay(300)
            _uiState.update { it.copy(isDataTransferInProgress = false) }
            _dataTransferProgress.value = null
        }
    }

    fun inspectBackupFile(uri: Uri, passphrase: String? = null) {
        if (_uiState.value.isInspectingBackup) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isInspectingBackup = true,
                    backupValidationErrors = emptyList(),
                    restorePlan = null,
                    wrongBackupPassword = false
                )
            }
            val result = backupManager.inspectBackup(uri, passphrase)
            result.fold(
                onSuccess = { plan ->
                    _uiState.update {
                        it.copy(
                            restorePlan = plan,
                            isInspectingBackup = false,
                            pendingEncryptedBackupUri = null,
                            wrongBackupPassword = false
                        )
                    }
                },
                onFailure = { error ->
                    when (error) {
                        is BackupEncryptedException -> _uiState.update {
                            it.copy(isInspectingBackup = false, pendingEncryptedBackupUri = uri.toString())
                        }
                        is BackupWrongPassphraseException -> _uiState.update {
                            it.copy(
                                isInspectingBackup = false,
                                pendingEncryptedBackupUri = uri.toString(),
                                wrongBackupPassword = true
                            )
                        }
                        else -> {
                            _dataTransferEvents.send(
                                context.getString(
                                    R.string.backup_invalid_format,
                                    error.localizedMessage ?: context.getString(R.string.error_unknown),
                                ),
                            )
                            _uiState.update { it.copy(isInspectingBackup = false) }
                        }
                    }
                }
            )
        }
    }

    /** Called when the user dismisses the encrypted-backup password prompt. */
    fun dismissEncryptedBackupPrompt() {
        _uiState.update {
            it.copy(
                pendingEncryptedBackupUri = null,
                wrongBackupPassword = false
            )
        }
    }

    fun updateRestorePlanSelection(selectedModules: Set<BackupSection>) {
        _uiState.update { state ->
            state.restorePlan?.let { plan ->
                state.copy(restorePlan = plan.copy(selectedModules = selectedModules))
            } ?: state
        }
    }

    fun restoreFromPlan(uri: Uri) {
        val plan = _uiState.value.restorePlan ?: return
        if (plan.selectedModules.isEmpty() || _uiState.value.isDataTransferInProgress) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDataTransferInProgress = true) }
            _dataTransferProgress.value = BackupTransferProgressUpdate(
                operation = BackupOperationType.IMPORT,
                step = 0,
                totalSteps = 1,
                title = context.getString(R.string.backup_progress_preparing_restore),
                detail = context.getString(R.string.backup_progress_starting_task),
            )
            val result = backupManager.restore(uri, plan) { progress ->
                _dataTransferProgress.value = progress
            }
            when (result) {
                is RestoreResult.Success -> {
                    _dataTransferEvents.send(context.getString(R.string.data_restored_successfully))
                    syncManager.sync()
                }
                is RestoreResult.PartialFailure -> {
                    val failedNames = result.failed.entries.joinToString { "${it.key.label}: ${it.value}" }
                    _dataTransferEvents.send(
                        context.getString(R.string.restore_partial_unresolved_format, failedNames),
                    )
                    if (result.succeeded.isNotEmpty() || !result.rolledBack) {
                        syncManager.sync()
                    }
                }
                is RestoreResult.TotalFailure -> {
                    _dataTransferEvents.send(context.getString(R.string.restore_failed_format, result.error))
                }
            }
            delay(300)
            _uiState.update { it.copy(isDataTransferInProgress = false, restorePlan = null) }
            _dataTransferProgress.value = null
        }
    }

    fun clearRestorePlan() {
        _uiState.value.restorePlan?.backupUri?.let { planUri ->
            runCatching { backupManager.discardDecryptedBackup(Uri.parse(planUri)) }
        }
        _uiState.update {
            it.copy(
                restorePlan = null,
                backupValidationErrors = emptyList(),
                pendingEncryptedBackupUri = null,
                wrongBackupPassword = false
            )
        }
    }

    fun removeBackupHistoryEntry(entry: BackupHistoryEntry) {
        viewModelScope.launch {
            backupManager.removeBackupHistoryEntry(entry.uri)
        }
    }

}
