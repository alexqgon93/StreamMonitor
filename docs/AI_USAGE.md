# AI usage

This phase was implemented with GitHub Copilot using the repository conventions in
`.github/copilot-instructions.md` and the delivery plan in `PLAN.md` as the governing
constraints.

Copilot inspected the existing Gradle project, created the module skeleton, added
catalog-backed dependencies and plugins, wired the Hilt application entry point, and
transcribed the frozen domain contracts. The module boundaries, dependency direction,
scope of Phase 0, and the decision to stop before implementation logic were controlled
by the plan and conventions.

For Phase 2, Copilot implemented the coordinator and its JVM tests from the invariants in
the plan. The lazy-computation, worker bound, conflation, retry, and failure-retention
decisions are recorded in `docs/DECISIONS.md`; these are design decisions, not generated
requirements.


For Phase 3, Copilot implemented source interfaces, Ktor clients, mock scenarios, the
BuildConfig-backed Hilt selector, and JVM tests using Ktor's `MockEngine`. The endpoint
contract and mock-data proportions are recorded in `docs/DECISIONS.md`.
