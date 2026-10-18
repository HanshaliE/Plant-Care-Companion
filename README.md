# Plant Care Companion

Plant Care Companion is an Android app for tracking and caring for houseplants. Users can add plants (by searching a live species database or entering details manually), log care activities such as watering, view a per-plant care history, set reminders for upcoming care tasks and manage a simple profile and app settings. The app is built to work fully offline with live species search as an optional online enhancement.

## Team Members

Group 08

- Sandinma Perera - 22672614
- Yessica Bandara - 22396891
- Hanshali Edirisinghe - 22017859

The work was divided between the three of us and each member chose the area they wanted to take on:

- **Sandinma Perera** - Project foundation, plants list, plant details and care log
   - Project foundation: Room database setup (AppDatabase, Plant and CareLogEntry entities, PlantDao and CareLogDao) and the Material theme (colors, typography)
   - Plants list: saved plants with search and sorting and navigation to details and the care log
   - Plant details screen: care schedule with actions to edit, delete, log care and view history
   - Edit Plant screen: form state preserved across rotation and reset when canceled
   - Plant deletion with a confirmation step
   - Care logging: a separate "Log Care Activity" dialog kept apart from the care history screen with its state moved into the ViewModel (PlantCareViewModel)
   - Shared back-arrow top bar component (BackTopBar)

- **Yessica Bandara** - Add-plant flow, species lookup, networking, photos and tests
   - Add-plant flow: AddPlantViewModel, UI state, species search screen add-plant screen and navigation
   - Perenual API integration: Retrofit API, network module and response models
   - SpeciesRepository with retry and offline fallback, SpeciesMapper and the built-in local plant catalog and lookup error handling
   - Rate-limit guard and connectivity checker for the free API plan
   - Plant photo storage, and the dependency container (AppContainer)
   - Unit tests: species repository, rate limiter, species mapper and plant-draft validation
   - Final UI adjustments for a consistent look and the project's code comments

- **Hanshali Edirisinghe** - Reminders, Settings, Profile, navigation and app polish
   - Bottom navigation bar with Plants, Reminders, Profile and Settings tabs
   - Reminders screen: due-today plants, mark-as-watered writing to the care log, custom reminders (Room table and database migration) and the Watering reminders switch plus reminder scheduling with WorkManager
   - Settings: notifications preference (DataStore), data export to JSON, privacy information and clear-data options
   - Profile: editable name, bio and photo with stats plus camera capture (system camera through a FileProvider) alongside the gallery picker
   - Splash screen and adaptive app icon matching the app theme
   - Back arrows applied to the sub-screens and the reminders state-persistence fixes

## App description

1. **Users** - people who keep several houseplants and currently rely on memory, phone notes or calendar alerts to remember watering.
2. **Core task** - record a plant with its watering schedule, get a reminder when it is due and log each care action.
3. **Data** - a Plant has many care log entries (Room, linked by plantId). Species care information comes from the Perenual API and is cached locally in Room. Settings and profile values are in DataStore.
4. **Scope** - works fully offline; user accounts, login and cloud sync are out of scope.

## Demonstrating network and device features

**Network (live species search)**
1. With a key in `local.properties`: Plants → Add Plant → search "monstera". Results without a "Built-in" tag come from the Perenual API.
2. Offline fallback: turn on airplane mode in the emulator (swipe down, Airplane mode) and search again. The screen shows the reason and returns cached results plus the built-in catalog.
3. No key: remove the key line, rebuild, and search. The app shows "Live plant search isn't set up on this device." and uses the built-in catalog.

**Device features**
1. Notifications: Reminders -> turn the Watering reminders switch on and allow the permission (Android 13+). Deny it once to see the message and that nothing is scheduled.
2. Camera: Profile -> Edit -> take a photo (the emulator camera shows a virtual scene). The same option is on the Add Plant screen.
3. Photo picker: choose a photo from the gallery. Drag an image onto the emulator window first if its gallery is empty.
4. Cancelling the camera or the picker leaves the previous photo unchanged.

## Requirements

