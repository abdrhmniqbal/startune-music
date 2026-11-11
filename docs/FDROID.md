# F-Droid

Startune Music is being prepared for F-Droid. The listing does not exist
yet; the draft build recipe below is ready to submit:

```text
https://f-droid.org/packages/com.startune.music/
```

The listing is fed by the Fastlane metadata under `fastlane/metadata/android/en-US`
(summary, full description, changelogs, screenshots).

The official build recipe lives in the `fdroiddata` repository:
`https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/com.startune.music.yml`.
A draft copy of the recipe is kept at `metadata/com.startune.music.yml` in
this repository — submit it to fdroiddata, then keep it in sync with the
official recipe when build configuration changes.

## Submitting the Recipe

1. Fork `fdroiddata` and add `metadata/com.startune.music.yml` from this
   repository.
2. Open a merge request against fdroiddata. F-Droid reviews the recipe and
   runs a test build from source.
3. Once merged, the listing appears and updates flow automatically per
   "How Updates Reach F-Droid" below.

## How Updates Reach F-Droid

The recipe is configured with `AutoUpdateMode: Version` and `UpdateCheckMode: Tags`, so
new releases flow automatically:

1. Bump `APP_VERSION_NAME` and `APP_VERSION_CODE` in `gradle.properties` and tag
   `v<APP_VERSION_NAME>` per [docs/RELEASE.md](RELEASE.md).
2. F-Droid's `checkupdates` bot detects the tag and reads the version name/code from
   `gradle.properties` (`UpdateCheckData: gradle.properties|CODE=(\d+)|.|NAME=(.+)` —
   keep those keys greppable).
3. A new build block is added automatically; F-Droid builds the unsigned release APK
   from source and signs it with the F-Droid signing key.
4. Expect a delay of several days between tagging and the update appearing on F-Droid
   while it works through the build cycle.

F-Droid APKs are signed by F-Droid, not with the project key used for GitHub releases.
The two installations cannot update over each other; switching requires an
uninstall/reinstall.

## Recipe Notes

The official recipe builds `:app:assembleRelease` with
`startune.enableAbiSplits=false` and `startune.disableReleaseSigning=true`,
producing a single universal unsigned APK. Its `prebuild` step strips two things the
F-Droid build servers cannot use:

- the `foojay` toolchain-resolver plugin line in `settings.gradle.kts` (it downloads
  JDKs from the network, which F-Droid builders forbid) — removed with
  `sed -i -e '/foojay/d'`;
- the `-XX:+ParallelRefParsingEnabled` JVM arg in `gradle.properties` — removed with a
  literal `sed` replacement.

When editing those lines in `settings.gradle.kts` or `gradle.properties`, check that the
recipe's `sed` commands still apply cleanly, or update the recipe in `fdroiddata`.

Build from git source or `git archive`, not from a manual copy of the working tree.
Ignored local artifacts such as `app/release/`, `vz-pixelplay.jks`,
`keystore.properties`, and `local.properties` must not be included in any source
tarball.

## Listing Requirements

These properties are required for acceptance and must be maintained:

1. GPL-3.0 license.
2. OSS-focused package name: `com.startune.music`.
3. No Firebase, Crashlytics, Play Store billing, ads, analytics, Cast, Wear OS, or
   Google Play Services runtime dependencies.
4. Optional network services documented in `docs/PRIVACY_POLICY.md`.
5. Release builds left unsigned when local signing keys are absent.
6. Store metadata in Fastlane format.
7. Last.fm and ListenBrainz scrobbling are strictly opt-in and disabled by
   default: nothing is submitted until the user connects their own account,
   and disconnecting deletes queued items. The data sent while opted in is
   documented in `docs/PRIVACY_POLICY.md`.

## Asset Licenses

General third-party notices are tracked in `docs/THIRD_PARTY_NOTICES.md`; a short copy is also bundled at `app/src/main/assets/licenses/THIRD_PARTY_NOTICES.md`.

The dependency license review table is tracked in `docs/DEPENDENCY_LICENSES.md`.

| Asset | Source | License evidence |
| --- | --- | --- |
| `app/src/main/res/font/gflex_variable.ttf` | Google Sans Flex, `https://fonts.google.com/specimen/Google+Sans+Flex/license?preview.script=Latn` | Google Fonts license page declares SIL Open Font License 1.1; OFL text is included at `app/src/main/assets/licenses/OFL.txt`. |
| `app/src/main/res/font/genre_variable.ttf` | Roboto Flex, `https://github.com/googlefonts/roboto-flex` | SIL Open Font License 1.1 text is included at `app/src/main/assets/licenses/OFL.txt`. |

## Native And Binary Maven Artifacts

These artifacts can produce native `.so` files in the APK. Keep the source/license trail current when versions change.

The project source is GPL-3.0-licensed. Release APKs also include `org.jellyfin.media3:media3-ffmpeg-decoder`, whose Maven POM declares GPL-3.0 — consistent with the source license. Preserve the source/license trail below.

| Artifact | Native library seen in APK | License/source evidence |
| --- | --- | --- |
| `org.jellyfin.media3:media3-ffmpeg-decoder:1.9.0+1` | `libffmpegJNI.so` | Maven POM declares GPL-3.0 and SCM `https://github.com/jellyfin/jellyfin-androidx-media`. |
| `io.github.kyant0:taglib:1.0.6` | `libtaglib.so` | Maven POM declares Apache-2.0 and SCM `https://github.com/Kyant0/taglib`. |
| `androidx.graphics:graphics-shapes:1.1.0` | `libandroidx.graphics.path.so` | Google Maven POM declares Apache-2.0 and SCM `https://android.googlesource.com/platform/frameworks/support`. |

## JitPack Dependencies

`settings.gradle.kts` allows JitPack only for these groups:

| Group | Why it is allowed | License/source evidence |
| --- | --- | --- |
| `com.github.racra` | Direct dependency `libs.smooth.corner.rect.android.compose`. | POM and upstream repository declare MIT License: `https://github.com/racra/smooth-corner-rect-android-compose`. |
| `com.github.philburk` | Transitive dependency `com.github.philburk:jsyn` required by `androidx.media3:media3-exoplayer-midi`. | POM declares Apache-2.0 and SCM `https://github.com/philburk/jsyn`. |

## Local Verification Build

Reproduce the F-Droid-style artifact locally — a universal unsigned release APK. Pass `startune.disableReleaseSigning=true` so local ignored signing files cannot affect the artifact:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleRelease -Pstartune.enableAbiSplits=false -Pstartune.disableReleaseSigning=true
```

Expected artifact:

```text
app/build/outputs/apk/release/app-release-unsigned.apk
```

Run the standard checks before tagging a release:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:compileDebugKotlin
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:lintDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest
```
