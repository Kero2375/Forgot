# Forgot

A simple local Android app for managing daily to-dos and scheduled reminders.

## Features

- **Day view**: Week and month calendar navigation with swipeable daily pages, a per-day to-do list, a chronological reminder timeline, and a rollover prompt for unfinished tasks from earlier days.
- **Reminders and categories**: Supports one-shot, daily (custom interval), weekly, and monthly schedules with multiple trigger times per reminder. Reminders are grouped into color-coded categories that can be toggled on/off or scheduled for specific date ranges.
- **Archive**: Archive tasks to keep the daily list clean, with options to restore to today/tomorrow or delete permanently.
- **Notifications**: Local reminder notifications scheduled with `AlarmManager`, including a quick "Mark done" action and automatic rescheduling after device reboot.
- **Home screen widgets**: Two widgets (To-Do List and Reminders) supporting tap-to-complete directly from the home screen and live sync with the app database.
- **Material 3**: Light and dark themes with Material You dynamic color on Android 12+.

## Tech Stack

- Kotlin
- Jetpack Compose & Material 3
- Room (with KSP)
- Kotlin Coroutines & Flow
- Android AppWidgets (`RemoteViews` / `RemoteViewsService`)

## Requirements

- Android 9.0+ (`minSdk 28`, `compileSdk 37`, `targetSdk 37`)
- Android Studio with JDK 11+

## Building and Testing

```bash
# Build debug APK
./gradlew :app:assembleDebug

# Build release APK
./gradlew :app:assembleRelease

# Run unit tests
./gradlew :app:testDebugUnitTest
```
