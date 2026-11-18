# Ciclo MVP (Phase 1)

Objective: offline Android app that tracks each patient's chemotherapy progress, with the timeline view (prototype E) as the home screen.
Repo: https://github.com/arielhechavaria/isora-medical-app (private) · Branch: `feat/ciclo-mvp` · Build and tests run only in GitHub Actions (no local JDK or SDK) · Delivery: `ask-on-risk`, chain strategy asked before the first commit (forecast ~5,000 authored lines) · Test runner: JUnit 5 for the pure Kotlin `domain` module, JUnit 4 + Robolectric + Compose UI tests for `app`.

## Specs

Quoted fragments are verbatim from the user, his wife's review, or the approved proposal (`docs/propuesta-ciclo.html`). Items marked **Decision (assumed)** were decided by the assistant on request: "responde tu misma las pregintas que faltan".

- **S1 — Platform.** Android only, fully offline, sideloaded APK, no Google services, no server, no account. "No depende de internet ni de los sistemas del hospital." Stack: Kotlin, Jetpack Compose, Room. minSdk 26 (Android 8.0) **Decision (assumed)**: Cuban devices are often old.
- **S2 — Architecture.** Hexagonal with screaming packages by feature: `protocols`, `patients`, `treatments`, `agenda`, `notifications`, `security`. A pure Kotlin `domain` module with no Android dependencies holds the models and the schedule engine; `app` holds Room adapters and Compose UI.
- **S3 — Treatment model (confirmed by assumption).** "Un tratamiento tiene ciclos, y cada ciclo dura un número de días y tiene una o más sesiones." Example: "gemcitabina + cisplatino, 4 ciclos de 21 días, con sesiones el día 1 y el día 8." The app counts both levels: "ciclo 1 de 4, sesión 2 de 2".
- **S4 — Protocol catalog.** Preloaded with the 8 protocols of the proposal (AC, AC dosis densa, Paclitaxel semanal, TC, FOLFOX, CAPOX, Carboplatino + paclitaxel, Gemcitabina + cisplatino). "La base está bien. Siempre se puede gestionar: añadir, editar y archivar protocolos." Stores structure only: cycle length in days, default number of cycles or "hasta progresión", session day offsets. Never doses. Editing a protocol never changes treatments already assigned (they keep a snapshot).
- **S5 — Patient form.** "Nombre o iniciales · nº de historia clínica · diagnóstico (opcional) · notas". Clinical record number is unique.
- **S6 — Assign treatment.** "Protocolo del catálogo · nº de ciclos · fecha de inicio". The cycle count defaults from the protocol and can be overridden: "FOLFOX adyuvante son 12 ciclos, pero en enfermedad metastásica puede ser otro número." The app generates every session and the end date.
- **S7 — Postpone and recalculate.** "Basta con marcar la sesión como pospuesta y poner la nueva fecha." "Si un ciclo se retrasa, se recalcula todo lo que viene después." Reference example (must be a test): AC every 21 days, 4 cycles, start 2026-11-03 → C2 2026-11-24, C3 2026-12-15, C4 2027-01-05, end 2027-01-26; postponing C2 by 7 days → C2 2026-12-01, C3 2026-12-22, C4 2027-01-12, end 2027-02-02.
- **S8 — Session status.** Each session is planned, given, or postponed. Marking given or postponed is available from the patient view.
- **S9 — End of treatment and history.** **Decision (assumed):** "termina" means the end of the last cycle (26 ene in the AC example), not the last session, because the last cycle still includes its recovery and toxicity-watch period. The last session is highlighted ("última sesión") in the views. The day after the last cycle ends, the patient leaves the main views and moves to the history: "Cuando un paciente termina, sale de la vista principal y pasa al histórico, dentro de la lista de pacientes." A manual "finish early" action (suspension) also moves the patient to the history, with a reason.
- **S10 — Until-progression treatments.** "No tienen fecha de fin. La app lo indica así y te avisa para reevaluar cada cierto número de ciclos, el que tú elijas."
- **S11 — Agenda views (prototype E), all in Phase 1.** User: "pasa ala fase 1.. todas las vistas del prototipo e". The Agenda tab has the segmented control of prototype E, "Semana · Mes · Línea", with the visual tokens of design A. Tapping a patient in any view opens the patient view. Designed for ~40 active patients and must stay smooth at 150.
  - **S11a — Línea (home default).** "Una fila por paciente en tratamiento. Cada bloque es un ciclo y cada marca es una sesión [...] La línea vertical es hoy." Legend: ciclo, sesión, última sesión, pospuesto. Horizontal scroll: "Desliza a la derecha para ver más semanas".
  - **S11b — Semana.** Design A ("Clara", light cards): the sessions of the selected week grouped by day, with patients finishing and postponed sessions flagged; previous and next week navigation.
  - **S11c — Mes.** Design D ("Calendario"): month grid where each day shows a person icon with a badge counting that day's sessions; tapping a day lists its patients.
