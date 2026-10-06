# Non-Goals: TestReason

To maintain scope clarity and avoid bloat, the following are explicit **Non-Goals** for TestReason:

1. **Cross-Platform / Multi-Platform Conversion**:
   - We will NOT support iOS, KMP, Flutter, or Web in this codebase. TestReason is strictly a Native Android application.

2. **Superficial / Distracting Gamification**:
   - We will NOT implement avatars, virtual currencies, streak repair items, or public leaderboards.
   - The focus is solely on actual concept mastery and exam passing readiness.

3. **Social / Networking Features**:
   - We will NOT build direct messaging, friend lists, or social feed features.

4. **General Exam / Generic Knowledge Coverage**:
   - TestReason will NOT cover non-JSTQB exams (e.g., IT Passport, FE, AP) within this product. It is dedicated exclusively to JSTQB certification.

5. **Heavy Server-Side Online Dependency**:
   - TestReason is strictly **Offline-First**. Core quiz execution, mastery computation, and review scheduling must function 100% without network connection.
