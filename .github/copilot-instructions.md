# Repository conventions

Rules that apply to every change in this repository, regardless of who or what writes it.
Written as prohibitions where possible: a rule that can be violated silently is not a rule.

## Stack and structure

- Kotlin, Jetpack Compose, Coroutines/Flow, Hilt.
- Modules:
  - `core:domain` — **pure Kotlin JVM, no Android dependencies.** Parsing, the stream
    coordinator, the result computer, domain models.
  - `core:data` — data sources (remote and mock), DTOs, the source switch.
  - `core:ui` — Compose design tokens and stateless atoms. Takes primitives only.
  - `feature:stream` — ViewModel, UI state, screen.
  - `app` — composition root.
- Dependency direction is one way. `core:domain` depends on nothing in the project.
- Gradle version catalog for all dependencies. No hardcoded versions in module scripts.

## Non-negotiable technical rules

### Concurrency and threading

- **Never run the result computation on the main dispatcher.** It is CPU-bound with
  multi-millisecond outliers. Use `Dispatchers.Default` with bounded parallelism.
- **Never launch unbounded work per incoming value.** Parallelism is explicitly limited;
  the bound is a named constant with a comment explaining the choice.
- **Whoever creates a `CoroutineScope` is responsible for cancelling it.** Components that
  need a scope receive one; they do not create their own.
- **Never let the UI collect an unthrottled per-value stream.** Snapshots emitted to the UI
  are conflated or sampled. The emission cadence is independent of the arrival cadence.
- Never call a suspending function while holding a lock.

### Parsing and validation

- Parsing is a **pure function** in `core:domain` with no I/O and no logging. It returns a
  sealed result: parsed or invalid-with-reason. It never throws for bad input.
- **An invalid number never becomes a row.** It increments the discarded count.
- **`null` in a numbers array and `null` in an inputs array are different failures** and are
  modelled separately. A null input is a row-level failure, not a discarded number.
- Bit layout is fixed and documented in one place: bits 0–1 section, bits 2–6 item value,
  bit 7 result flag. Item values above 5 are invalid.

### Data sources

- The remote and mock implementations sit behind the **same interface**. No call site knows
  which one it is using.
- **Never make the switch a code edit.** It is a build-time or runtime configuration change.
- Mock scenarios are data, not branches scattered through the implementation.
- A mock that generates random bytes produces ~81% invalid values. Mock generators state
  their intended valid/invalid ratio explicitly.

### State and identity

- Rows have a **stable identity by source index**, separate from their sort key. A result
  arriving must move an existing row, not replace it with a new one.
- Sorting is deterministic and total: item ascending, then result sort key, then source
  index as the final tie-breaker. No two rows ever compare equal.
- **Never re-sort the whole dataset on every update.** Sorting is scoped to what changed.

### UI

- `core:ui` atoms take primitives (`String`, `Int`, `Boolean`) — never domain types.
- Route/Screen split: the Route knows Hilt and the ViewModel; the Screen is stateless,
  takes a UI state and emits intents.
- **All user-visible text lives in `strings.xml`.** No string literals in composables.
  Plurals use `<plurals>`, never manual singular/plural branching.
- Numeric text that updates in place uses tabular figures.
- Every interactive element has a minimum 48dp touch target and a content description.
- The list is lazy with stable keys.

### Testing

- `core:domain` is tested on the JVM with **virtual time** — `runTest`, `TestDispatcher`,
  `advanceTimeBy`. **Zero real `delay`, zero `Thread.sleep`.**
- Time-dependent behaviour is tested against an injected time source, never the system clock.
- Every parsing rule has a test with an explicit byte value and expected output.
- Tests that guard a non-obvious correctness property are mutation-checked: break the
  implementation, confirm the test fails, restore it.

## What not to do

- No offline caching. It is not in the requirements.
- No analytics, crash reporting, flavors for their own sake, or release signing.
- No UI polish beyond the defined tokens and states. The brief states a fancy UI is not part
  of the challenge.
- No dependency added without a stated reason in the README.
