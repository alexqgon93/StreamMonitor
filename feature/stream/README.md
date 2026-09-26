# feature:stream

Stream screen boundary. It owns the Route/Screen split, ViewModel, and pure
`StreamSnapshot` to UI-state mapping. It depends on domain contracts and `core:ui`, never on
data implementations.

## Known issue

Phase 5 measured the `LARGE` mock scenario (20,000 numbers plus 20,000 inputs) on the Android
emulator at a mean `2.650822252 s` to `Completed`. The snapshot mapper rebuilt all visible UI
rows on every surviving snapshot and consumed `1.862560118 s` cumulatively in the measurement,
or `70.26%` of that end-to-end mean. A per-row reference cache was prototyped and verified for
correctness, but it regressed the measured mapper time to `2.222957407 s` cumulatively, or
`82.10%` of a `2.707627418 s` mean end-to-end run. It was measured and reverted, since it made
things worse rather than better: the cache still traversed every row and added map/list
bookkeeping.

The next optimisation should change the stream contract to expose row deltas or bound UI mapping
cadence before mapping. Reusing row references alone cannot remove the full-snapshot traversal.
