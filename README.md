# 🤖 RS Assistant v100

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.java.com/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![API 24+](https://img.shields.io/badge/API-24%2B-green?style=for-the-badge)](https://developer.android.com/about/dashboards)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](LICENSE)

> 🔥 The ultimate AI-powered voice assistant for Android — control your entire phone with voice commands in Hindi, English, and Hinglish. Built by [T3rmuxk1ng](https://youtube.com/@T3rmuxk1ng).

---

## ✨ Features

- 🎙️ **Voice Recognition** — Hindi / English / Hinglish support
- 🔊 **Wake Word Detection** — Hands-free activation
- 📱 **Phone Control** — Accessibility Service-based full phone control
- 📷 **Camera Control** — Take photos and manage camera
- ✋ **Hand Gesture Detection** — Shake to toggle torch
- 🔐 **OAuth Authentication** — Login via Z.AI platform
- 🧠 **AI Chat Integration** — Powered by Z.AI Chat Engine
- 💾 **Memory Manager** — Remembers commands, shortcuts & usage patterns
- 🆘 **SOS Emergency** — Shake or voice-triggered emergency alerts
- 🔋 **Background Voice Service** — Always-listening mode
- 🔒 **Device Admin** — Lock screen, power off remotely
- ⚡ **Smart Suggestions** — Predictive command suggestions
- 📊 **Feature Usage Tracking** — Analytics dashboard

---

## 🎙️ Voice Commands

| Command | Action |
|---------|--------|
| `"Call [contact]"` | Make phone calls |
| `"Message [contact]"` | Send SMS messages |
| `"Open [app]"` | Launch any application |
| `"Flashlight on/off"` | Control torch |
| `"WiFi settings"` | Open WiFi settings |
| `"What time is it"` | Get current time |
| `"Scroll up/down"` | Navigation gestures |
| `"Torch on/off"` | Toggle flashlight |
| `"SOS" / "Emergency"` | Trigger emergency alert |
| `"Shake on/off"` | Enable/disable shake detection |
| `"Show features"` | Open features screen |
| `"टॉर्च जला"` | Torch ON (Hindi) |
| `"टॉर्च बुझा"` | Torch OFF (Hindi) |
| `"मदद"` | Emergency SOS (Hindi) |

---

## 🏗️ Architecture

```
rs-assistant-v100/
├── app/src/main/java/com/rsassistant/
│   ├── MainActivity.java          # Main activity with voice UI
│   ├── FeaturesScreenActivity.java # Features dashboard
│   ├── ai/
│   │   ├── SmartAssistantManager.java  # Smart predictions & suggestions
│   │   └── ZAIChatManager.java         # AI chat engine integration
│   ├── auth/
│   │   └── OAuthManager.java          # Z.AI OAuth authentication
│   ├── gesture/
│   │   └── ShakeDetector.java         # Shake gesture for torch
│   ├── memory/
│   │   └── MemoryManager.java         # Command memory & shortcuts
│   ├── service/
│   │   ├── RSAccessibilityService.java # Phone control service
│   │   ├── VoiceRecognitionService.java # Background voice service
│   │   └── CameraService.java         # Camera management
│   ├── voice/
│   │   └── TTSEngine.java             # Text-to-Speech engine
│   ├── worker/
│   │   └── UpdateReminderWorker.java  # Periodic update reminders
│   ├── receiver/
│   │   └── BootReceiver.java          # Auto-start on boot
│   └── util/
│       ├── CommandProcessor.java       # Voice command parser
│       ├── DeviceControlManager.java   # Hardware control
│       ├── SystemLevelManager.java     # System-level operations
│       ├── PermissionHelper.java       # Permission management
│       ├── PreferenceManager.java      # Shared preferences
│       └── RSDeviceAdminReceiver.java  # Device admin receiver
├── app/build.gradle
├── build.gradle
└── settings.gradle
```

---

## 📋 Requirements

- **Android 7.0** (API 24) or higher
- **Permissions**: Microphone, Camera, Phone, Contacts, SMS, Overlay, Device Admin
- **Services**: Accessibility Service, Notification Listener

---

## 🚀 Installation

1. Download the latest APK from the [Releases](https://github.com/rajsaraswati-jatavv/rs-assistant-v100/releases) section
2. Enable "Install from Unknown Sources" in your Android settings
3. Install the APK
4. Grant all requested permissions on first launch
5. Enable Accessibility Service for full phone control
6. Enable Device Admin for lock screen and power features

### Build from Source

```bash
# Clone the repository
git clone https://github.com/rajsaraswati-jatavv/rs-assistant-v100.git
cd rs-assistant-v100

# Build with Gradle
./gradlew assembleDebug

# APK will be at app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔧 Configuration

### Z.AI OAuth Setup
The app integrates with Z.AI for AI chat capabilities. Configure your OAuth credentials in the app settings.

### Shake Detection
Enable shake detection via voice command `"Shake on"` or in settings. Shake your phone to toggle the flashlight.

---

## 📺 YouTube

> Learn how to build AI-powered Android assistants! Watch tutorials on **[T3rmuxk1ng YouTube Channel](https://youtube.com/@T3rmuxk1ng)**

---

## 👤 Author

**Rajsaraswati Jatav (T3rmuxk1ng)**

- YouTube: [https://youtube.com/@T3rmuxk1ng](https://youtube.com/@T3rmuxk1ng)
- GitHub: [rajsaraswati-jatavv](https://github.com/rajsaraswati-jatavv)

---

## 📄 License

This project is licensed under the MIT License.

---

<div align="center">

**If you found this project useful, give it a star!**

[YouTube](https://youtube.com/@T3rmuxk1ng) | [GitHub](https://github.com/rajsaraswati-jatavv)

</div>