- **S12 — Patient view.** Cycles and sessions with dates and status, progress "ciclo X de N, sesión Y de M", expected end date.
- **S13 — Patient list.** Active patients and a history section with finished patients, with search by name or clinical record number.
- **S14 — Friday notification.** "Todos los viernes, con lo que viene la semana siguiente." **Decision (assumed):** Fridays at 08:00 local time, time configurable in settings. Content: who finishes next week, sessions next week grouped by day, postponed sessions, and until-progression patients due for reevaluation. Local alarms only, rescheduled after reboot. A setup screen asks to disable battery optimization: "Hay que desactivar el ahorro de batería para esta app, o el móvil puede cancelar los avisos."
- **S15 — Lock and privacy.** "La ley cubana protege los datos de salud. Por eso la app guarda lo mínimo y se bloquea con PIN." PIN plus optional fingerprint; database encrypted at rest with a key protected by Android Keystore; lock after the app goes to background. **Decision (assumed):** no PIN recovery in Phase 1; forgetting it means reinstalling (stated on the PIN setup screen).
- **S16 — Release.** Signed release APK that installs by sideloading, with a one-page install guide in Spanish.

Out of Phase 1 (Phase 2, **Decision (assumed)** for the open questions): photo of the daily list with on-device OCR; patients by protocol, postponed, and finishing-this-month lists; post-treatment follow-up (default proposal: review at 3 months, configurable); encrypted backup to a file. Expected active load: ~40 patients.

## Tasks

| ID | Specs | Task | Route | Model | Risk | Status | Commit |
|----|-------|------|-------|-------|------|--------|--------|
| T1 | S1, S2 | Scaffold: `git init`, Gradle project with `domain` and `app` modules, Compose, Room, test runners, feature packages | inline | sonnet | 5 (dependencies) | done, CI run 44328819078 green | e2efa22 |
| T2 | S3, S6, S7, S9, S10 | Schedule engine in `domain`: generate sessions and end date, postpone with cascade, finish rules, until-progression, TDD with the S7 example | inline | opus | 6 | done, RED run 44331369591 (29/30 failed), GREEN run 44341324981 | fa2cc14, ad26429, de4dc0c |
| T3 | S4, S5, S6, S8 | Persistence: Room entities, DAOs, repository adapters for domain ports, seed of the 8 protocols, protocol snapshot on assignment | inline | opus | 1 (stored format) | pending | — |
| T4 | S4 | Protocol catalog UI: list, add, edit, archive | inline | sonnet | none | pending | — |
| T5 | S5, S6 | Patient form and assign-treatment form | inline | sonnet | none | pending | — |
| T6 | S8, S9, S12 | Patient view: sessions, mark given or postponed, finish early | inline | sonnet | none | pending | — |
| T7 | S11, S11a | Agenda tab with Semana · Mes · Línea switch; Línea as default: Canvas Gantt, today line, legend, scroll, tap to open | inline | sonnet | none | pending | — |
| T12 | S11, S11b | Semana view (design A) | inline | sonnet | none | pending | — |
| T13 | S11, S11c | Mes view (design D) with per-day badge and day detail | inline | sonnet | none | pending | — |
| T8 | S9, S13 | Patient list with history and search; navigation bar Agenda · Pacientes · Protocolos | inline | sonnet | none | pending | — |
| T9 | S14 | Friday notification: exact alarm, boot receiver, content builder (unit tested in `domain`), battery-optimization screen, time setting | inline | sonnet | 6 | pending | — |
| T10 | S15 | PIN and fingerprint lock, SQLCipher encryption, Keystore key | inline | opus | 2 (security) | pending | — |
| T11 | S16 | Signed release APK and Spanish install guide | inline | sonnet | 5 (release) | pending | — |

