# core:domain

Pure Kotlin/JVM contracts and domain types. This module must not depend on Android,
data sources, UI, or feature modules.

Rows are ordered within each section by `itemIndex`, then `ResultState.sortOrder`, then
`ResultState.resultSortKey` (the numeric value for `Ready`, 0 otherwise), then `sourceIndex`. The state order is `NotNeeded`, `Pending`, `Ready`, `Failed`: `NotNeeded`
comes before `Ready`, so an `Item 4` row with no result sits above an `Item 4` row showing
`result = 03`.
Pending is placed before computed results because it is unresolved, and Failed is placed
last as the terminal error state. `Ready` rows in the same item are ordered by their computed value, and `sourceIndex` is the
final tie-breaker. These state placements are application assumptions.

JSON number values will be represented as `Int` in the data DTO, so the parser must retain
`InvalidReason.OUT_OF_BYTE_RANGE` for values outside `0..255`.
