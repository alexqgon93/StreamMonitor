# AI usage

## Approach

AI was used throughout this project as an implementation and documentation assistant,
directed by a human-authored process: contracts and rules frozen before implementation,
review by an independent agent, and evidence required before accepting any claim.

That process is the point of this document. An unguided "implement this spec" pass tends to
default to the path of least resistance on every ambiguous or hard decision — eager
computation instead of lazy, an unbounded coroutine per value, a shared generic DTO instead of
the contract's actual JSON keys, a mapper that reconstructs everything because that's simpler
to write. None of those fail to compile. They fail the four things this brief explicitly asks
to be demonstrated: correctness, resilience, performance, separation of concerns. The
sections below show where that showed up in practice, and how the process caught it.

## The mechanism, concretely

- **`.github/copilot-instructions.md`** — non-negotiable technical rules, written before any
  code existed: bounded CPU work, conflated UI emission, stable row identity, no locks around
  suspension, all text in resources.
- **`PLAN.md`** — phases in risk order, each with a frozen contract and a gate I verified
  personally before allowing the next phase to start.
- **`docs/DECISIONS.md`** — the architectural decisions and their costs, recorded as they were
  made.

None of these are AI output. They are the human-authored constraints the AI operated inside.
The gap between "AI implements a spec" and this repository is almost entirely explained by
that scaffolding — not by the underlying model being smarter on this task than on any other.

## Where the frozen rules changed the outcome

For each of these, an unconstrained implementation pass would plausibly have produced the
easier, weaker version. The rules and contracts fixed in `CONVENTIONS.md`/`PLAN.md` before
writing code are what pushed the implementation to the harder, correct one instead.

- **Lazy vs. eager computation.** Nothing in the brief forces lazy scheduling — eager
  computation with a discard is simpler to write and easier to reason about locally. The plan
  required lazy scheduling explicitly, with the cost stated up front (a result waits if its
  input arrives before its number). Without that constraint, the natural default is eager.
- **Bounded parallelism.** An unguided implementation commonly launches one coroutine per
  incoming value — it works, until the input is large. `Dispatchers.Default.limitedParallelism(4)`
  with batch draining was a requirement in the plan, not something the model proposed
  spontaneously.
- **Stable identity separate from sort order.** The easy version keys a `LazyColumn` by list
  position. The plan required a stable `sourceIndex` key from the start, specifically because
  a naive implementation would only reveal the bug once a result changed a row's position —
  which is exactly the kind of defect that looks fine in a quick manual test.
- **All UI text in resources.** Left unconstrained, generated Compose UI defaults to string
  literals in composables — faster to write, and the majority of generated Android UI code
  I've seen do exactly that. `CONVENTIONS.md` made this a stated rule from the first UI file.
- **The JSON contract.** Left to infer a "reasonable" payload shape, the first draft used a
  single generic `{"values": [...]}` for both endpoints — simpler to model with one shared
  DTO. The brief actually specifies two distinct keys (`numbers`, `computation_input`). This
  was caught in review, not by the writer noticing on its own, and fixed to use two DTOs with
  explicit `@SerialName`s matching the brief. It's a clear example of the model choosing
  internal simplicity over spec fidelity when nothing forced otherwise.

## Where guidance wasn't enough on its own — findings from review

Freezing contracts controls the big defaults. It doesn't catch everything; some things only
surface when a second pass actively tries to break the first one.

**The "malformed" scenario didn't exercise its own advertised failures.** The mock scenario
was built to demonstrate both row-failure paths (`MISSING_INPUT`, `INVALID_INPUT`), and its
KDoc claimed a 50/50 valid/invalid number ratio. An independent oracle — a from-scratch
reimplementation of the bit-parsing rules, deliberately not reusing `NumberParser` — showed
the malformed inputs happened to sit on indices whose *numbers* were already invalid, so
those inputs were discarded before they could fail a row. The scenario compiled, ran, and
matched its documented number ratio, while silently failing to demonstrate the one thing it
was named for. Fixed by moving the malformed inputs to indices with valid, result-bearing
numbers, and adding a test that pins the alignment itself so it can't silently drift again.

**A test verified its own mock instead of production wiring.** An early test for "fresh data
sources per coordinator run" built its own factory lambda inline. A mutation that broke the
actual production code — hoisting endpoint resolution outside `create()` — still passed,
because the test never called the real `StreamDataSourceModule` provider. It was measuring
itself. Rewritten to call the production factory directly with counting providers; the same
mutation then failed as it should have.

**A claim of "wired" wasn't the same as "reachable."** After adding Hilt `@Provides` bindings
for the two endpoints, the summary reported the data layer as "wired." It compiled and the
bindings were correctly qualified — but nothing in the graph actually depended on them yet, so
Dagger's reachability was never exercised. This was flagged mid-project on a second look,
before I had to catch it: the distinction between "the binding exists and compiles" and "the
graph resolves it" was left explicitly open until Phase 4 wired the ViewModel, which is what
actually exercises it.

**A measured optimization made things worse, and was reverted because of the measurement.**
The Phase 5 large-stream benchmark showed the UI mapper consuming 70.26% of total run time by
rebuilding the full row list on every surviving snapshot. A per-`sourceIndex` reference cache
was implemented, and it was functionally correct — verified with dedicated identity tests. Real
measurement on the same benchmark showed it made the metric worse (mapper share rose to
82.10%, mean run time increased by ~57ms), because it still walked every row and added map/list
bookkeeping on top. It was reverted specifically because the number went the wrong way, not
because it looked inelegant. This is the clearest example in the project of the difference
between verifying a change with tests and actually measuring it — only the second one caught
this.

**HTTP retry classification was split across two layers.** A review found that Ktor's raw
5xx exceptions were escaping untranslated, while the domain coordinator's retry logic only
recognized `IOException` and an already-translated `StreamEndpointException(retryable=true)`.
5xx would have failed permanently on the first attempt. Fixed by making `core:data` the single
owner of transport/HTTP classification — 5xx, 408, 429, and connection timeouts translate to
retryable; other 4xx and payload failures translate to terminal — with a test asserting that
removing the coordinator's `IOException` branch changes nothing, because the domain no longer
needs to know about transport exception types at all.

## Why this process, not just AI output

Every one of the corrections above was **findable**, once something — a frozen contract, an
independent reviewer, or a re-measurement — forced a second look. None of them required
insight beyond what the tools already had; they required a process that didn't accept the
first plausible-looking answer:

1. State the constraint before implementation, so the easy wrong answer isn't the default.
2. Review with a different reasoning path than the one that wrote the code.
3. Demand evidence — a failing mutation, a measured number, a test against production wiring
   — rather than a description of intended behavior.
4. Be willing to revert a change that measured worse, even after it was implemented and
   tested.

An unguided pass on the same brief would very plausibly compile, pass a happy-path smoke test,
and still contain every defect listed above, because none of them fail loudly — they fail
quietly, in exactly the ways this process was built to surface. That gap is why the process
above was applied throughout rather than treated as optional polish.

## Process followed each phase

1. Read the relevant section of `PLAN.md` and `CONVENTIONS.md` before writing anything.
2. State the phase's scope and frozen contract before implementation began.
3. Build and test the affected module in isolation.
4. Request independent review for phases with concurrency, lifecycle, or performance risk.
5. Triage findings — fix, defer with a stated reason, or document as a scope cut.
6. Re-run validation after every fix.
7. Stop at the phase boundary. Do not let the agent start the next phase unprompted.

This document is the record of that process.
