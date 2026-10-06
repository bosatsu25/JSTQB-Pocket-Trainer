# ADR-0004: Offline-First Architecture via Room and DataStore

## Context
Users study JSTQB questions while commuting or in areas with spotty network connectivity. The core learning experience must function seamlessly offline.

## Decision
We adopt an **Offline-First Architecture**.
- **Room Database** serves as the Single Source of Truth for domain entities (questions, attempts, sessions, review schedules, mastery).
- **Jetpack DataStore (Preferences)** stores lightweight user settings, goal preferences, and daily session state.

## Status
Accepted

## Consequences
### Positive
- Zero network latency when taking quizzes or reviewing answers.
- Reliable state persistence across app restarts and process deaths.

### Negative
- Database migrations must be managed carefully as entities evolve.
