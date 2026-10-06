# ADR-0005: Repository-Driven Agent Harness Architecture with Continuous Execution

## Context
AI Agents (such as Gemini in Android Studio Agent Mode) can drift, hallucinate progress, or produce monolithic unverified code changes if relying solely on chat history or loose prompt engineering.

## Decision
We implement a **Repository-Driven Agent Harness**:
- `AGENTS.md` acts as the repository constitution.
- `docs/` stores specifications, architecture decisions, and roadmap execution state.
- `.agents/skills/` contains specialized execution workflows (`execute-wave`, `verify-change`, `ux-review`, `architecture-review`).
- All work is organized into Waves (`Phase -> Wave -> PR`), where `1 Wave = 1 Cohesive PR`.
- **Continuous Execution Update**: Once a Wave passes all Quality Gates, recorded evidence is logged in `CURRENT_STATE.md`, and execution automatically advances to the next eligible Wave without blocking for user approval on every step, while retaining strict verification gates.

## Status
Accepted (Supersedes original single-wave stop requirement)

## Consequences
### Positive
- High predictability, deterministic execution loops, and mechanical verification gates.
- Streamlined multi-wave implementation flow from M0 through M4.
- Context remains clean and structured across iterative development.

### Negative
- Requires maintaining documentation state in sync with code changes at each Wave boundary.
