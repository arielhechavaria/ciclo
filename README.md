# Ciclo

Offline Android app for one oncologist to track how far each patient is into their chemotherapy treatment.

- Product proposal: `docs/propuesta-ciclo.html`
- Phase 1 plan and specs: `odd/tasks/ciclo-mvp.md`

## Modules

- `domain`: pure Kotlin models and schedule engine, tested with JUnit 5.
- `app`: Android app (Jetpack Compose).

## Build

There is no local Android SDK requirement. GitHub Actions (`.github/workflows/build.yml`) runs the unit tests and builds the debug APK on every push. Download the APK from the run's `ciclo-debug-apk` artifact.

## Releases

Pushing a tag such as `v0.1.0` runs `.github/workflows/release.yml`, which tests, builds a signed APK and publishes it as a GitHub pre-release. Every release uses the same key, so a new APK installs over the previous one without losing data. Run `scripts/setup-release-signing.sh` once to create that key outside the repo and store it as repository secrets, and back up `~/.config/ciclo-signing`.

Locally, with JDK 17, Gradle 8.7 and the Android SDK installed:

```sh
gradle :domain:test :app:testDebugUnitTest :app:assembleDebug
```

## UI screenshots

Every CI run renders the main screens (Agenda in its three views, patients, patient with a treatment, protocols, settings, lock) with fake data and publishes them as PNGs, so UI changes can be reviewed without installing the app.

To download them: GitHub → **Actions** → latest **Build** run → **Artifacts** → `ui-screenshots`. The run summary also lists the file names. Locally they are written to `app/build/screenshots/` by `gradle :app:testDebugUnitTest`.
