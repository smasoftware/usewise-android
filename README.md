# Usewise Android SDK

Official Android SDK for [Usewise](https://usewise.io) product analytics.

## Installation

Add JitPack repository and dependency:

```gradle
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts (app)
dependencies {
    implementation("com.github.smasoftware:usewise-android:2.1.1")
}
```

## Quick Start

```kotlin
import io.usewise.android.Usewise
import io.usewise.android.UsewiseConfig

// In Application.onCreate()
Usewise.initialize(this, UsewiseConfig(
    apiKey = "uw_live_your_api_key_here",
    baseUrl = "https://api.usewise.io/api/v1",
))
Usewise.enableCrashReporting()
Usewise.track("app_opened")

// After login
Usewise.identify("user@example.com", traits = mapOf("name" to "Jane"))

// Process tracking
val processId = Usewise.startProcess("checkout") ?: return
Usewise.processStep(processId, "cart_review")
Usewise.processStep(processId, "payment")
Usewise.completeProcess(processId)

// On failure
Usewise.failProcess(processId, reason = "card_declined")

// Error tracking
Usewise.trackError("Payment failed", type = "payment_error", screen = "checkout", processId = processId)

// Logout
Usewise.reset()
```

## Features

- Event tracking with auto-batching
- User identification
- Process/funnel tracking with sub-processes
- Error tracking linked to process steps
- Auto crash reporting
- Device context (model, OS version, VPN, root detection)
- Screen size capture
- Retry with exponential backoff
- Opt-out/opt-in support

## Platforms

- Android API 24+ (Android 7.0+)
