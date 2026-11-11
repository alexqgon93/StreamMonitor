# Stream monitor: design notes

These are my notes on how the screen is put together and why. The app is one screen that
shows a live join of two streams, so most decisions are about keeping a fast-changing list
readable and honest about what state it is in. Values below match the code in `core:ui`,
`feature:stream` and `app`; where something was designed but not built, it says so.

## Principles I held to

- **State is never colour alone.** Every state pairs a colour with a glyph and a text label.
  Tokens are opaque (no alpha on text) and chosen for at least 4.5:1 contrast on their
  background.
- **Zero elevation.** Surfaces are separated by a 1dp outline, not shadows. A dense list stays flat.
- **Text grows, boxes grow with it.** All sizes are `sp` with no ceiling, rows and banners
  use minimum heights, and nothing is ellipsized, so errors and values are never cut off at
  large font scales.
- **The UI primitives know nothing about the domain.** `core:ui` atoms take strings, ints and
  booleans, which is why the design system can be previewed on its own (see the README's
  architecture section).

## Design tokens

Dimensions are dp, type sizes are sp. Dynamic colour is off; the theme is fixed so the
success/warning roles (which Material 3 does not have) stay consistent with the rest.
Edge-to-edge is enabled in `MainActivity`; the app bar surface extends behind the status bar
and only its content is inset.

| Role | Material mapping | Light (implemented) | Dark (planned) | Use |
|---|---|---|---|---|
| Background | background | `#F8FAFC` | `#101820` | Screen |
| Surface | surface | `#FFFFFF` | `#19242D` | App bar, rows |
| On surface | onSurface | `#18212B` | `#E6EEF4` | Main text |
| Muted text | onSurfaceVariant | `#526172` | `#B2C1CC` | Secondary text |
| Outline | outline | `#CBD5DF` | `#536472` | Row borders, counter divider |
| Primary | primary | `#245B77` | `#A6D2E8` | Button, idle/streaming glyph |
| On primary | onPrimary | `#FFFFFF` | `#101820` | Button text |
| Primary container | primaryContainer | `#E7F1F5` | `#243E4D` | Idle/streaming banner, MOCK badge |
| Success | `LocalStreamColors.success` | `#286147` | `#AAD9BC` | Ready glyph and text, completed glyph |
| Success container | `LocalStreamColors.successContainer` | `#E5F3EA` | `#244739` | Completed banner |
| Warning | `LocalStreamColors.warning` | `#765319` | `#F2D18D` | Pending glyph and text |
| Warning container | `LocalStreamColors.warningContainer` | `#FFF2D7` | `#594525` | Defined, currently unused |
| Error | error | `#A12C32` | `#FFB4B8` | Failed glyph and text |
| Error container | errorContainer | `#FCEBED` | `#58282F` | Failed banner |

Only the light scheme exists in code (`StreamMonitorTheme` uses `lightColorScheme`). The dark
column is the palette I worked out up front; wiring it is a matter of adding a second
`ColorScheme` and a second `StreamColors`. `app/ui/theme` still holds the unused Android
Studio template theme and should be deleted.

Success and warning live in a small `StreamColors` composition local rather than being
squeezed into Material roles they do not mean.

**Type.** I use the platform default family (Roboto on Android) through the Material type
roles, and only override sizes to a tighter scale:

| Role | Size / line height | Weight | Use |
|---|---|---|---|
| headlineSmall | 22 / 28 | 600 | App title |
| titleLarge | 20 / 28 | 600 | Empty-state title |
| titleMedium | 17 / 24 | 600 | Banner title |
| titleSmall | 15 / 22 | 600 | Row title, button label |
| bodyMedium | 14 / 20 | 400 | Counters, empty-state detail |
| bodySmall | 13 / 18 | 400 | Banner detail, discarded line, row state and failure reason |
| labelMedium | 12 / 16 | 700 | Section label |
| labelSmall | 11 / 16 | 700 | MOCK badge |
| `StreamResultTextStyle` | 13 / 18 | 600 | `result = 03`; monospace with tabular digits (`tnum`) |

The result text is monospaced with tabular digits so a column of results lines up and a value
changing from `09` to `10` does not shift the layout. It uses `FontFamily.Monospace`, not a
bundled font.

**Spacing and shape.** Spacing scale in `StreamSpacing`: 4, 6, 8, 12, 16, 20, 24, 32. Screen
gutter 16, app bar horizontal padding 20, 6 between rows. Row radius 10, button 12, banner
14, badge 7. Content is capped at 600dp wide and centred on wider screens.

**Motion.** None is implemented. The plan was a short placement animation only when an
existing row changes sorted position. Today a row whose result arrives keeps its
`sourceIndex` key and simply changes position in the list, with no animation.

## Components

Atoms, molecules and the screen are split the way the code is split: atoms in `core:ui`, the
composed screen in `feature:stream`.

**`core:ui` atoms**

