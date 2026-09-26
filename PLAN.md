# Build plan

One working day. Phases are ordered by risk, not by visibility. Each phase ends with a gate
that is verified before moving on.

**Stop at every gate.** Do not start work from a later phase.

## Time budget

| Phase | Budget | Cumulative |
|---|---|---|
| 0 — Skeleton and contracts | 0:45 | 0:45 |
| 1 — Parsing and computation | 0:45 | 1:30 |
| 2 — The stream coordinator | 2:15 | 3:45 |
| 3 — Data sources and mocks | 1:00 | 4:45 |
| 4 — UI | 1:30 | 6:15 |
| 5 — Evidence: tests and performance | 1:00 | 7:15 |
| 6 — Documentation | 1:00 | 8:15 |
| Buffer | 0:45 | 9:00 |

**Cut from the bottom, never the top.** If time runs short, the UI gets simpler and mock
scenarios get dropped. The coordinator, its tests, and the documentation of decisions do not.

---

## Phase 0 — Skeleton and contracts (0:45)

- Multi-module skeleton per `CONVENTIONS.md`, version catalog, Hilt wired, compiles.
- Freeze the domain contracts before any logic exists:

```kotlin
// Parsing
sealed interface ParseResult {
    data class Valid(
        val sectionIndex: Int,   // 0..3
        val itemIndex: Int,      // 0..5
        val needsResult: Boolean,
    ) : ParseResult
    data class Invalid(val reason: InvalidReason) : ParseResult
}

enum class InvalidReason { ITEM_OUT_OF_RANGE, NULL_VALUE, OUT_OF_BYTE_RANGE }

// Row state — mirrors the four visual variants
sealed interface ResultState {
    data object NotNeeded : ResultState
    data object Pending : ResultState
    data class Ready(val value: String) : ResultState        // zero-padded "00".."99"
    data class Failed(val reason: FailureReason) : ResultState
}

enum class FailureReason { MISSING_INPUT, INVALID_INPUT, COMPUTATION_FAILED }

data class StreamRow(
    val sourceIndex: Int,        // stable identity, never changes
    val sectionIndex: Int,
    val itemIndex: Int,
    val result: ResultState,     // mutates as inputs arrive
)

// What the UI observes
data class StreamSnapshot(
    val lifecycle: StreamLifecycle,
    val rows: List<StreamRow>,       // already sorted
    val numbersReceived: Int,
    val inputsReceived: Int,
    val discardedCount: Int,
)

sealed interface StreamLifecycle {
    data object Idle : StreamLifecycle
    data object Streaming : StreamLifecycle
    data object Completed : StreamLifecycle
    data class Failed(val message: String) : StreamLifecycle
}
```

**Gate:** contracts reviewed and frozen. `./gradlew build` green.

---

## Phase 1 — Parsing and computation (0:45)

Pure functions, pure Kotlin, fully tested. Fast and high value.

- `NumberParser`: bits 0–1 section, bits 2–6 item, bit 7 result flag. Item > 5 is invalid.
  Handles `null` entries. Never throws.
- `ResultComputer`: returns the last two digits, zero-padded to two characters. Simulates
  CPU cost so the performance work later is meaningful — **not** with `delay`, which would
  make it a suspension rather than CPU load. A bounded busy computation or a real arithmetic
  workload, with the simulation clearly marked and easy to disable.
- Tests with explicit byte vectors, including these verified values:

| byte | section | item | valid | result bit |
|---|---|---|---|---|
| 4 | 0 | 1 | yes | 0 |
| 150 | 2 | 5 | yes | 1 |
| 12 | 0 | 3 | yes | 0 |
| 21 | 1 | 5 | yes | 0 |
| 136 | 0 | 2 | yes | 1 |
| 16 | 0 | 4 | yes | 0 |
| 24 | 0 | 6 | **no** | 0 |
| 255 | 3 | 31 | **no** | 1 |
| 131 | 3 | 0 | yes | 1 |

Also test: exactly 48 of the 256 byte values parse as valid.

**Gate:** all parsing tests green. Read the parser and confirm the bit shifts by hand against
two of the vectors above.

---

## Phase 2 — The stream coordinator (2:15) — the hard part

This is where the challenge is won or lost. Budget the most time here and protect it.

### The problem

Two endpoints are polled independently. Each returns the values available at request time,
possibly truncated. Index `i` of numbers corresponds to index `i` of inputs, but the two
streams advance at different rates, so either can arrive first for a given index. An empty
array means that stream is done.

### Decisions to make explicitly

1. **Index tracking.** Each stream keeps its own cursor; the absolute index is the cumulative
   count received from that stream. Both cursors advance independently.
2. **Join strategy.** An index-keyed store holds what is known so far. A number arriving
   creates a row (if valid) or increments the discarded count. An input arriving is stored
   and may trigger computation. Both paths converge on the same store.
3. **When to compute.** Compute lazily — only when the number at that index is known, valid,
   and has the result bit set. This avoids wasted CPU on inputs whose rows will never show a
   result, at the cost of latency when the number arrives after the input. **Document the
   trade-off; the alternative is eager computation with a discard.**
4. **Parallelism.** `Dispatchers.Default.limitedParallelism(N)`. N is a named constant with a
   comment justifying it.
