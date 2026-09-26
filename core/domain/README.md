# core:domain

Pure Kotlin/JVM contracts and domain types. This module must not depend on Android,
data sources, UI, or feature modules.

Rows are ordered within each section by `itemIndex`, then `ResultState.sortOrder`, then
`sourceIndex`. The state order is `NotNeeded`, `Pending`, `Ready`, `Failed`: `NotNeeded`
comes before `Ready` to match the brief's "Item 4" before "Item 4 result=03" example.
Pending is placed before computed results because it is unresolved, and Failed is placed
last as the terminal error state. `Ready` values share the same state sort key and therefore
use `sourceIndex` rather than their display value as the final ordering tie-breaker. These
state placements are application assumptions.

JSON number values will be represented as `Int` in the data DTO, so the parser must retain
`InvalidReason.OUT_OF_BYTE_RANGE` for values outside `0..255`.
