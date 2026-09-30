# AI Phone Companion

A consent-based Android companion application that empowers users to monitor and manage **their own Android device**, or another device whose owner has explicitly granted permission.

Designed with a strict **Privacy-First & Zero-Surveillance Architecture**, AI Phone Companion never secretly captures or broadcasts data. Sensitive capabilities (Notification Access, App Usage Stats, MediaProjection Screen Sharing) require explicit Android system-level authorization, and all active streaming sessions present persistent, user-visible system notifications.

---

## 1. Key Features

### Dual Device Roles
- **Device A (Monitored Phone)**: Collects permitted information (notification history, app usage statistics, battery status, network state) and hosts an encrypted local socket server for live consent-based screen sharing.
- **Device B (Viewer / Controller Phone)**: Connects to Device A over the local network via cryptographic pairing to inspect activity feeds, notification logs, app usage charts, AI productivity insights, and live screen streams.

### Comprehensive Functional Modules
1. **Consent-Gated Onboarding**: Clear role selection ("This is my monitored phone" vs "This is my viewer phone") with privacy notices.
2. **Notification History**:
   - Built on Android's `NotificationListenerService`.
   - Stores app name, package name, title, body text, timestamp, and category in a local Room database.
   - Search by keyword, filter by application, sort by newest/oldest, delete individual records, or clear history.
   - Pause/resume collection switch.
3. **App Usage Tracker**:
   - Queries Android's `UsageStatsManager` (gated behind explicit Usage Access permission).
   - Real-time total screen time, app breakdown, launch counts, first and last active timestamps.
   - Daily, weekly, and monthly views with customizable date selection.
4. **Consent-Based Screen Sharing**:
   - Powered by Android's `MediaProjection` API and a foreground service (`foregroundServiceType="mediaProjection"`).
   - Explains what will be captured before launching Android's system consent dialog.
   - Persistent foreground notification with a one-tap **"Stop Sharing"** button.
   - Configurable stream quality (Low, Medium, High) with live FPS calculation.
   - Interactive Viewer on Device B featuring pinch-to-zoom, pan, fit-to-screen, and fullscreen mode.
5. **Secure Device Pairing**:
   - 6-digit numeric pairing code (e.g. `482 719`) generated with `SecureRandom`.
   - Local Wi-Fi socket communication with SHA-256 handshake verification and session token authentication.
   - Nearby companion auto-discovery on local subnet.
6. **AI Usage & Productivity Insights**:
   - Local analytics engine identifying most-used apps, daily screen time shifts, and notification peak hours (e.g. "Activity peaked between 7 PM and 9 PM").
   - Actionable digital wellness suggestions.
   - One-tap toggle to disable AI analysis and wipe cached insights.
7. **Global Unified Search**: Instant searching across application titles, notification messages, and usage records.
8. **Visual Analytics & Canvas Charts**: Custom Jetpack Compose Canvas visualizations for weekly screen time trends, application time distribution, and hourly notification density.
9. **Device Diagnostics**: Non-sensitive hardware specs including battery level, charging status, Wi-Fi SSID, IP address, OS version, available storage, and system uptime.
10. **Security & App Lock**:
    - Optional in-app PIN and biometric authentication protecting sensitive screens.
    - Salted SHA-256 PIN storage.
11. **Permissions Center**: Direct status badges (ON/OFF) and deep-links to Android Settings for Notification Access and Usage Access.

---

## 2. Technical Stack & Architecture

- **Language**: Kotlin 2.x
- **UI Framework**: Jetpack Compose with Material Design 3 (M3)
- **Architecture**: Clean MVVM (Model-View-ViewModel) + Repository Pattern
- **Local Persistence**: Room Database (SQLite) with Coroutines and Flow
- **Preferences & Keystore**: Encrypted local preferences and `SecureRandom` token generation
- **Networking**: Asynchronous TCP Sockets & OkHttp WebSocket support
- **System Services**:
  - `NotificationListenerService`: for status bar notification capture
  - `UsageStatsManager`: for screen time metrics
  - `MediaProjectionManager`: for screen capture
  - `BatteryManager` & `ConnectivityManager`: for hardware state telemetry
- **Min SDK**: Android 8.0 (API 24)
- **Target SDK**: Android 15 / 16 (API 36)

---

