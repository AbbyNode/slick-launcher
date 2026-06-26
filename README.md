# Slick Launcher

A minimal, modern Android home-screen launcher written in Kotlin.

## Features

- **Alphabetical app list** — every installed app sorted A → Z (non-letter apps grouped under `#` at the end)
- **Alphabet scrollbar** — slim A–Z strip on the right edge; only letters that have at least one app are shown
  - Tap or drag to jump instantly to any section
  - Active section letter is highlighted in accent colour as you scroll the list
  - Large pill indicator appears on the scrollbar while dragging
- **Section headers** — compact letter dividers in accent colour between each group
- **Slick animations**
  - List fades + slides up on launch
  - Scrollbar fades in with a short delay
  - App rows play a subtle scale-down/up on tap before launching
- **Edge-to-edge** — draws behind status bar and navigation bar
- **Dark "space" theme** — deep dark background (`#0F0F14`) with Material You purple accent (`#BB86FC`)
- **Package change awareness** — list refreshes automatically when apps are installed or removed

## Requirements

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog or newer |
| Android Gradle Plugin | 8.2.2 |
| Gradle | 8.2.2 |
| Kotlin | 1.9.22 |
| `compileSdk` / `targetSdk` | 34 |
| `minSdk` | 26 (Android 8.0) |

## Building

```bash
# 1. Clone the repo
git clone https://github.com/AbbyNode/slick-launcher.git
cd slick-launcher

# 2. Generate the Gradle wrapper JAR (requires Gradle installed locally)
gradle wrapper --gradle-version 8.2.2

# 3. Build a debug APK
./gradlew assembleDebug

# 4. Install on a connected device / emulator
./gradlew installDebug
```

> **Note:** `gradle/wrapper/gradle-wrapper.jar` is a binary and is not committed to the
> repository. Run `gradle wrapper` once (step 2 above) to generate it, or open the project
> in Android Studio which will handle this automatically.

## Project structure

```
app/src/main/
├── java/com/slick/launcher/
│   ├── AppInfo.kt               — data class for an installed app
│   ├── AppListItem.kt           — sealed class: Header | App
│   ├── AlphabetScrollbarView.kt — custom View for the A–Z rail
│   ├── AppListAdapter.kt        — RecyclerView adapter (two view types)
│   └── MainActivity.kt          — launcher Activity
└── res/
    ├── layout/
    │   ├── activity_main.xml      — horizontal split: list + scrollbar
    │   ├── item_app.xml           — icon + name row
    │   └── item_section_header.xml— letter divider
    └── values/
        ├── colors.xml
        ├── strings.xml
        └── themes.xml
```

## Permissions

`QUERY_ALL_PACKAGES` — required to enumerate every installed app on Android 11+.