Order: T1 → T2 → T3 → (T4, T5, T6, T7, T12, T13, T8 in sequence) → T9 → T10 → T11. T10 goes before any real patient data is entered. Risk items 1, 2 and 5 get their own commit and an independent verifier after their checks.

## Log

- **L1 (2026-11-18, user, verbatim):** "responde tu misma las pregintas que faltan... decido el.prototipo e. vamos a usar gentle ai... haz elplan de tareas."
- **L2 (2026-11-18):** Prior decisions come from the wife's review recorded in `docs/propuesta-ciclo.html` ("Lo que decidiste") and project memory. The six open questions of the proposal were answered by assumption in S3, S9, S11, S14 and the Phase 2 note; any of them can be reopened by her.
- **L3 (2026-11-18):** "decido el prototipo e" interpreted as: timeline (E) is the home and the only agenda view in Phase 1. Next step: T1.
- **L4 (2026-11-18):** Engram mirror `odd/ciclo-mvp/tasks` is pending: auto mode could not check the `mem_save` call because a hook changes its input. Resync when available.
- **L5 (2026-11-18, user, verbatim):** "estoy de acuerdo con el plan pero pasa ala fase 1.. todas las vistas del prototipo e". Rewrote S11 into S11a-S11c (Semana, Mes, Línea all in Phase 1), removed them from Phase 2, added T12 and T13. Forecast rises to ~6,000 authored lines.
- **L6 (2026-11-18):** Environment check: no `java`, no Android SDK (`ANDROID_HOME` unset). T1 is blocked until JDK 17 and the Android SDK are installed.
- **L7 (2026-11-18, user, verbatim):** "ienes que crear un repo en github...la compilacion la haremos usando githu actions entiendes?" Created private repo `arielhechavaria/isora-medical-app`, `main` holds docs and plan (be111a8). Build config reuses the proven setup of the user's `rodrigo` app: AGP 8.5.2, Kotlin 1.9.24, Compose compiler 1.5.14, Gradle 8.7 via `setup-gradle` (no wrapper). RED/GREEN evidence for later tasks is observed in CI runs: push the failing test first, then the fix.
- **L8 (2026-11-18):** T1 done. CI run 44328819078: unit tests and `assembleDebug` succeeded, APK uploaded as `ciclo-debug-apk`. No separate verifier: the CI run executed the full build. Next: T2 schedule engine.
- **L9 (2026-11-18, user, verbatim):** "si, empieza con T2".
- **L10 (2026-11-18):** T2 done in `domain` (`protocols/Protocol.kt`, `treatments/Treatment.kt`). Decisions: postponing any session (also D8) shifts that session, every later one and the end date by the same delay ("se recalcula todo lo que viene después"); postponements are stored as delay in days, not absolute dates, so postponing an earlier session later still moves a later postponed one (T3 must persist delays); the end date is the day after the last cycle ends (AC example: 2027-01-26); finishing early drops sessions on or after the finish date; reevaluation dates are the starts of cycles kN+1. The test for the delay case was added in the GREEN step, so its RED was not observed in CI. A test that walked an infinite until-progression sequence hung CI run 44332090480 (cancelled); fixed by bounding it, and the test step now has a 10-minute timeout. Next: T3 persistence.
- **L11 (2026-11-18, user, verbatim):** "guarda el resumen en engram, necesito el repo publico y haybquebir haciendo realse para ir prpbando." Repo made public after a secret scan (no matches). Added `release.yml` (tag `v*` → tests, signed APK, GitHub pre-release) and `scripts/setup-release-signing.sh`. Auto mode denied creating the key and secrets, so the user runs the script once. Engram summary blocked by a hook in auto mode.
