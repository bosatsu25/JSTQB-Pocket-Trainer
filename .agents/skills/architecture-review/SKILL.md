---
name: architecture-review
description: Audits code against pragmatic Clean Architecture, UDF rules, module boundaries, and YAGNI invariants.
---

# Architecture Review Skill

Use this skill when adding or refactoring components, modules, or state flows.

## Architecture Audit Checklist

1. **Unidirectional Data Flow (UDF)**:
   - Are ViewModels exposing immutable `StateFlow`?
   - Are UI events flowing up via explicit lambda calls or events?

2. **Single Source of Truth (SSOT)**:
   - Is Room the database SSOT for persistent domain entities?
   - Is DataStore used for configuration and preferences?

3. **YAGNI & Simplicity**:
   - Are there redundant Repositories, UseCases, or Mappers that add no value? Eliminate unnecessary layers.

4. **Module Boundary**:
   - Are core modules free from dependencies on feature modules?
