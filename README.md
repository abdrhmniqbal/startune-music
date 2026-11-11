# Startune Music

Startune Music is an open-source Android music player. It focuses on local playback, self-hosted music libraries, expressive Material 3 UI, and user-controlled online lookups.

The app works offline by default. Optional online services are disabled until you enable them or sign in.

Package name: `com.startune.music`

## Why This Exists

Startune Music continues the [PixelPlayerOSS](https://github.com/PixelPlayerHQ/PixelPlayerOSS) codebase with its own features and direction, keeping the same FOSS stance: no ads, no analytics, no account needed for local playback.

Cloud playback is limited to self-hosted sources: Navidrome/Subsonic and Jellyfin.

## Features

| Area | Highlights |
| --- | --- |
| Playback | Media3 playback engine, FFmpeg support, gapless playback, crossfade, custom transitions, queue controls, shuffle, repeat, sleep timer, external file playback |
| Library | Local scanning for MP3, FLAC, AAC, OGG, WAV, M4A, albums, artists, genres, folders, favorites, playlists, stats, metadata editing, Top Songs, mixes |
| Self-hosted | Navidrome/Subsonic and Jellyfin login, sync, streaming, artwork, and app-private offline downloads |
| Lyrics | Embedded lyrics, local `.lrc` files, lyrics import/editing, optional LRCLIB lookup, translation, romanization |
| Artwork | Local artwork, album-art palette extraction, optional Deezer artist image lookup |
| Metadata | On-demand MusicBrainz matching for recording, release, and artist identifiers |
| UI | Jetpack Compose, Material 3 Expressive, dynamic color, light/dark/pure black themes, Glance widgets, animated player surfaces |
| Backup | Preferences, playlists, favorites, lyrics, stats, and app state backup/restore (`.starbk` files) |
| Scrobbling | Last.fm and ListenBrainz, after sign-in |

## Online Services

Startune Music separates offline playback from network lookups.

| Service | Purpose | Default |
| --- | --- | --- |
| Navidrome/Subsonic | Self-hosted library sync, streaming, and offline downloads | User login required |
| Jellyfin | Self-hosted library sync, streaming, and offline downloads | User login required |
| MusicBrainz | On-demand metadata matching and identifier enrichment | Only when requested |
| LRCLIB | Search online lyrics when local or embedded lyrics are missing | Off |
| Deezer | Fetch missing artist artwork and cache it locally | Off |
| Last.fm | Scrobble played tracks | User login required |
| ListenBrainz | Scrobble played tracks | User login required |

LRCLIB and Deezer can be enabled during first-run setup or later in Settings. MusicBrainz searches run only when you choose the lookup action for a track.

## Requirements

| Requirement | Version |
| --- | --- |
| Android | 11 or newer, API 30+ |
| JDK | 21 |
| Android SDK | compile/target 37 |

## Build From Source

Clone the repository (the `main` branch holds this app; the `master` branch holds the older TypeScript project):

```sh
git clone -b main https://github.com/abdrhmniqbal/startune-music.git
cd startune-music
```

Build the debug APK:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug
```

Build one universal debug APK for local installation:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleDebug -Pstartune.enableAbiSplits=false
```

Build a universal unsigned release APK suitable for F-Droid verification:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleRelease -Pstartune.enableAbiSplits=false -Pstartune.disableReleaseSigning=true
```

Run unit tests:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest
```

Generate the baseline profile with a connected device or emulator:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :baselineprofile:generateBaselineProfile
```

## Download

GitHub releases:

```text
https://github.com/abdrhmniqbal/startune-music/releases
```

`v2.0.0-beta` is the upcoming first release from the `main` branch.

F-Droid listing for `com.startune.music` is being prepared for the beta release:

```text
https://f-droid.org/packages/com.startune.music/
```

Obtainium app id:

```text
com.startune.music
```

> Note: F-Droid builds and signs its own APKs from source, so they may lag behind GitHub releases while the new version works through the F-Droid build cycle. F-Droid and GitHub APK signatures differ — switching between the two requires an uninstall/reinstall.

## Project Structure

```text
app/src/main/java/com/startune/music/
- data/ Room, repositories, preferences, services, workers
- di/ Hilt modules and qualifiers
- presentation/ Compose screens, components, navigation, ViewModels
- ui/ Theme and Glance widgets
- utils/ Shared utilities
baselineprofile/ Macrobenchmark and baseline profile generation
```

## Tech Stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Jetpack Compose |
| Design | Material 3 Expressive |
| Playback | AndroidX Media3, ExoPlayer, FFmpeg |
| Database | Room |
| Dependency Injection | Hilt |
| Preferences | DataStore |
| Background Work | WorkManager |
| Networking | Retrofit, OkHttp |
| Images | Coil |
| Metadata | TagLib, JAudioTagger |

## Contributing

Contributions are welcome. Open an issue or pull request with a focused change and include test/build results when possible.

Useful local checks:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:compileDebugKotlin
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:lintDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest
```

Translations are managed on Crowdin: https://crowdin.com/project/startune-music/

Release process: [docs/RELEASE.md](docs/RELEASE.md)

F-Droid notes: [docs/FDROID.md](docs/FDROID.md)

Privacy policy: [docs/PRIVACY_POLICY.md](docs/PRIVACY_POLICY.md)

Security policy: [docs/SECURITY.md](docs/SECURITY.md)

## License

Startune Music is licensed under the [GNU General Public License v3.0](LICENSE) (`SPDX-License-Identifier: GPL-3.0-or-later`).

Portions are based on PixelPlayerOSS by Theo Vilardo and Duhan Yağmur Delikkulak.

Distributed APKs include third-party components under their own licenses. In particular, the optional FFmpeg decoder dependency `org.jellyfin.media3:media3-ffmpeg-decoder` is GPL-3.0; see [docs/THIRD_PARTY_NOTICES.md](docs/THIRD_PARTY_NOTICES.md).
