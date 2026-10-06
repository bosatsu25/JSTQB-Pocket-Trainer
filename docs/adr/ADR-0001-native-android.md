# ADR-0001: Adoption of Native Android with Kotlin

## Context
TestReason is a mobile learning app designed specifically for Android users preparing for the JSTQB Foundation Level exam in Japan. We needed to choose between cross-platform frameworks (Flutter, React Native, Kotlin Multiplatform) and pure Native Android development.

## Decision
We adopt **Native Android with Kotlin** as the sole development stack for TestReason.

## Status
Accepted

## Consequences
### Positive
- Deep integration with modern Android APIs (Material 3, Adaptive Layouts, Navigation 3, WorkManager, Room, Baseline Profiles).
- Maximum performance, fast startup times, and optimal battery efficiency.
- Seamless compatibility with standard Android Studio tooling, AGP, and Android Knowledge Base.

### Negative
- Application is limited to the Android platform (aligned with non-goals).
