# Gate — Phase 0: Skeleton and contracts

## Scope delivered (vs. plan)

- Added `core:domain`, `core:data`, `core:ui`, and `feature:stream`.
- Kept `core:domain` as a pure Kotlin/JVM module with no Android plugin.
- Added the frozen parsing, result, row, snapshot, and lifecycle contracts.
- Wired Hilt/KSP in `app` with an application entry point and root module.
- Added only module boundaries and dependencies; no parsing, streaming, data-source, or
  UI logic was implemented.

## Deviations from plan and why

- The repository has no `build-logic` convention plugins, so the skeleton uses official
  Android/Kotlin plugins directly from the version catalog.
- The plan is at `/PLAN.md` and the base branch is `master`; these repository facts are
  recorded in `docs/DECISIONS.md`.

## Decisions taken (links to DECISIONS entries)

- [Phase 0 repository paths and branch base](../DECISIONS.md#proposed-phase-0-repository-paths-and-branch-base)

## Verification (commands + results)

- `./gradlew build` — **BUILD SUCCESSFUL**

## Staff-reviewer verdict and remaining non-blocking items

No staff-reviewer agent is available in this environment. No review findings remain.

## Known gaps / shortcuts

- The feature has no ViewModel or Route/Screen yet; those belong to later phases.
- Data-source interfaces and implementations belong to Phase 3.
- Compose atoms and UI strings belong to Phase 4.

## Questions I expect in the gate review

- Confirm that the frozen contract package names and module edges are acceptable before
  implementing Phase 1.
