# Contributing to STEREO

Thank you for your interest in contributing to **STEREO**! STEREO is an independent, open-source native Android music player crafted with Jetpack Compose, Material 3, and Kotlin.

We welcome contributions from developers, designers, and music enthusiasts. Whether you are fixing a bug, translating UI strings, improving recommendation heuristics, or optimizing audio performance, your help is appreciated.

---

## Code of Conduct

All contributors and community members are expected to adhere to our [Code of Conduct](CODE_OF_CONDUCT.md). Please keep discussions respectful, constructive, and inclusive.

---

## Getting Started

### Prerequisites

- **Android Studio:** Ladybug (2024.2.1) or newer.
- **JDK:** OpenJDK 17 or higher.
- **Android SDK:**
  - `compileSdk = 34`
  - `minSdk = 26` (Android 8.0 Oreo)
  - `targetSdk = 34` (Android 14)

### Fork and Clone

1. Fork the repository on GitHub: `https://github.com/rishibandaru3109/STEREO`.
2. Clone your fork locally:
   ```bash
   git clone https://github.com/<your-username>/STEREO.git
   cd STEREO
   ```
3. Open the project in Android Studio and let Gradle sync.

---

## Development Workflow

### Branching Strategy

- `master`: Stable, release-ready branch.
- Feature/Bugfix branches: Create descriptive branches off `master` (e.g., `fix/stream-timeout`, `feat/lyrics-synced`).

### Building and Testing

Run unit tests locally before submitting any changes:

```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Assemble debug build
./gradlew assembleDebug

# Check code formatting and lint
./gradlew lintDebug
```

> [!IMPORTANT]
> All unit tests (currently 98 tests) must pass with a 100% success rate. PRs with broken tests will not be merged.

---

## Architectural Guidelines

STEREO follows clean architecture principles and Android Jetpack recommendations:

- **UI Layer:** 100% Jetpack Compose using Material 3 design tokens. Avoid legacy Android Views or XML layouts for UI.
- **State Management:** MVVM pattern with Kotlin Coroutines `StateFlow` and immutable UI state models.
- **Dependency Injection:** Koin modules defined in `AppModule.kt`.
- **Database & Persistence:** AndroidX Room with Room DAOs and Jetpack DataStore Preferences for settings and themes.
- **Media Playback:** AndroidX Media3 (`MediaSessionService`, `ExoPlayer`, `ForwardingPlayer`). Never directly manipulate low-level audio sockets without going through the Media3 pipeline.
- **Zero Paid Cloud Services:** STEREO operates strictly under a ₹0 budget constraint. Do not introduce dependencies requiring paid API keys, developer subscriptions, or cloud analytics backends.

---

## Submitting Pull Requests

1. Commit your changes with clear, semantic commit messages (e.g., `fix(player): resolve seekbar jitter during live stream buffer`).
2. Push your branch to your GitHub fork:
   ```bash
   git push origin feat/your-feature-name
   ```
3. Open a Pull Request against `rishibandaru3109/STEREO:master`.
4. Fill out the provided [Pull Request Template](.github/pull_request_template.md).
5. Ensure CI builds and tests pass.

---

## Reporting Issues

If you discover a bug or have a suggestion:

- Check existing [GitHub Issues](https://github.com/rishibandaru3109/STEREO/issues) before opening a new one.
- Use our structured issue forms:
  - [Bug Report Form](.github/ISSUE_TEMPLATE/bug_report.yml)
  - [Feature Request Form](.github/ISSUE_TEMPLATE/feature_request.yml)
- Include device model, Android OS version, and reproducible steps.

---

## Licensing Note

By contributing to STEREO, you agree that your contributions will be licensed under the [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
