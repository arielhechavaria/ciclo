# Ciclo

Offline Android app for one oncologist to track how far each patient is into their chemotherapy treatment.

- Product proposal: `docs/propuesta-ciclo.html`
- Phase 1 plan and specs: `odd/tasks/ciclo-mvp.md`

## Modules

- `domain`: pure Kotlin models and schedule engine, tested with JUnit 5.
- `app`: Android app (Jetpack Compose).

## Build

There is no local Android SDK requirement. GitHub Actions (`.github/workflows/build.yml`) runs the unit tests and builds the debug APK on every push. Download the APK from the run's `ciclo-debug-apk` artifact.

Locally, with JDK 17, Gradle 8.7 and the Android SDK installed:

```sh
gradle :domain:test :app:testDebugUnitTest :app:assembleDebug
```