| Component | What it is |
|---|---|
| `StateGlyph` | 20dp canvas glyph, so it needs no vector assets. Open circle (idle, streaming, pending), check (completed, ready), exclamation (failed). Tint comes from the state. |
| `MockBadge` | Static `MOCK` label, 56dp wide, min 26dp high, shown only when the data source is `mock`. It is not interactive. |
| `SectionLabel` | `SECTION n` in muted label style, min 34dp, exposed as a heading with a "Section n" description. |
| `ActionButton` | Full-width, min 48dp, radius 12, one per lifecycle (Start / Restart / Retry). |
| `SourceCounts` | Numbers and Inputs received counts in two equal columns with a 1dp divider underneath. Counts are raw received entries, including malformed ones. Each is its own accessibility node ("Numbers received, N"). |
| `LifecycleBanner` | Glyph, title and detail on a tinted container (primary container, success container for Completed, error container for Failed), min 78dp, one merged accessibility node reading "title, detail". |
| `DiscardedSummary` | A single muted line from a plurals resource, min 41dp. |
| `ResultRow` | Title on the left, state on the right. Four visual variants (below). Min 62dp, 74dp when failed, 1dp outline, one merged accessibility node. |

**`ResultRow` variants**

| State | What shows |
|---|---|
| NotNeeded | Title only. The result bit was not set, so there is nothing to wait for. |
| Pending | Open-circle glyph and `Pending` in the warning colour. |
| Ready | Check glyph and `result = 03` in the success colour, monospaced. |
| Failed | Exclamation glyph and `Failed`, plus a second line with `Missing input`, `Invalid input` or `Computation failed`. |

NotNeeded deliberately has no glyph or label: the row is complete as it is, and only rows
that have a result or a problem carry extra marks.

**Screen (`StreamScreen`).** One `LazyColumn` under a fixed app bar: status panel (banner,
counters, action), the discarded line, then sections in ascending order, each with a header
and its rows. A section header appears only once that section has a row. Rows are keyed by
`sourceIndex`. The bottom system inset is added to the list padding so the last row clears
the navigation bar. When there are no rows, an empty state replaces the list; its detail
text differs between idle ("Start the stream to receive values.") and streaming ("Waiting for
valid numbers.").

`StreamRoute` owns the ViewModel and collects state; `StreamScreen` is stateless and takes a
`StreamUiState` plus an `onIntent` callback.

## Lifecycle and what the screen shows

| Lifecycle | Banner | Action | Rows |
|---|---|---|---|
| Idle | Idle / No stream started | Start stream | None; empty state. No discarded line, because no snapshot exists yet. |
| Streaming | Streaming / Receiving values | None | Appear as they are joined and computed. |
| Completed | Completed / Stream finished | Restart stream | Final set. Individual rows can still be Failed (missing or invalid input). |
| Failed | Stream failed / Network error after retries | Retry | Rows already received stay visible. |

Decisions behind this:

- **Failure does not blank the list.** After retries are exhausted the user still sees
  everything received so far. Retry starts a new run (it does not resume); the old rows stay
  until the new run produces its own, while the counters reset to zero immediately. The
  mapper tests pin this.
- **Row failures and discards are different things.** A number that cannot be parsed is
  discarded and counted in the discarded line; it never becomes a row. A missing or invalid
  input leaves its row visible as Failed. They are shown differently because they mean
  different things.
- **Counts are of what was received, not what was kept.** Numbers and Inputs counters include
  entries that were later discarded or failed.
- **The failure banner text is fixed.** The domain carries a message, but the UI always shows the
  same string and does not surface transport error text.

## Ordering

Rows are grouped by section (1 to 4) and, inside a section, ordered by item number, then
result state (NotNeeded, Pending, Ready, Failed), then the computed value for Ready rows, then
source index. The domain owns this (`rowComparator` in `StreamCoordinator.kt`) and hands the
UI a list that is already sorted; the feature layer only groups it. Because source index is
the last tie-breaker the order is total. Source index is also the row's identity, kept
separate from its sort position, so a result arriving moves the row instead of recreating it.

Display numbers are one-based: the parser yields section 0 to 3 and item 0 to 5, and the UI
shows `SECTION 1` to `SECTION 4` and `Item 1` to `Item 6`.

## Accessibility

- Each row is one merged node: "Section N, Item M, state". Pending reads "Pending", NotNeeded
  reads "No result needed", Failed reads the reason, and Ready reads the digits one by one
  ("Result 0 3") rather than as a cardinal number.
- Section labels are headings. The banner reads as one announcement. The action button has
  the Button role and a 48dp minimum height.
- Not built: live-region announcements for lifecycle changes, throttled counter
  announcements, and reduced-motion handling (there is no animation to reduce).

## Copy

All user-facing text is in `feature/stream/src/main/res/values/strings.xml`; the app name is
in `app/src/main/res/values/strings.xml`. Keys follow the pattern `lifecycle_*`,
`result_*`, `failure_*`, `empty_*`, plus `*_accessibility` variants for what a screen reader
says. The discarded line uses a plurals resource (`1 discarded invalid number` /
`N discarded invalid numbers`). The result is formatted as `result = %s` from a two-character
zero-padded string.

## Mock data and the badge

The badge only marks a build as running on mock data. Which scenario runs is chosen at build
time with Gradle properties (`-PmockScenario=happy_path|malformed|unbalanced|large`), so
there is no in-app selector. I considered a bottom sheet with a launcher on the badge and cut
it: it would need mutable data-source configuration, as the README's trade-offs explain.

## Not built

- Dark theme (palette above only).
- An "offline, cached data" lifecycle state. The domain has no such state; a failed run with
  retained rows covers the same need.
- Row placement animation and reduced-motion handling.
- Mock scenario sheet and badge launcher.
- Live-region announcements.
