# Release Checklist

Startune Music releases are shipped from `main` by the release
workflow: Crowdin sync first, then checks, then the release. The release
candidate passes local checks and a basic device smoke test before tagging.

## Release Workflow

Run the `Release` workflow from the Actions tab on the `main`
branch, with the version from `gradle.properties` (for example `2.0.0-beta`).
It runs, in order:

1. **Crowdin sync**: uploads the current `res/values` strings and downloads
   the latest translations, committing them to `main`.
2. **Checks**: compile, lint, unit tests (the same gate as PR checks).
3. **Release**: verifies the input version matches `gradle.properties`,
   builds the signed ABI-split release APKs, creates the `v<version>` tag
   and the GitHub release, then attaches the `CHANGELOG.md` section for
   the version as the release notes.

Required repository secrets:

- `CROWDIN_PROJECT_ID` and `CROWDIN_PERSONAL_TOKEN` (Crowdin project API).
- Signing, one keystore for beta and stable (Iqbal's decision
  2026-11-11): `ANDROID_KEYSTORE_BASE64` + `ANDROID_KEYSTORE_PASSWORD` +
  `ANDROID_KEY_ALIAS` + `ANDROID_KEY_PASSWORD` in the `main` environment.
  The workflow fails fast when any of the four is missing; it never
  falls back to a throwaway key.

## Versioning

Version values live in `gradle.properties`:

```properties
APP_VERSION_NAME=2.0.0-beta
APP_VERSION_CODE=2000099
```

For every public release:

1. Update `APP_VERSION_NAME`.
2. Increment `APP_VERSION_CODE`.
3. Move the relevant `CHANGELOG.md` entries from `Unreleased` to the release version.
4. Tag the release as `v<APP_VERSION_NAME>`, for example `v2.0.0-beta`. The
   release workflow creates the tag itself; only tag manually when
   publishing without Actions.

## Required Local Checks

Run these before tagging:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:compileDebugKotlin
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:lintDebug
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:testDebugUnitTest
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleRelease -Pstartune.enableAbiSplits=false -Pstartune.disableReleaseSigning=true
```

For split APK artifacts, use:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew :app:assembleRelease -Pstartune.enableAbiSplits=true
```

## Signing

Release signing is driven by an untracked `keystore.properties` file at the repository root.

Supported keys:

```properties
storeFile=/absolute/or/repo-relative/path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

`storeFile` is optional when the release keystore is available as `vz-pixelplay.jks` at the repository root. `storePassword`, `keyAlias`, and `keyPassword` are required for signing. If signing properties or the keystore file are missing, release builds are unsigned. CI workflows create temporary CI signing keys for artifacts; those are not official release keys.

## Signing Decision (unified keystore)

Beta and stable share one keystore (Iqbal's decision 2026-11-11):
the release workflow signs with `ANDROID_KEYSTORE_BASE64` +
`ANDROID_KEYSTORE_PASSWORD` + `ANDROID_KEY_ALIAS` +
`ANDROID_KEY_PASSWORD` from the `main` environment, and fails fast
when any of the four is missing. The key material is the keystore
generated for the beta CI setup (alias `startune-ci-key`, RSA 4096,
valid to 2054-03-29; backup at `~/workspace/startune-backups/`).
Because beta and stable share the key, beta users can update to
stable in place; because it is the single permanent key, losing it
means the app can never be updated in place on GitHub releases again.
Back up the keystore file and its passwords somewhere safe and never
commit them. The old `CI_KEYSTORE_B64` / `CI_KEYSTORE_PASSWORD`
secrets were removed after the four `ANDROID_KEYSTORE_*` secrets were
set (same key material, new names).

For F-Droid-compatible unsigned verification builds, pass `-Pstartune.disableReleaseSigning=true` even when local signing files exist.

## Device Smoke Test

Install the release candidate and verify:

1. First launch and setup complete.
2. Local library scan finds music.
3. Playback starts, pauses, resumes, skips, and survives backgrounding.
4. Full player opens and closes smoothly.
5. Widget controls still reach the playback service.
6. Navidrome and Jellyfin login screens open.
7. Backup export flow creates a file.

## Publishing

1. Ensure `main` is clean and pushed.
2. Run the `Release` workflow from the Actions tab with the version.
3. The workflow creates the tag, the GitHub release, the APK artifacts,
   and the changelog notes. Verify the release page afterwards.

Without Actions, the manual fallback is: create a tag
`git tag v<APP_VERSION_NAME>`, push it
`git push origin v<APP_VERSION_NAME>`, create a GitHub release from the
tag, attach the APK artifacts, and paste the changelog section.

## F-Droid Metadata

Before submitting a tagged release to F-Droid-compatible app stores:

1. Update `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`.
2. Verify the unsigned universal release build from [FDROID.md](FDROID.md).
3. Check `docs/PRIVACY_POLICY.md` still matches the optional network services present in the app.
4. Create source archives from git, not from the working tree, so ignored local artifacts are excluded.
