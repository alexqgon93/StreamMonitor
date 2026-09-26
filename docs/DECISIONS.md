# Decisions

## Proposed: Phase 0 repository paths and branch base

The repository stores the plan at `/PLAN.md`, conventions at
`.github/copilot-instructions.md`, and has `master` as its only existing base branch.
Phase 0 therefore uses those paths and creates `phase/0-skeleton-contracts` from
`master`.

## Proposed: Phase 2 coordinator decisions

- The coordinator computes lazily: only a valid number with its result bit set and a valid
  input schedules work. This avoids wasted CPU for rows that never display a result, at the
  cost of waiting when the two streams deliver their matching indexes at different times.
- Four fixed workers on `Dispatchers.Default.limitedParallelism(4)` bound CPU work without
  creating one worker coroutine per incoming value. The bound is deliberately small for a
  mobile CPU and is isolated behind a named constant.
- The coordinator drains its pending-job queue in batches of at most four computations on
  `Dispatchers.Default.limitedParallelism(4)`. It creates no persistent workers or work
  channel, and never creates more than four computation coroutines at once.
- Snapshots are conflated at the coordinator boundary so UI consumers cannot receive one
  emission per incoming value.
- When inputs end, result-bearing rows with no paired input resolve to
  `Failed(MISSING_INPUT)` before the lifecycle reaches `Completed`.
- A terminal endpoint failure emits `StreamLifecycle.Failed` and leaves all rows already
  received in the final snapshot.
- Endpoint adapters classify failures with `StreamEndpointException`: retryable transport
  failures retry with linear backoff; malformed or terminal failures become a failed
  lifecycle without escaping the coordinator flow.
