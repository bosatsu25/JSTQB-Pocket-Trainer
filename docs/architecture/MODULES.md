# Module Structure: TestReason

TestReason follows a modular architecture organized by layer and feature boundaries:

```text
JSTQB-Pocket-Trainer/
├── app/                  # Application entry point, Hilt Component, MainActivity, Navigation3 host
│
├── core/
│   ├── model/            # Pure Kotlin domain data classes and enums
│   ├── database/         # Room DB, Entities, DAOs, TypeConverters
│   ├── datastore/        # DataStore preferences & user settings
│   ├── data/             # Repository interfaces and implementations
│   ├── domain/           # Business logic UseCases (Mastery Engine, Review Scheduler)
│   └── ui/               # Common Compose Design System, Material 3 theme, Shared UI components
│
├── feature/
│   ├── home/             # Home dashboard screen
│   ├── quiz/             # Active quiz session screen
│   ├── explanation/      # Answer explanation & mistake tagging screen
│   ├── result/           # Session result screen
│   ├── review/           # Weakness review screen
│   └── analytics/        # Mastery & LO breakdown screen
│
└── benchmark/            # Macrobenchmark & Baseline Profile module