## 3. Project Directory Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/example/
│   ├── MainActivity.kt
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt
│   │   │   ├── SecurityStorage.kt
│   │   │   ├── dao/
│   │   │   │   ├── DeviceDao.kt
│   │   │   │   ├── NotificationDao.kt
│   │   │   │   └── UsageDao.kt
│   │   │   └── entity/
│   │   │       ├── DeviceEntity.kt
│   │   │       ├── NotificationEntity.kt
│   │   │       └── UsageEntity.kt
│   │   ├── remote/
│   │   │   ├── CompanionConnectionManager.kt
│   │   │   └── model/CompanionPayload.kt
│   │   └── repository/
│   │       ├── AiInsightsRepository.kt
│   │       ├── DeviceRepository.kt
│   │       ├── NotificationRepository.kt
│   │       └── UsageStatsRepository.kt
│   ├── domain/model/
│   │   └── DomainModels.kt
│   ├── service/
│   │   ├── CompanionConnectionService.kt
│   │   ├── CompanionNotificationListenerService.kt
│   │   └── ScreenCaptureService.kt
│   └── ui/
│       ├── ai/AiInsightsScreen.kt
│       ├── analytics/AnalyticsScreen.kt
│       ├── dashboard/
│       │   ├── MonitoredDashboardScreen.kt
│       │   └── ViewerDashboardScreen.kt
│       ├── device/DeviceInfoScreen.kt
│       ├── navigation/AppScreen.kt
│       ├── notifications/NotificationHistoryScreen.kt
│       ├── onboarding/OnboardingScreen.kt
│       ├── pairing/PairingScreen.kt
│       ├── screen/ScreenShareScreen.kt
│       ├── search/GlobalSearchScreen.kt
│       ├── security/AppLockOverlay.kt
│       ├── settings/SettingsScreen.kt
│       ├── theme/
│       │   ├── Color.kt
│       │   ├── Theme.kt
│       │   └── Type.kt
│       ├── usage/AppUsageScreen.kt
│       └── viewmodel/
│           ├── AiInsightsViewModel.kt
│           ├── MainViewModel.kt
│           ├── NotificationViewModel.kt
│           └── UsageViewModel.kt
└── res/
    ├── drawable/
    │   ├── ic_launcher_background.xml
    │   └── ic_launcher_foreground.xml
    └── values/
        └── strings.xml
```

---

## 4. Setup & Running in Android Studio

1. Open Android Studio (Hedgehog, Iguana, Jellyfish, or newer).
2. Select **File > Open** and point to the project root directory.
3. Allow Gradle to sync dependencies automatically.
4. Select an Android emulator or physical device running Android 8.0+ (API 24+).
5. Click **Run > Run 'app'** (`Shift + F10`).

---

## 5. How to Pair Two Devices

1. Connect both devices to the same Wi-Fi network.
2. Launch **AI Phone Companion** on **Device A**:
   - Tap **"This is my monitored phone"** during onboarding.
   - Go to **Pairing** or tap **"Pair Code"** on the dashboard.
   - Note the **6-digit Pairing Code** (e.g. `482 719`) and the **Local IP Address** shown.
3. Launch **AI Phone Companion** on **Device B**:
   - Tap **"This is my viewer phone"** during onboarding.
   - Enter the 6-digit code and Device A's IP address.
   - Tap **"Pair Device"**.
4. Both devices will perform an authenticated handshake. Once connected:
   - Device B receives real-time battery status, Wi-Fi details, and usage metrics.
   - Device A displays a green "Connected" indicator.

---

## 6. How Permissions & Sensitive Features Work

### Notification History
- **Permission**: `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`
- Android strictly isolates notification access. The user must manually enable the app under:
  `Settings > Apps > Special app access > Notification access > AI Phone Companion`.
- The in-app **Permissions Center** provides a direct shortcut button.

### App Usage Tracker
- **Permission**: `android.permission.PACKAGE_USAGE_STATS`
- Requires explicit user authorization in:
  `Settings > Apps > Special app access > Usage access > AI Phone Companion`.

### Live Screen Sharing
- **API**: Android `MediaProjection` API + `FOREGROUND_SERVICE_MEDIA_PROJECTION`.
- Upon tapping **"Start Screen Sharing"**, a consent dialog details what will happen, followed by Android's system projection permission prompt.
- While active, a persistent high-priority notification with a **"Stop Sharing"** action is pinned to the notification tray.

---

## 7. Privacy & Anti-Surveillance Policy

This application is strictly intended for personal device management, accessibility assistance, or authorized multi-device workflows with full mutual consent.
- **No Stealth Mode**: There is no hidden background recording, keystroke logging, microphone eavesdropping, or permission bypass mechanism.
- **Revocability**: The owner of Device A can revoke permissions, disconnect the viewer, or stop screen capture with a single tap at any time.
- **Local-First**: Sensitive records remain on-device in an encrypted Room SQLite database and are transmitted strictly over authenticated peer-to-peer network sessions.
