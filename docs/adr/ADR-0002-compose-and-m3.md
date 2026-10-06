# ADR-0002: Adoption of Jetpack Compose & Material 3

## Context
Legacy Android View systems rely on XML layouts, synthetic bindings, and complex state management across View and Fragment lifecycles. Modern Android UI development uses declarative UI frameworks.

## Decision
We adopt **Jetpack Compose** and **Material Design 3 (M3)** for all user interfaces across TestReason. No XML Views will be introduced.

## Status
Accepted

## Consequences
### Positive
- Fully declarative, reactive UI bound directly to ViewModel StateFlows via Unidirectional Data Flow (UDF).
- Built-in dynamic color, light/dark mode support, and accessibility primitives.
- Reduced boilerplate compared to XML View layouts.

### Negative
- Requires careful handling of recomposition stability (`@Stable`, `@Immutable`, `derivedStateOf`).