5. **Emission cadence.** The UI never sees one emission per value. Conflate or sample so the
   snapshot rate is bounded independently of arrival rate.
6. **Sorting.** Maintain order per section rather than re-sorting everything. A result
   arriving moves an existing row — same `sourceIndex`, new sort key.
7. **Termination.** The stream is complete when both endpoints have returned empty **and**
   all triggered computations have settled. A pending row must not leave the UI stuck.
8. **Failure.** Transient errors retry with backoff; exhausted retries move the lifecycle to
   `Failed` **without erasing rows already received**.

### Required tests, all with virtual time

- Numbers arrive before inputs for an index, and vice versa
- Inputs arrive for an index whose number was invalid (must be ignored, not crash)
- Unbalanced streams: one finishes long before the other
- A `null` input produces `Failed(MISSING_INPUT)`, not a discard
- Result bit not set: the row never enters `Pending`, no computation is scheduled
- A row moves position when its result arrives, keeping the same identity
- Retry on transient error, then recovery
- Exhausted retries: lifecycle `Failed`, existing rows preserved
- Completion only after both streams are empty and computations have settled

**Gate:** read the coordinator line by line. Be able to answer, without looking: what happens
when an input arrives for an index whose number hasn't arrived yet, and what stops the
computation dispatcher from being saturated.

---

## Phase 3 — Data sources and mocks (1:00)

- `NumbersDataSource` / `InputsDataSource` interfaces returning raw DTOs.
- Remote implementations against the two endpoints.
- Mock implementations behind the same interfaces.
- **The switch:** a build-time or runtime configuration change, never a code edit.
- Scenarios (four, not six):
  - **Happy path** — balanced streams, mostly valid values
  - **Malformed data** — nulls, out-of-range values, malformed payloads
  - **Unbalanced streams** — inputs lag far behind numbers, one finishes early
  - **Large stream** — enough values to make the performance claim measurable

Mock generators state their valid/invalid ratio. Random bytes are ~19% valid; a generator
that ignores this produces an almost empty list.

**Gate:** each scenario runs end to end and produces the expected shape of output.

---

## Phase 4 — UI (1:30)

The brief says a fancy UI is not part of the challenge. Build what the states require and
stop.

- Tokens and type scale from the design spec (cheap, makes it look intentional).
- Atoms: state glyph, section label, result row, action button, counts, discarded summary.
- `StatusPanel`: lifecycle banner, counts, one contextual action.
- Lazy grouped list with section headers and **stable keys by source index**.
- Empty state, mock badge, mock scenario sheet.
- All strings in `strings.xml`, discarded count as a `<plurals>`.
- Row semantics merged into one spoken node; section headers get heading semantics.

**Cut if time is short:** the mock scenario sheet (fall back to a build-config switch), the
empty-state illustration, dark theme.

**Do not cut:** stable keys, strings in resources, the four row states.

**Gate:** run it on a device against each mock scenario. Confirm rows move rather than
flicker when results arrive.

---

## Phase 5 — Evidence and performance (1:00)

Both are explicit requirements in the brief and are easy to forget.

**Evidence that it works:**
- A short test summary in the README: what is covered at each layer and why.
- Mutation-check the two or three tests that guard non-obvious properties — break the
  implementation, confirm the test fails, restore. Mention that you did this.

**Evidence that it is performant:**
- Measure, don't assert. Run the large-stream scenario and record a concrete number:
  values processed per second, time to first row, or frame timing during streaming.
- Check recomposition counts with the Layout Inspector and record before/after if you tuned
  anything.
- One paragraph in the README with the numbers and the conditions they were measured under.

**Gate:** you have at least one real measured number, not an estimate.

---

## Phase 6 — Documentation (1:00)

Three documents. The README is the one that gets read.

**`README.md`**
- What it is, how to run it, how to switch mock/real.
- Architecture: module graph and why each boundary exists.
- **Key decisions**, each with: the problem, the choice, and **what it cost**.
  - Lazy vs eager computation
  - Bounded parallelism and why that bound
  - Emission cadence decoupled from arrival cadence
  - Stable identity separate from sort key
  - Retain rows on global failure
- **Assumptions**, because the brief says ambiguity is intentional:
  - Item values 6–31 are invalid and discarded (5 intermediate bits, 6 titles)
  - Sort order within a section, and where Pending/NotNeeded/Failed rows fall
  - Whether retry resumes or restarts
  - Index pairing assumes cumulative receive order per stream
- **Shortcuts and what you would do next**, with the threshold at which you would change
  each decision.

**`AI_USAGE.md`** — explicitly required by the brief.
- Which tools, for what.
- What you decided versus what was generated: the contracts, the conventions, the phase
  gates and the review are yours.
- Reference `CONVENTIONS.md` and this plan as the mechanism.
- Be specific and honest. A vague answer here reads worse than none.

**`CONVENTIONS.md`** — already written; keep it accurate.

**Gate:** read the README end to end as a stranger. Every claim in it is true of the code.

---

## Final checklist

- [ ] Clone fresh into a temp directory and build from scratch
- [ ] Both mock and real paths are reachable; the switch is documented
- [ ] No string literals left in composables
- [ ] `PLAN.md` rewritten as a delivery record matching what was actually built
- [ ] Every requirement in the brief maps to something in the README
