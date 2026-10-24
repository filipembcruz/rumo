<div align="center">

<img src="docs/icon.png" width="112" alt="Rumo icon" />

# Rumo

**Take back your time.**

A modern digital wellbeing app for Android: screen time per app **and per website**, limits that actually block, focus sessions, goals, and a friendly companion — 100% private and offline.

[English](README.md) · [Português](README.pt-BR.md)

<a href="https://github.com/filipembcruz/rumo/releases/latest/download/rumo.apk">
  <img src="https://img.shields.io/badge/Download-rumo.apk-6B4EFF?style=for-the-badge&logo=android&logoColor=white" alt="Download rumo.apk" />
</a>

<sub>Android 8.0+ · Free and open source · MIT</sub>

<br/><br/>

<img src="docs/hero.png" alt="Rumo screens" width="100%" />

</div>

---

## Why Rumo

Android's built-in Digital Wellbeing tells you that you spent 3 hours in *Brave* — but not **on what**. Rumo shows the websites inside your browser, groups everything by category, compares periods, and turns lost time into something tangible: *"this year, that could have been 1.7 new languages."* Then it helps you change it, with limits, focus sessions, schedules and a mindful pause before opening distracting apps.

## Screenshots

| Home | Insights | Focus | Growth |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/home.png" width="200"/> | <img src="docs/screenshots/insights.png" width="200"/> | <img src="docs/screenshots/focus.png" width="200"/> | <img src="docs/screenshots/growth.png" width="200"/> |

| App details | Dark mode (PT-BR) | Onboarding | Block screen |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/detail.png" width="200"/> | <img src="docs/screenshots/home-dark-pt.png" width="200"/> | <img src="docs/screenshots/reality.png" width="200"/> | <img src="docs/screenshots/block.png" width="200"/> |

<div align="center">
<img src="docs/rumi.png" width="640" alt="Rumi's moods" /><br/>
<sub><b>Meet Rumi</b> — your companion floats, breathes, blinks and changes mood with your day. Tap it for a little celebration.</sub>
</div>

## Features

### 📊 Know where your time goes
- **Screen time per app**, with opens, average session, first pickup of the day and unlocks.
- **Screen time per website** inside Brave, Chrome, Firefox, Edge, Samsung Internet, Opera, DuckDuckGo and more.
- **Categories** for apps and sites (social, video, games, learning, productivity…) — change any of them with a tap.
- **Insights** by day, week, month, year or a custom range: animated bar charts with your daily goal line, donut by category, hour-by-weekday heatmap, averages by hour and weekday, longest day, change vs the previous period.
- **Focus score** (0–100) from your goal, balance and pickups.
- **Battery estimate** per app for the current charge.

### 🛡️ Take control
- **Daily limits** per app, website or whole category — alert at 80% and 100%, or **block** when time is up (with an optional "5 more minutes").
- **Focus sessions** with a countdown ring; distracting apps and sites are blocked. **Strict mode** requires holding to end.
- **Schedules** like *Bedtime* or *Work hours* that block chosen categories automatically.
- **Mindful pause**: a breathing screen before opening the apps you pick, showing how many times you opened them today.
- **Adult sites (+18) blocking**, in every browser.

### 🌱 Grow
- **Goals in hours** (learn a language, read more, a degree, learn to code…) with progress, weekly pace and ETA.
- **Log offline activities** and link focus sessions to goals.
- **Streaks**, best streak and **achievements**.
- **"This time could have been…"** — non-productive screen time converted into languages, books, workouts or nights of sleep.

### ✨ And more
- **Home screen widget** (small, medium, large) with goal ring, top apps, websites and limits.
- **Notifications**: limit alerts, gentle nudges after long distracting streaks, daily summary, weekly report, bedtime reminder.
- **Onboarding** with your goals, a daily target and a reality check of your real usage.
- **English** and **Português (Brasil)**; light, dark or system theme.
- **Backups**: save a JSON file anywhere (including Google Drive), share it, or restore it.

## Install

1. On your Android phone, download **[rumo.apk](https://github.com/filipembcruz/rumo/releases/latest/download/rumo.apk)** and open it.
2. Allow installing from your browser or file manager when asked.
3. Open Rumo and follow the onboarding: allow **Usage access** (required).

Updates install over the previous version and keep your data.

> **Google Play Protect blocked the install?** Because Rumo uses accessibility (to read the browser's address bar and show block screens), Play Protect may block apps installed outside the Play Store. Open **Play Store → profile → Play Protect → ⚙ → turn off "Scan apps with Play Protect"**, install, then turn it back on.

### Turning on websites & blocking

Android protects accessibility for apps installed outside the Play Store ("Restricted setting"). Rumo walks you through it in **Settings → Permissions → Sites & blocking**:

1. **Settings → Accessibility → Installed apps → Rumo** → turn on (Android will refuse the first time — that's expected).
2. **Settings → Apps → Rumo → ⋮ (top right) → Allow restricted settings**.
3. Back to **Accessibility** and turn Rumo on.

Prefer not to? Use **"Display over other apps"** instead: limits, focus, schedules and the mindful pause still block apps (websites aren't tracked in this mode).

## Privacy

- **No account, no servers, no ads, no analytics.** Everything is stored only on your phone.
- The accessibility service reads **only which app is on screen and the browser's address bar**. Rumo never reads what you type, your messages or passwords.
- A quiet notification shows while website tracking is active (you can hide it).
- Backups are plain files that you control.

| Permission | Why |
|---|---|
| Usage access | Time per app, opens, unlocks (required) |
| Accessibility *(optional)* | Websites in browsers, block and pause screens |
| Display over other apps *(optional)* | Blocking without accessibility |
| Notifications *(optional)* | Alerts, summaries and reminders |

## Build from source

Requirements: **JDK 17+**, **Android SDK 35**, **Gradle 8.11+**.

```bash
git clone https://github.com/filipembcruz/rumo.git
cd rumo
gradle assembleDebug            # app/build/outputs/apk/debug/app-debug.apk
gradle :app:recordRoborazziDebug  # renders the screenshots above into app/build/screens
```

Every push to `main` builds a signed release APK with **GitHub Actions** and publishes it under [Releases](https://github.com/filipembcruz/rumo/releases/latest). Signing uses the `RUMO_KEYSTORE_BASE64` and `RUMO_KEYSTORE_PASSWORD` secrets — forks should add their own.

### Tech stack

- **Kotlin** + **Jetpack Compose** (Material 3), custom design system with Poppins and Inter
- Hand-made Compose **Canvas charts** (bars, area, donut, heatmap, rings) with animations
- **UsageStatsManager** for app usage, an **AccessibilityService** for websites and blocking, a foreground service fallback with overlay
- **Glance** for the home screen widget, **WorkManager** for summaries and alerts
- **SQLite** (no ORM), JSON backups through the Storage Access Framework
- **Roborazzi + Robolectric** screenshot tests in CI

### Project structure

```
app/src/main/java/io/github/filipembcruz/rumo/
├── core/      # preferences, permissions, actions
├── data/      # database, usage collector, categories, stats, backup, battery
├── service/   # accessibility service, blocking fallback, rules, notifications
├── ui/        # theme, components (charts, Rumi), screens, strings (EN/PT)
├── widget/    # Glance home screen widget
└── work/      # periodic worker, focus timer
```

## License

[MIT](LICENSE) © Filipe Cruz
