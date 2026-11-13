# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Last opened tab option in Default Tab settings: the app opens on the tab you last used (Home, Search, or Library).
- Full theme customization: every color role can now be set explicitly in the theme editor (containers, surface levels, text colors, outline, fixed and inverse colors), with an Auto badge when a role is still derived. The color picker now has a full-spectrum rainbow picker (saturation/value square plus hue slider) alongside hex and presets; an override can be reset to Auto.
- All eight built-in themes recreated from their official palettes: foreground, surface container ramp, primary container, muted text and outline now use the real theme values instead of the auto-derived guesses.
- Rainbow colors now follow the selected theme: genre cards and the Settings category icons share one rainbow palette, taken from the theme's official accent palette (Nord Aurora, Catppuccin accents, Dracula, Gruvbox brights, Tokyo Night, Solarized accents, Everforest, Ayu), so the app's colorful elements look like the theme itself. The rainbow is customizable in the theme editor (8 slots per light/dark variant, with an Auto badge when a slot is still derived) and travels in the exported theme JSON.
- The rainbow palette is now consistent: the earlier version mixed the four muted containers with the four saturated accents, so half the colorful elements looked faded and half looked bright. The stored palette uses the official accents at one consistent level, and the derived fallback (for custom themes without a stored rainbow) generates every color at the same lightness and saturation.
- Theme export is now a full theme (format v3): the exported JSON defines every overridable role explicitly (42 roles per light/dark variant) instead of only the six base roles plus the overrides the author happened to set, so an exported theme renders identically no matter how the mapper's derivation changes later. Roles the derivation defines with an alpha are composited over the surface to their effective opaque hex. Version 2 files still import the same way.

