# Odysee for Android

Native Android client for [Odysee](https://odysee.com), written in Kotlin with Jetpack Compose. This is a from-scratch rewrite replacing the previous Cordova-based app.

## Distributions

Three product flavors built from this codebase:

| Flavor | Application ID    | Distribution                                                                                  |
| ------ | ----------------- | --------------------------------------------------------------------------------------------- |
| `play` | `com.odysee.app`  | Google Play Store. Includes Firebase Cloud Messaging and Google Cast.                         |
| `apk`  | `com.odysee.app`  | Direct APK distributed from `apk.odysee.tv`. Same Google services as `play`, plus a built-in updater that checks `https://apk.odysee.tv/release_native.json`. |
| `foss` | `com.odysee.floss`| F-Droid / fully open source. No Firebase, no Google Cast, no updater. Falls back to a poller for push notifications. |

The `play` and `apk` flavors share most of their source via `app/src/playAndApk/`; only the updater module differs.

## Tech stack

- **Kotlin 2.1** + **Jetpack Compose** (Material 3)
- **Hilt** for DI
- **Retrofit** + **kotlinx.serialization** for networking
- **Media3 (ExoPlayer)** for playback, plus a custom floating overlay player and Chromecast support on `play`/`apk`
- **Room** + **DataStore Preferences** for local persistence
- **WorkManager** + a foreground service for large uploads (`tus-java-client`)

## Project structure

```
app/                         Application module — entry point, navigation, theming
  src/main/                  Code shared by every flavor
  src/play/                  Google Play Store flavor (NoOp updater)
  src/apk/                   Direct-APK flavor with the built-in updater
  src/foss/                  FOSS flavor (NoOp updater, no Google services)
  src/playAndApk/            Source shared by play + apk (cast, FCM)
core/
  common/                    Cross-cutting utilities
  model/                     Domain models (Claim, Channel, Comment, ...)
  network/                   Retrofit interfaces, DTOs, mappers
  data/                      Repositories, player controller, upload manager
  datastore/                 Preferences + Room database wrappers
  designsystem/              Reusable Compose components (claim cards, comments, ...)
  database/                  Room database for offline state
feature/
  home/, search/, channel/, wallet/, library/, settings/,
  notifications/, shorts/    Per-feature UI + view models
```

## Building

Prerequisites:
- JDK 17
- Android SDK 36 (compileSdk) / minSdk 31
- `ANDROID_HOME` (or `local.properties` pointing at the SDK)

Debug builds for each flavor:

```bash
./gradlew :app:assemblePlayDebug
./gradlew :app:assembleApkDebug
./gradlew :app:assembleFossDebug
```

Outputs land in `app/build/outputs/apk/<flavor>/debug/`.

### Release builds

Create `keystore.properties` in the project root (it's gitignored):

```properties
storeFile=path/to/odysee.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then:

```bash
./gradlew :app:assemblePlayRelease
./gradlew :app:assembleApkRelease
./gradlew :app:assembleFossRelease
```

## Versioning

`versionName` (e.g. `0.1.0`) is the user-facing version. `versionCode` is an integer used by Android for upgrade ordering and is bumped monotonically per release. It continues the Cordova app's numbering so existing Cordova installs see new releases as updates and migrate.

### APK flavor updater

The `apk` flavor checks `https://apk.odysee.tv/release_native.json` on launch and from **Settings → About → Check for updates**. The manifest is versionName-based:

```json
{ "versionName": "0.1.0" }
```

The updater semver-compares against `BuildConfig.VERSION_NAME` and downloads the next release from `https://apk.odysee.tv/apk/odysee-<versionName>.apk`. The Cordova `release.json` (integer `latest`) is left in place for the one-time migration and is not consulted by the native updater.

## License

[MIT](LICENSE) (same as the previous Cordova-based app).