- **Android Studio** - Quail 3 | 2026.1.3 (build AI-261.26222.65.2613.15948027) the version used for development
- **JDK** - 21 (set via `gradle/gradle-daemon-jvm.properties`, `toolchainVersion=21`); Gradle downloads it automatically on first sync if it is not installed
- **Gradle** - 9.5.0 (`gradle/wrapper/gradle-wrapper.properties`)
- **Android Gradle Plugin (AGP)** - 9.3.3
- **Kotlin** - 2.2.10
- **KSP** - 2.3.12
- **compileSdk** - 37 (Android Studio installs SDK Platform 37 on first sync if missing)
- **minSdk** - 24
- **targetSdk** - 36

Recommended emulator: **Pixel 9, API 35** (within the app's supported `minSdk`–`targetSdk` range of 24–36)

## How to build and run

### In Android Studio

1. Open the project root (the folder containing `settings.gradle.kts`) in Android Studio.
2. Let Gradle **Sync** finish. The first sync needs an internet connection to download Gradle, the Android Gradle Plugin and all dependencies.
3. Select an emulator or connected device and click **Run**.

### From the command line

```
./gradlew assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

### Unit tests

```
./gradlew testDebugUnitTest
```

The unit tests cover the species repository, the rate limiter, the species mapper and plant-draft validation.

## Perenual API key setup

Live species search needs a free Perenual API key (create one at perenual.com). The app reads a `PERENUAL_API_KEY` property from `local.properties` (see `app/build.gradle.kts`) and bakes it into `BuildConfig.PERENUAL_API_KEY` at build time.

1. Open (or create) `local.properties` in the project root, next to `settings.gradle.kts`.
2. Add a line in this form, with no quotes and no spaces around `=`:

```
      PERENUAL_API_KEY=your_key_here
```

3. Sync the project, then **Build → Rebuild Project** so the new key is picked up.

`local.properties` is deliberately excluded from version control and from the submitted ZIP, because it is per-developer and holds a private key. The key is optional: the app runs without it.

Without a key, live search is disabled and the app shows "Live plant search isn't set up on this device." It then falls back to a built-in catalog of 30 common plants plus previously cached species and the user's saved plants, so the rest of the app (adding plants manually, logging care, reminders, profile, settings) keeps working offline.

The free Perenual plan allows **100 requests per day**. The app enforces this itself with a `RateLimitGuard` (`data/network/RateLimitGuard.kt`). It counts requests for the current day, blocks new ones once the limit is reached or the API returns a 429, and respects a `Retry-After` header when present. When the guard is closed, search and care-info lookups fall back to cached and offline data instead of failing.

## Permissions and device notes

- **Notifications (`POST_NOTIFICATIONS`)** - declared in the manifest and requested at runtime only on Android 13 (API 33) and above, when the user switches reminders on from the Reminders screen. If the permission is denied, the screen shows a message, no reminders are scheduled and the app does not crash. The reminder worker also rechecks the permission before it posts a notification.
- **Camera** - the app does **not** request the `CAMERA` permission. Taking a photo launches the system camera app through `ActivityResultContracts.TakePicture()`, and picking an existing photo uses the system photo picker (`ActivityResultContracts.PickVisualMedia()`). The manifest declares `android.hardware.camera.any` as optional (`required="false"`), so the app also runs on devices without a camera. If the picker is cancelled nothing changes, and if no camera app exists the screen shows a message and the previous photo is kept.
- **FileProvider** - declared in the manifest (authority `${applicationId}.fileprovider`) to share camera-captured photo files with the system camera app.
- **Internet / network state** - `INTERNET` and `ACCESS_NETWORK_STATE` are declared for live species search and connectivity checks.

## Features by screen

- **Plants list** - searchable, sortable (recently added / name A–Z) list of saved plants, with an action to add a plant.
- **Plant details** - shows the care schedule (watering interval, last watered, light requirement) with actions to edit, delete, log care and view care history.
- **Edit Plant** - change nickname, location, watering interval and light requirement.
- **Care history** - chronological list of logged care activities for a plant, with a separate Log Care Activity dialog.
- **Add-plant flow** - search for a species (live API search with offline fallback) or enter one manually; set nickname, location, watering interval and light requirement; attach a photo from the camera or the photo picker.
- **Reminders** - switch watering reminders on or off, mark due plants as watered, and add custom reminders with a title, notes, date and time.
- **Profile** - view and edit a display name, bio and profile photo (camera or photo picker).
- **Settings** - toggle notifications, export all plant and care-log data to JSON, clear saved photos only, or clear all local data.
- **Splash** - system splash screen (`androidx.core:core-splashscreen`) shown on launch.

## Architecture

The app follows **MVVM**:

- **UI layer** (`ui/`) - Jetpack Compose screens organised by feature (`ui/screens`, `ui/addplant`, `ui/theme`), plus screens for Reminders, Profile and Settings.
- **ViewModel layer** - one `ViewModel` per feature area (`PlantCareViewModel`, `AddPlantViewModel`, `ReminderViewModel`, `ProfileViewModel`, `SettingsViewModel`), each created through a factory and backed by Room and/or DataStore.
- **Data layer** (`data/`) - Room entities and DAOs (`data/local`), the Perenual API client on Retrofit/OkHttp with Kotlinx Serialization (`data/remote`), repositories that combine live and local data (`data/repository`, `ProfileRepository`, `SettingsRepository`), a connectivity checker and rate-limit guard (`data/network`), and local photo storage (`data/photo`).
- **Domain layer** (`domain/`) - plain Kotlin models and logic (`Species`, `PlantDraft`, `LookupError`, `LocalPlantCatalog`).
- **Dependency wiring** - `di/AppContainer.kt` builds the repositories, the database and the ViewModel factories.

**Room**: `AppDatabase` is at **version 3**, with entities `Plant`, `CareLogEntry`, `CachedSpecies` and `CustomReminder`. `MIGRATION_1_2` adds the `cached_species` table and `MIGRATION_2_3` adds the `custom_reminders` table, so existing installs keep their data.

**DataStore**: Preferences DataStore holds app settings, such as the notifications-enabled flag in `SettingsRepository`. Reminders, Settings and Profile all read this one flag, so they always agree.

**WorkManager**: schedules and runs reminder notifications (`ReminderScheduler`, `ReminderWorker`). `SettingsRepository.clearLocalData()` also cancels scheduled work when the user clears their data.

**Navigation**: Compose Navigation with type-safe routes. Each destination is a `@Serializable` object or class (for example `PlantList`, `PlantDetails(plantId)`, `CareLog(plantId)`, `EditPlant(plantId)`, `RemindersRoute`, `SettingsRoute`, `ProfileRoute`), wired up in `ui/PlantCareApp.kt`.

**State rules**: screen state lives in ViewModels backed by Room and/or DataStore, so it survives tab switches, rotation and app restarts. This includes the watered state (derived from the care log), the reminders switch and the custom reminders list, and the Log Care Activity dialog state. Purely visual state, such as a dropdown being expanded, uses `remember`/`rememberSaveable` inside the composable.

## Demo walkthrough

1. Launch the app; the splash screen appears briefly before the Plants list.
2. Tap **Add Plant**, search for a species (or choose manual entry), and fill in nickname, location, watering interval and light requirement.
3. Attach a photo from the camera or the photo picker, then save the plant.
4. Open the plant's details and tap **Log Care Activity** to record a watering.
5. Open the care history to confirm the new entry appears.
6. Go to Reminders, switch reminders on (allow the notification permission on Android 13+), tick a plant as watered, and add a custom reminder.
7. Switch to another tab and back to check that the watered tick and the reminders list are still there.
8. Go to Profile and update the name, bio and photo (camera or picker).
9. Go to Settings: export data to JSON, clear photos only then clear all local data to confirm the app resets cleanly.

## Privacy and data handling

- All plant, care-log, species-cache, reminder, profile and settings data is stored **locally only**, in a Room database and DataStore on the device.
- When live species search is used, only the search term and the Perenual API key (as a request parameter) are sent to the Perenual API. No other personal data leaves the device.
- The app has **no user accounts** and no sign-in.
- Clearing data from Settings deletes all locally stored plants, care logs, reminders and settings, and cancels scheduled reminder work.

## Known limitations

- No dedicated dark theme polish.
- Live species search needs a Perenual API key in `local.properties` and the free plan is limited to 100 requests per day. Without it the app uses the built-in catalog and cached species.
- Custom reminders are standalone (title, notes, time) and are not linked to a specific plant.
- WorkManager reminder delivery can be delayed and is not exact to the minute.
- Unit tests cover the species lookup, rate limiter, mapper and plant-draft validation; the Reminders, Profile and Settings screens were tested manually on the emulator.

## Data source

Plant care information comes from the [Perenual plant API](https://perenual.com/docs/api)