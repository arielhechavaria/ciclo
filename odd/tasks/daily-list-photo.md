# Daily list photo (Phase 2, OCR)

Objective: register patients from a photo of the day's patient list, read on the phone with OCR, reviewed and confirmed before anything is saved.
Branch: `feat/daily-list-photo` · Build and tests run only in GitHub Actions · Delivery: work-unit commits on the branch, merged to `main` after green CI when the user says so (as in earlier Phase 2 features) · Forecast: ~1,100 authored lines.

## Specs

Quoted fragments are verbatim from the user or the approved proposal (`docs/propuesta-ciclo.html`). Items marked **Assumption** were decided by the assistant and can be reopened.

- **S1 — OCR engine.** "ML Kit incluido, sin permiso de red." Bundled `com.google.mlkit:text-recognition` (model linked into the APK, works offline, no Google Play Services). The merged manifest must not request `android.permission.INTERNET` nor `android.permission.ACCESS_NETWORK_STATE`; library permissions are removed with `tools:node="remove"`. A test proves it.
- **S2 — Data read.** "Los datos son nombre y apellisdos, carnet de identidd y tratamiento." Each row of the list yields a name, an identity card number (carnet) and a treatment text.
- **S3 — Identity card field.** "sí, campo nuevo opcional y único". Patients gain an optional identity card number, unique among patients. **Assumption:** a Cuban carnet is 11 digits; spaces are ignored and anything else is rejected by the form. Stored format: Room schema v3 (`patients.identity_card`, unique index, `MIGRATION_2_3`, `3.json` committed, migration tested from v2); backup JSON gains the optional field `identityCard` (absent reads as none, FORMAT stays 1; repeated cards make a backup invalid). Patient form shows the field; patient search also matches it.
- **S4 — Flow (proposal).** "Haces la foto de la lista de pacientes del día." "La app la lee en el propio móvil. La foto no sale del teléfono y se borra al terminar." "Tú revisas lo que leyó y lo confirmas." "Los pacientes nuevos se registran con el formulario ya relleno. A los que ya existen se les aplica su seguimiento." "Funciona bien con listas impresas y peor con letra a mano."
- **S5 — Matching.** A row matches an existing patient by identity card. **Assumption:** with no card match, a row whose name equals a patient's name ignoring case, accents and extra spaces is offered as a probable match that the user confirms or rejects. The treatment text is matched to a catalog protocol ignoring case, accents and spacing; no match leaves the protocol to choose.
- **S6 — Existing patients.** **Assumption** for "se les aplica su seguimiento": the review shows each existing patient's progress (as in the proposal mock: "Ya registrada · C2 hoy", "9 de 12"); confirming fills the identity card if the patient had none, and marks the session planned for today as given when the row's "Sesión de hoy dada" check is on (on by default, shown only when a session is planned today).
- **S7 — New patients.** A new row opens the patient form prefilled with name and identity card (the clinical record number is still required and typed by hand), then the assign-treatment form with the matched protocol preselected.
- **S8 — Row layout.** **Assumption:** lists are tables, and OCR returns text by column, so rows are rebuilt from line positions (lines that overlap vertically form one row, read left to right). Each row is read in the order the user gave: name and surnames, then the card, then the treatment; a row with no 11-digit card, or no name before it, is shown as unread. "La lista es flexible y se ajustará cuando veamos una real."
- **S9 — Mocked data.** Until the public release, the screen offers a sample list (mock mode only) that exercises an existing patient by card, a probable name match, a new patient and an unknown treatment, so the flow can be tried without a real list.

## Tasks

| ID | Specs | Task | Route | Model | Risk | Status | Commit |
|----|-------|------|-------|-------|------|--------|--------|
| T1 | S3 | Identity card field: domain, Room v3 + migration test, backup field, form, search | inline | opus | 1 (stored format) | written, CI not run (commit blocked) | |
| T2 | S2, S5, S8 | Domain: row rebuilding, row parsing, matching against patients and protocols (TDD) | inline | opus | none | written, CI not run | |
| T3 | S1, S4, S6, S7, S9 | App: ML Kit, camera capture and deletion, review screen, apply, no-network manifest test, sample list | inline | opus | 5 (dependency, CI) | written, CI not run | |

Routes: inline, no Writer trigger (units depend on each other: T2 uses T1's field, T3 uses T2). T1 gets its own commit and an independent verifier (risk 1).

## Log

- **L1 (2026-11-19, user, verbatim):** "Comenzamos con el OCR"
- **L2 (2026-11-19, user, verbatim):** "ML Kit incluido, sin permiso de red. Los datos son nombre y apellisdos, carnet de identidd y tratamiento."
- **L3 (2026-11-19, user, verbatim):** "sí, campo nuevo opcional y único"
- **L4 (2026-11-19):** ML Kit bundled vs unbundled verified in the official docs (bundled: model linked at build time, ~4 MB per script per architecture, works without network). The app requests no network permission today. Next: T1.
- **L5 (2026-11-19):** Engram mirror `odd/daily-list-photo/tasks` is pending: auto mode could not check the `mem_save` call because a hook changes its input (same as ciclo-mvp L4).
- **L6 (2026-11-19):** Auto mode denied `git commit` and `git push`, so the test-first commit could not reach CI and RED was not observed. All three tasks are written in the working tree and wait for the user's permission to commit and push; no local JDK, so nothing has compiled yet.
- **L7 (2026-11-19):** T1: `Patient.identityCard` (normalised, 11 digits), `PatientConflict` with `DuplicateIdentityCardException`, Room v3 with `MIGRATION_2_3` and `CicloDatabase.MIGRATIONS`, backup field `identityCard` (repeated or malformed cards refuse the backup), form field and errors, search by card, card line in the patient view. `3.json` must be committed from the CI `room-schemas` artifact after the first green run.
- **L8 (2026-11-19):** T2: `dailylist/DailyList.kt` in `domain` (`DailyListReader`, `DailyListMatcher`, `matchProtocol`, `ReviewDecision`, `ListAction`, `DailyListApplier`). T3: `ListPhoto` (FileProvider target in the private cache, read with bundled ML Kit, deleted in `finally` and on screen entry), `DailyListScreen` and `DailyListReview`, "Foto de la lista" button in Pacientes, `AssignTreatmentDialog(preselected, startDate)`, `treatmentProgress` extracted from the patient view, network permissions removed in the manifest, `NoNetworkTest` plus a CI step that fails if the APK requests network access, mock identity cards and `MockData.sampleList()`. Camera opens with `lockGuard.suspended` so the lock does not drop the result.
