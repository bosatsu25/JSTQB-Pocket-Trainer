# ADR-0003: Adoption of Jetpack Navigation 3

## Context
Navigation in Jetpack Compose has evolved from string-route navigation to type-safe Navigation 3 (`androidx.navigation3`), providing type-safe state-driven backstacks and scene support.

## Decision
We adopt **Jetpack Navigation 3** as the primary navigation solution.

## Status
Accepted

## Consequences
### Positive
- Type-safe navigation objects without string route parsing bugs.
- Built-in support for multi-pane adaptive scenes and custom backstack transitions.
- First-class support for single-activity architecture.

### Negative
- Relatively new API requiring explicit alignment with official Jetpack Navigation 3 documentation.