### Fixed
- Changelog now loads from the raw CHANGELOG.md file instead of the GitHub API, which failed on shared mobile data.
- Home top bar now shows the app name instead of "Stable" or "Debug".
- Accent containers now sit correctly against the surface in every color scheme, light and dark.
- Empty states unified into two shared patterns, with and without a container.
- Collapsed playlist top bar: the title no longer runs under the Sort and More actions, and it is centered on the navigation row.
- Expanded playlist title is now centered instead of left-aligned.
- Mix cards are visible in every theme again: the official raised levels sit only about 3 lightness points above the background in every theme (not just Solarized, Everforest and Ayu), so a surfaceContainer card blended into the page. All eight themes now use the derived container ramp (which keeps its 7-point dark / 5-point light separation) instead of the too-close official values.
- The current song row in the song list is highlighted again: primaryContainer was overridden to each theme's selection color, which sits only about 2-8 lightness points from the normal row background, so the highlight blended into the list. primaryContainer is now derived by the mapper (7 points beyond the surface, primary hue kept), so the playing song stands out in every theme.
- Nord theme now follows the official Nord palette exactly: the rainbow is pure Aurora (nord11-15, red/orange/yellow/green/purple) with no Frost blue mixed in; the dark surface is nord1 (the elevated level for panels and buttons) instead of nord2 (the selection color); the light primary is nord8, the official primary accent, which the Frost palette uses identically in light and dark.
- Nord dark muted text now uses nord5, the official subtle-text color for dark ambiance, instead of a custom gray-blue that is not a Nord color. Every hex code in the Nord theme is now an official Nord palette color.
- Nord rainbow has no more duplicates: Aurora has only five colors, so the other three slots are new colors with the same feel (Aurora's average lightness and saturation): lime #BDCA7D, violet #967DCA and pink #CA7DA3, all eight slots distinct and in hue order.
- Every built-in theme now uses only its official palette colors, verified against the official source for each theme. Dracula light (Alucard) now uses the real Alucard colors: foreground #1F1F1F instead of the dark theme's background, comment #6C664B instead of the dark comment, and the Alucard accent rainbow instead of the dark Dracula accents. Everforest outlines now use the official medium bg3 level. Tokyo Night light (Day) now uses the enkia Light foreground #343B58 and comment #9699A3, and its rainbow uses the official Day bright red #FF4774 and bright magenta #A463FF. Ayu now uses the real light and dark accent rainbows (the two variants had shared one rainbow with three colors that belong to neither), the official foregrounds #5C6166 / #BFBDB6, muted colors #828E9F / #5A6378, and the official light panel white #FFFFFF. Catppuccin, Gruvbox, Solarized, Tokyo Night dark and Dracula dark were already fully official and unchanged.
- Now Playing bottom row (shuffle, lyrics, repeat) inactive buttons are visible again. The old inactive pair was a transparent pill (onSurface at 7% alpha, so it showed the dark bar through it) with a black onPrimaryContainer icon, measured from a screenshot as 1.96:1, effectively invisible. Each button now uses a muted version of its own Fixed color at 50% alpha with its Fixed on-color, which is light with a dark icon in both light and dark schemes: icon vs pill 6.41:1, pill vs bar 3.41:1, and still clearly dimmer than the active pill.
- Catppuccin light surface now uses mantle #E6E9EF instead of surface0 #CCD0DA. surface0 is a real Catppuccin color but the wrong role here: it is a medium gray, a big jump from the near-white base #EFF1F5, so the whole light theme looked gray instead of bright Latte. Mantle is Catppuccin's own panel color, one small step from base. The dark surface stays surface0 #313244, which is the correct lighter elevated surface for Mocha.
- Genre detail page title is readable again. The big genre title (e.g. Afro-Beat) was black on a black background, measured from a screenshot as 2.74:1. The title color came from the genre color's on-color, which is correct for the solid container color (black on the mauve container is 7.41:1), but the title sits at the bottom of the header where the vertical gradient has faded to almost transparent, so it is actually drawn over the dark page background. The title now uses the page background's on-color, so it is light on dark and dark on light (7.67:1 on the screenshot's background). The back button and genre illustration keep the container's on-color because they sit at the top over the strong gradient, where that color is correct.
- Dracula dark low-emphasis text is readable again. The search placeholder, bottom nav unselected labels, and bookmark icon were invisible because they all use onSurfaceVariant, which was the Dracula comment color #6272A4: only 3.03:1 on the background, 1.94:1 on the surface, and 1.12:1 on surfaceContainerHighest (the bookmark icon's container). The Dracula palette has no lighter muted text color, so the dark onSurfaceVariant is now the foreground #F8F8F2, the only official readable option: 13.36:1 on the background, 8.59:1 on the surface, 4.93:1 on surfaceContainerHighest. The light theme keeps #6C664B, which is readable at 5.33:1 on its background.

## [2.0.0-beta] - 2026-11-11

First beta of the Kotlin rewrite: Startune Music rebuilt as a native Kotlin app, analytics-free.

> If you installed one of the earlier test APKs, uninstall it first. This beta is signed with the release keystore, so it cannot update over the throwaway-signed test builds.

## [1.0.5] - 2026-07-24

### ⚙️ Changed

- Replaced `useGuardedRouter` 900ms guard delay with direct router calls to prevent dropped navigation events and blank screens. (@abdrhmniqbal)

### 🧩 Fixed

- MiniPlayer component maintained persistently in DOM tree with transform animations to prevent disappearing on rapid transitions. (@abdrhmniqbal)
- Removed `isFetching` check from artist & album detail screens to prevent infinite loading spinners on background refetches. (@abdrhmniqbal)
- Created Expo Config Plugin (`with-startune-modules`) to auto-register custom native modules (`BatteryOptimizationModule`, `AppUpdaterModule`) in `MainApplication.kt` during prebuilds and EAS builds. (@abdrhmniqbal)
- Fixed GitHub Actions release workflow 403 error by using `workflow_call` and `contents: write` permissions for attaching release notes. (@abdrhmniqbal)
- Prevented repeated Deezer API queries for artists with existing cached artwork and added an in-memory query cache for artist image lookups. (@abdrhmniqbal)

## [1.0.4] - 2026-07-21

### ⚙️ Changed

- Replaced `react-native-audio-browser` with custom pure modules and explicitly pin Node 24 for APK releases. (@abdrhmniqbal)
- Consolidated navigation, logging, and localization wrappers. (@abdrhmniqbal)
- Player projector now syncs based on meaningful state updates rather than playback ticks to prevent UI stutters. (@abdrhmniqbal)
- Moved Deezer artist queries entirely to the client side, dropping Last.fm integration for bios. (@abdrhmniqbal)
- Cleaned up build scripts: migrated to Bun, removed dead metadata retrievers, and pinned trusted deps. (@abdrhmniqbal)

### 🧩 Fixed

- Playlist active track hydration no longer gets wiped on restore. (@abdrhmniqbal)
- Queue list fast-scroll crash fixed by dropping mismatched `getItemLayout`. (@abdrhmniqbal)
- Player no longer forces full re-render of queue items on every tick. (@abdrhmniqbal)
- TrackInfo anchors to bottom reliably in compact, lyrics, and queue views. (@abdrhmniqbal)
- Last.fm scrobbler now sends only the primary artist name. (@abdrhmniqbal)
- Notification handlers execute at the right time, decoupled from render bodies. (@abdrhmniqbal)
- Deezer search prefers exact artist matches before falling back to normalized ones. (@abdrhmniqbal)

## [1.0.3] - 2026-07-09

### ✨ Added

- Dev builds default to extra logging so contributors get full diagnostics without manually enabling it. (@abdrhmniqbal)

### ⚙️ Changed

- Categorized the indexer module into cohesive submodules (scan, metadata, external, progress, state, utils) and moved batch utilities into `indexer/utils/batch`. (@abdrhmniqbal)
- Redesigned the now-playing equalizer loader into a continuous, seamless wave with proportional sizing; larger on grid carousels. (@abdrhmniqbal)

### 🧩 Fixed

- Version comparison ignores the local `-dev` build marker so dev builds no longer offer a spurious app update. (@abdrhmniqbal)
- Bottom navigation hides while scrolling and reappears after scroll settles, removing the hide→show→hide flicker. (@abdrhmniqbal)

## [1.0.2] - 2026-07-08

### ⚙️ Changed

- Refactored lyrics module into a single parser and `useLyrics` hook, removing workarounds and duplicate files. (@abdrhmniqbal)
- Collapsed duplicated zoom-transition helpers and thin navigation wrappers. (@abdrhmniqbal)
- Refactored notifications: fixed dedup key, extracted shared pending-route flush, table-driven indexer actions. (@abdrhmniqbal)
- Replaced bootstrap waiter registry with a single bootstrap completion promise. (@abdrhmniqbal)
- Collapsed duplicated cast command try/catch/log boilerplate into one helper. (@abdrhmniqbal)
- Deleted dead history cache-service and duplicated write functions; consolidated play recording into `tracks/repository`. (@abdrhmniqbal)
- Redesigned runtime error state with the canonical `EmptyState`; removed `DatabaseProvider` pass-through. (@abdrhmniqbal)

### 🧩 Fixed

- Last.fm artist refresh: avoid HTTP 406, isolate per-artist failures, add resume cursor. (@abdrhmniqbal)
- Notification-open blank screen: drop stale launch notification replay and gate route until router ready. (@abdrhmniqbal)
- Mini-player not restoring after idle/paused state on reopen. (@abdrhmniqbal)
- Replaced deprecated `FileSystem.downloadAsync` with `File.downloadFileAsync` in artwork cache. (@abdrhmniqbal)
- Bottom bar stays hidden on short (non-scrollable) lists; auto-shows after scroll idle. (@abdrhmniqbal)

## [1.0.1] - 2026-07-04

### ✨ Added

- Native APK download & install with notification progress for in-app updates. (@abdrhmniqbal)
- Battery Optimization as Expo native module (auto-linked, no manual registration). (@abdrhmniqbal)
- Static theme colors auto-generated from CSS as single source of truth. (@abdrhmniqbal)
- Auto-link raw URLs in release notes with shortened display text. (@abdrhmniqbal)

### ⚙️ Changed

- Migrated from app.json to app.config.ts with Expo Modules API for native code. (@abdrhmniqbal)
- Upgraded to Expo SDK 57. (@abdrhmniqbal)
- Migrated file-path.ts from expo-file-system/legacy to modern File/Directory API. (@abdrhmniqbal)
- App-update sheet redesigned with proper snap points (48%, 88%) and matching footer color. (@abdrhmniqbal)
- Removed unused exports (101 symbols across 49 files) and consolidated duplicate utilities. (@abdrhmniqbal)
- Extracted shared detail screen hooks (useArtistDetailData, usePlaybackActions, useDetailScrollHandlers). (@abdrhmniqbal)

### 🧩 Fixed

- Album card crash when handlePress is undefined in horizontal grid. (@abdrhmniqbal)
- Kotlin JVM target mismatch on react-native-image-colors causing build failures. (@abdrhmniqbal)
- All lint warnings resolved (unused imports, useless escapes, unsafe finally, exhaustive-deps). (@abdrhmniqbal)
- Update sheet text now non-selectable with consistent drag handler. (@abdrhmniqbal)

## [1.0.0] - 2026-06-30

### ✨ Added

- 15 new themes: Default, Nord, Dracula, Catppuccin, Tokyo, Gruvbox, Everforest, Rose Pine,
  Solarized, Ayu, Monochrome, Aquamarine, Crimson Pulse, Banana Breeze, Candy Pop. (@abdrhmniqbal)
- Theme search filter on settings screen. (@abdrhmniqbal)
- Opus file artwork extraction support. (@abdrhmniqbal)
- Mic-01 and Playlist-03 icons. (@abdrhmniqbal)

### ⚙️ Changed

- Top-tracks time picker changed from BottomSheet to inline Tabs. (@abdrhmniqbal)
- Top-tracks chart now uses unified data source across all time ranges. (@abdrhmniqbal)
- Pull-to-refresh removed from top-tracks (uses scroll-driven refresh only). (@abdrhmniqbal)
- Native header text size increased app-wide. (@abdrhmniqbal)
- Tabs styling updated to match app color conventions. (@abdrhmniqbal)
- Lyrics zoom level now persists between sessions. (@abdrhmniqbal)
- Library tabs now require explicit long-press drag to reorder, preventing accidental reordering. (@abdrhmniqbal)

### 🧩 Fixed

- Playlist form scrolling blocked when touch starts on track list area. (@abdrhmniqbal)
- Mix artwork missing on action sheet. (@abdrhmniqbal)
- Mix visuals no longer overlap with reserved genre colors. (@abdrhmniqbal)
- Sleep timer not applying selected duration correctly. (@abdrhmniqbal)
- Settings search missing some entries. (@abdrhmniqbal)

## [1.0.0-rc.2] - 2026-06-29

### ✨ Added

- Custom sort order option on playlist detail screen. (@abdrhmniqbal)
- Update timestamp display on mix detail page. (@abdrhmniqbal)

### ⚙️ Changed

- Restructured settings page layout for visual consistency. (@abdrhmniqbal)
- Unified playlist creation, editing, and save-to-queue flows. (@abdrhmniqbal)

### 🧩 Fixed

- Mixes not refreshing after daily/weekly DB regeneration. (@abdrhmniqbal)
- Sheet components crashing or failing to mount in library genres and album detail views. (@abdrhmniqbal)
- Full player not dismissable when launched from external file deep-links. (@abdrhmniqbal)
- Action sheet content clipped due to conflicting snap points. (@abdrhmniqbal)
- Multi-artist split name display formatting. (@abdrhmniqbal)

## [1.0.0-rc.1] - 2026-06-25

### ✨ Added

- Adaptive theme system with Nord, Catppuccin (Latte/Mocha), Dracula, and Alucard variants. (@abdrhmniqbal)
- Last.fm integration with artist bio, artwork, scrobbling, and authentication. (@abdrhmniqbal)
- Daily Mix and For You Mix with custom layouts and persisted state. (@abdrhmniqbal)
- Collection action sheet and artwork grid for mixes. (@abdrhmniqbal)
- Backup and restore settings with automatic backup option. (@abdrhmniqbal)
- Onboarding flow covering theme, folder filters, permissions, and battery settings. (@abdrhmniqbal)
- Search indexing with auto-scroll and UI highlighting. (@abdrhmniqbal)
- Top-tracks timespan picker with chart update time display. (@abdrhmniqbal)
- Album artist metadata support. (@abdrhmniqbal)
- Play and shuffle buttons on artist detail page. (@abdrhmniqbal)
- Expanded media action sheets across library views. (@abdrhmniqbal)
- Reorderable and toggleable library tabs via settings. (@abdrhmniqbal)
- Force-update mixes action with danger dialog. (@abdrhmniqbal)
- Reset search history option in settings. (@abdrhmniqbal)
- User feedback feature. (@abdrhmniqbal)
- Polled progress for time indicator. (@abdrhmniqbal)
- Fallback to lrclib API when local lyrics are missing. (@abdrhmniqbal)
- Lyrics extracted and persisted during indexing for faster playback. (@abdrhmniqbal)

### ⚙️ Changed

- Last.fm authentication reworked for improved reliability. (@abdrhmniqbal)
- Direction-aware entering/exiting animations for library tabs. (@abdrhmniqbal)
- Icons added to player action menu. (@abdrhmniqbal)
- Faster app startup by deferring cached track loading and initial scan. (@abdrhmniqbal)
- Redesigned track metadata sheet UI. (@abdrhmniqbal)
- Redesigned Last.fm integration flow with shared bottom-sheet inputs. (@abdrhmniqbal)
- Refined backup UI flow and consolidated automatic backup options. (@abdrhmniqbal)
- Redesigned settings layout grouped by intent. (@abdrhmniqbal)
- Improved rainbow color contrast across themes. (@abdrhmniqbal)
- Removed outer borders on settings to match borderless style. (@abdrhmniqbal)
- Refined What's New typography. (@abdrhmniqbal)
- Smoother audio crossfade transitions. (@abdrhmniqbal)
- Improved track action sheet design. (@abdrhmniqbal)

### 🧩 Fixed

- Shuffle not applying correctly when starting playback. (@abdrhmniqbal)
- Player queue rendering, shuffle settings, history scrobbling, and playlist form layout. (@abdrhmniqbal)
- Playback not preserved when app is reopened or dismissed. (@abdrhmniqbal)
- Startup playback and settings state not restored correctly. (@abdrhmniqbal)
- Duplicate mix visual presets causing overlapping colors. (@abdrhmniqbal)
- Random blank screen after navigation. (@abdrhmniqbal)
- Playlist form track list not scrollable. (@abdrhmniqbal)
- Local track artwork overwriting Last.fm artist artwork. (@abdrhmniqbal)
- Albums not split by effective album artist. (@abdrhmniqbal)
- Last.fm returning invalid fallback art or placeholder images. (@abdrhmniqbal)
- Last.fm authentication not working with mobile flow. (@abdrhmniqbal)
- Restore backup CTA appearing in wrong step. (@abdrhmniqbal)
- Duplicate artist entries in indexer causing duplicates in lists. (@abdrhmniqbal)
- Poor contrast on artist detail overlay. (@abdrhmniqbal)
- Slow external file playback handoff. (@abdrhmniqbal)
- Sleep timer not stopping at track end or play count limit. (@abdrhmniqbal)
- Startune Music deep-link scheme not registered. (@abdrhmniqbal)
- Notification player deep-link broken. (@abdrhmniqbal)
- Repeat mode behavior incorrect at queue end and on manual skips. (@abdrhmniqbal)
- Metadata year not parsed correctly from date-like values. (@abdrhmniqbal)
- Malformed UTF-16 prefix in lyrics causing display issues. (@abdrhmniqbal)
- Build failures from missing generated files and invalid dependency version. (@abdrhmniqbal)

## [0.3.0] - 2026-05-03

### ✨ Added

- Help Translate entry in About settings with Crowdin integration groundwork. (@abdrhmniqbal)

### ⚙️ Changed

- Simplified What's New experience; release notes now sourced from GitHub Releases and repository CHANGELOG. (@abdrhmniqbal)
- Refined player and sleep timer layouts for better spacing, stability, and clearer timer state. (@abdrhmniqbal)
- Improved lyrics rendering for timed karaoke markup and wrapped word handling. (@abdrhmniqbal)

### 🧩 Fixed

- Karaoke wrapping showing leading spaces or splitting lyric fragments incorrectly. (@abdrhmniqbal)
- Custom sleep timer selection not waiting for confirmation before applying. (@abdrhmniqbal)
- Update notification not opening latest version popup on tap. (@abdrhmniqbal)

## [0.3.0-rc.5] - 2026-05-02

### ✨ Added

- Sleep timer settings to customize auto-stop behavior. (@abdrhmniqbal)
- Customizable "count as played" threshold value. (@abdrhmniqbal)

### ⚙️ Changed

- Improved player layout with refined controls and better spacing. (@abdrhmniqbal)
- Better karaoke handling for timed lyrics. (@abdrhmniqbal)
- Refined sleep timer option layout with aligned controls. (@abdrhmniqbal)
- Removed unnecessary lyrics view and search route. (@abdrhmniqbal)
- Improved performance by moving database sync outside render phase. (@abdrhmniqbal)

### 🧩 Fixed

- Queue view remounting on every track change, causing previous tracks to disappear. (@abdrhmniqbal)
- Changelog showing versions above installed app version. (@abdrhmniqbal)
- Lyrics screen crash when lyrics unavailable. (@abdrhmniqbal)
- Scale loader animation not animating in some cases. (@abdrhmniqbal)
- Update checker not detecting available versions. (@abdrhmniqbal)
- External player integration not working. (@abdrhmniqbal)

## [0.3.0-rc.4] - 2026-04-30

### ✨ Added

- App update checker. (@abdrhmniqbal)
- Open source license screen. (@abdrhmniqbal)

### ⚙️ Changed

- Improved realtime lyrics performance. (@abdrhmniqbal)
- Improved artwork caching. (@abdrhmniqbal)
- Filter chip on favorites list now hides when no items available. (@abdrhmniqbal)

### 🧩 Fixed

- Split multiple values input not recognizing keyboard input. (@abdrhmniqbal)
- External audio intent handling. (@abdrhmniqbal)

## [0.3.0-rc.3] - 2026-04-29

### ✨ Added

- Save queue to playlist feature. (@abdrhmniqbal)
- "Playing from" indicator on player. (@abdrhmniqbal)
- Track count sort option on genres list. (@abdrhmniqbal)
- Play queue based on search result. (@abdrhmniqbal)

### ⚙️ Changed

- Redesigned favorites list. (@abdrhmniqbal)
- Improved search screen consistency with other screens. (@abdrhmniqbal)
- Improved settings categorization. (@abdrhmniqbal)
- Enhanced audio playback settings with transition and resume behavior. (@abdrhmniqbal)

### 🧩 Fixed

- Not all songs added to queue when using "view more" on lists. (@abdrhmniqbal)
- External audio playback handling. (@abdrhmniqbal)

## [0.3.0-rc.2] - 2026-04-28

### ✨ Added

- Configurable multi-value splitting for artists and genres. (@abdrhmniqbal)
- More language options. (@abdrhmniqbal)
- Incremental indexer updates (no full rescan needed). (@abdrhmniqbal)

### ⚙️ Changed

- Improved split multiple values settings and metadata handling. (@abdrhmniqbal)
- Improved settings page layout. (@abdrhmniqbal)
- Shuffle state now preserved between sessions. (@abdrhmniqbal)

### 🧩 Fixed

- Multiple artists not displayed on UI. (@abdrhmniqbal)
- Navigation issue on multiple value picker. (@abdrhmniqbal)
- Search not working properly. (@abdrhmniqbal)
- Artist artwork not uniform across all screens. (@abdrhmniqbal)
- Queue resetting on track change. (@abdrhmniqbal)
- Deep-link not matching route for external file intents. (@abdrhmniqbal)

## [0.3.0-rc.1] - 2026-04-27

### ✨ Added

- i18n internationalization for multiple languages. (@abdrhmniqbal)
- Audio crossfade configuration. (@abdrhmniqbal)
- Reset listening history option with confirmation dialog. (@abdrhmniqbal)
- Session-only lyric preferences for karaoke mode and zoom level. (@abdrhmniqbal)

### ⚙️ Changed

- Crossfade setting now applied during playback. (@abdrhmniqbal)
- Improved indexer performance for batch processing. (@abdrhmniqbal)
- Player and notification icons now use visible white assets. (@abdrhmniqbal)

### 🧩 Fixed

- Audio file open intents not working. (@abdrhmniqbal)
- Artist header actions unstable. (@abdrhmniqbal)
- Track list action spacing inconsistent. (@abdrhmniqbal)
- Lyrics not caching resolved sources. (@abdrhmniqbal)
- Completed indexer notification tap not handled. (@abdrhmniqbal)
- Angle-bracket and timed markup lyrics not supported. (@abdrhmniqbal)
- Playlist detail tracks not sorted properly. (@abdrhmniqbal)
- Notification icon not uniform across Android versions. (@abdrhmniqbal)
- Missing track details causing React Query instability after file deletion. (@abdrhmniqbal)
- Search input overlapping back button during focus transition. (@abdrhmniqbal)
- Player drag-to-close responding from entire interface instead of drag handle only. (@abdrhmniqbal)
- Duplicate rapid taps to same navigation target causing navigation issues. (@abdrhmniqbal)
- Decorative chevrons on playlist and folder rows blocking row taps. (@abdrhmniqbal)
- Back button on detail screens requiring two taps. (@abdrhmniqbal)

## [0.2.2] - 2026-04-25

### ✨ Added

- Dedicated full-player route with zoom-style presentation. (@abdrhmniqbal)
- "Recently Added" search destination for newly indexed tracks. (@abdrhmniqbal)
- Playback session snapshots: queue position restored after app restart. (@abdrhmniqbal)
- Track counts on genre cards. (@abdrhmniqbal)
- Shared themed refresh control across major screens. (@abdrhmniqbal)

### ⚙️ Changed

- Refined album, artist, playlist, and player transitions with zoom presentation. (@abdrhmniqbal)
- Reworked search screen composition and keyboard handling. (@abdrhmniqbal)
- Refreshed home, library, settings, and shared UI surfaces. (@abdrhmniqbal)
- More reliable play count and listening history recording. (@abdrhmniqbal)

### 🧩 Fixed

- Detail-screen back navigation requiring two taps. (@abdrhmniqbal)
- Search back button overlapping typed query text during focus transition. (@abdrhmniqbal)
- Recent-search playlist artwork showing single image instead of collage. (@abdrhmniqbal)
- Zoom-transition backgrounds not rendering correctly. (@abdrhmniqbal)
- Expo Router "Too many screens defined" warnings from extraneous Home route declarations. (@abdrhmniqbal)
- Back button overlapping search input. (@abdrhmniqbal)

## [0.2.1] - 2026-04-16

### ⚙️ Changed

- Playlist picker selection handling unified for player and track action sheets. (@abdrhmniqbal)

### 🧩 Fixed

- Duplicate navigation/tab bars on search detail screens (album, artist, playlist). (@abdrhmniqbal)

## [0.2.0] - 2026-04-10

### ✨ Added

- "Recently Added" tracks on Search home screen. (@abdrhmniqbal)
- Recent search history with richer targets (albums, artists, playlists). (@abdrhmniqbal)
- Search detail routes for albums, artists, and playlists. (@abdrhmniqbal)
- Genre browsing as a first-class Library tab. (@abdrhmniqbal)
- Indexer run snapshots with retry/backoff handling. (@abdrhmniqbal)

### ⚙️ Changed

- Simplified player session restore for better long-background recovery. (@abdrhmniqbal)
- Removed skeleton loading UIs in favor of direct content and empty states. (@abdrhmniqbal)
- Enhanced grid and list component layout and performance. (@abdrhmniqbal)

### 🧩 Fixed

- Shuffle and queue updates interrupting active playback. (@abdrhmniqbal)
- Playback state transitions unstable when switching tracks from lists. (@abdrhmniqbal)
- Search and detail routing inconsistencies. (@abdrhmniqbal)
- Artwork fallback inconsistent across track, album, and artist surfaces. (@abdrhmniqbal)
- Indexer notifications and autoscan timing unreliable. (@abdrhmniqbal)
- Genre-related screens not visible. (@abdrhmniqbal)
- Artist image fallback on album lists. (@abdrhmniqbal)
- Native repeat mode not preserved across queue resets. (@abdrhmniqbal)
- Stale "preparing" notification overwriting completion notification. (@abdrhmniqbal)

## [0.2.0-rc.2] - 2026-04-02

### ✨ Added

- System notification controls for indexing (pause, resume, cancel, open library). (@abdrhmniqbal)
- Google Cast controls in full player. (@abdrhmniqbal)

### ⚙️ Changed

- Improved navigation transitions for media detail routes. (@abdrhmniqbal)
- Faster indexing with parallel metadata extraction and artwork caching. (@abdrhmniqbal)

### 🧩 Fixed

- Background playback and foreground autoscan freezing. (@abdrhmniqbal)
- Player state not resyncing after shuffle and skip. (@abdrhmniqbal)
- Artist artwork inconsistent between tabs and detail screens. (@abdrhmniqbal)
- Incremental indexing not responding and history not refreshing after playback. (@abdrhmniqbal)
- Progress indexer notification spamming. (@abdrhmniqbal)
- Repeated media permission prompts during autoscan. (@abdrhmniqbal)

## [0.2.0-rc.1] - 2026-03-28

### ✨ Added

- Synchronized lyrics support for embedded lyrics, `.lrc`, and TTML formats. (@abdrhmniqbal)

### ⚙️ Changed

- Settings and preferences consolidated into clearer module ownership. (@abdrhmniqbal)
- Player session persistence separated into dedicated module. (@abdrhmniqbal)

### 🧩 Fixed

- Repeated media permission prompts during autoscan. (@abdrhmniqbal)
- Home history not refreshing after playback activity. (@abdrhmniqbal)

## [0.1.0] - 2026-03-12

### ✨ Added

- Offline local music playback with queue, repeat, shuffle, seeking, and background playback. (@abdrhmniqbal)
- Library browsing for tracks, albums, artists, genres, favorites, folders, and playlists. (@abdrhmniqbal)
- Playlist creation, editing, reordering, and track action sheet flows. (@abdrhmniqbal)
- Full-player UI, mini player, queue view, artist and album detail screens. (@abdrhmniqbal)
- Folder filters, track-duration filters, force reindex, autoscan, battery optimization, and logging controls. (@abdrhmniqbal)
- Track metadata inspection and removable local files. (@abdrhmniqbal)

### ⚙️ Changed

- Major UI redesign across home, library, search, album, artist, playlist, player, and settings. (@abdrhmniqbal)
- Adopted Expo Router with bottom-sheet based full-player flows and HeroUI controls. (@abdrhmniqbal)

### 🧩 Fixed

- Search route handling, navigation history, queue sorting, playback resume, and indexing updates. (@abdrhmniqbal)
- Artist image fallback and genre metadata handling. (@abdrhmniqbal)

## [0.1.0-rc.3] - 2026-03-02

### ✨ Added

- About screen. (@abdrhmniqbal)

### 🧩 Fixed

- History not recorded on track repeat. (@abdrhmniqbal)
- Track action sheet not appearing on first long-press. (@abdrhmniqbal)
- Bottom padding missing on list components. (@abdrhmniqbal)
- Album artist metadata never indexed. (@abdrhmniqbal)
- History missing for manual track selection. (@abdrhmniqbal)
- Routes unable to handle special characters properly. (@abdrhmniqbal)
- Folder filters applied without user confirmation. (@abdrhmniqbal)
- Lists not refreshing immediately after applying filters. (@abdrhmniqbal)

## [0.1.0-rc.2] - 2026-02-28

### ✨ Added

- Player action sheet. (@abdrhmniqbal)
- Full-player bottom sheet. (@abdrhmniqbal)
- Add tracks to playlist from track action sheet. (@abdrhmniqbal)
- Playlist item reordering. (@abdrhmniqbal)
- Queue list reordering. (@abdrhmniqbal)
- Battery optimization settings. (@abdrhmniqbal)

### ⚙️ Changed

- Restructured settings page. (@abdrhmniqbal)
- Full-player UI improvements. (@abdrhmniqbal)
- Track action sheet improvements. (@abdrhmniqbal)
- Upgraded HeroUI sliders. (@abdrhmniqbal)

### 🧩 Fixed

- Inconsistent interface elements across screens. (@abdrhmniqbal)
- Notification click not matching correct route. (@abdrhmniqbal)
- Color contrast on full-player background. (@abdrhmniqbal)
- Notification icon issues. (@abdrhmniqbal)
- Open file functionality. (@abdrhmniqbal)
- Sort logic not handling special characters. (@abdrhmniqbal)
- Selected items not showing on track picker sheet. (@abdrhmniqbal)
- Case sensitivity in sort values. (@abdrhmniqbal)
- Database init blocking UI. (@abdrhmniqbal)
- Playback session not persisting. (@abdrhmniqbal)
- Bottom nav not hiding while scrolling on library tabs. (@abdrhmniqbal)
- Search tab resetting on clear input. (@abdrhmniqbal)
- Albums and artists not filtered correctly. (@abdrhmniqbal)

## [0.1.0-rc.1] - 2026-02-19

### ✨ Added

- Initial project setup and basic UI scaffolding. (@abdrhmniqbal)
- Local library indexing and playback foundations. (@abdrhmniqbal)
- Early iterations of queueing, sorting, and favorites. (@abdrhmniqbal)
