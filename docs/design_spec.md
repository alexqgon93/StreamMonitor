# Stream monitor — Android UI design specification

## Design tokens

All dimensions are dp; type sizes are sp. Dynamic color is off. Android status and navigation bars follow the theme surface. The SVGs are 390dp reference compositions; the 200% specimen shows a scrolled portion of the screen.

| Semantic role | M3 mapping | Light | Dark | Use |
|---|---|---|---|---|
| Background | background | `#F8FAFC` | `#101820` | Screen |
| Surface | surface | `#FFFFFF` | `#19242D` | App bar, rows, sheet |
| On surface | onSurface | `#18212B` | `#E6EEF4` | Main text |
| Muted text | onSurfaceVariant | `#526172` | `#B2C1CC` | Secondary text |
| Outline | outline | `#CBD5DF` | `#536472` | Row strokes, dividers |
| Primary | primary | `#245B77` | `#A6D2E8` | Controls, progress identity |
| On primary | onPrimary | `#FFFFFF` | `#101820` | Button text |
| Primary container | primaryContainer | `#E7F1F5` | `#243E4D` | Streaming, offline, mock |
| Success | custom success/onSuccessContainer | `#286147` | `#AAD9BC` | Ready and completed icon/text |
| Success container | custom successContainer | `#E5F3EA` | `#244739` | Completed banner |
| Warning | custom warning/onWarningContainer | `#765319` | `#F2D18D` | Pending icon/text |
| Warning container | custom warningContainer | `#FFF2D7` | `#594525` | Reserved for alert variant |
| Error | error | `#A12C32` | `#FFB4B8` | Failure icon/text |
| Error container | errorContainer | `#FCEBED` | `#58282F` | Global failure banner |

Colors are paired with icons and labels; never interpret a state from hue. Text on its stated background targets WCAG AA (normal text at least 4.5:1). Use opaque values only. Do not apply alpha to text tokens.

| M3 type role | Size / line height | Weight | Font | Use |
|---|---|---|---|---|
| HeadlineSmall | 22 / 28 | 600 | Roboto | App title |
| TitleLarge | 20 / 28 | 600 | Roboto | Sheet title, empty title |
| TitleMedium | 17 / 24 | 600 | Roboto | Lifecycle title |
| TitleSmall | 15 / 22 | 600 | Roboto | Row title, button |
| BodyMedium | 14 / 20 | 400 | Roboto | Counters, empty support |
| BodySmall | 13 / 18 | 400 or 600 | Roboto | Status, discard line, reason |
| LabelMedium | 12 / 16 | 700 | Roboto | Section title |
| LabelSmall | 11 / 16 | 700 | Roboto | MOCK badge |
| Result | 13 / 18 | 600 | Roboto Mono, tabular digits | Result text and 00–99 |

Font scaling uses Android `sp` without a ceiling. At 200% scale the section title is 24sp, row title 30sp, status 26sp and reason at least 24sp. Width is measured at runtime; text wraps and height expands. Do not ellipsize errors or values.

Spacing scale: `4, 6, 8, 12, 16, 20, 24, 32`dp. Screen gutter 16dp; app bar horizontal 20dp; between rows 6dp. Shapes: row 10dp, button 12dp, banner 14dp, badge 7dp, sheet top corners 22dp, grabber 2dp. Elevation: 0dp throughout; row outline 1dp. No shadow or blur. Motion: only row placement movement when an existing row changes sorted position, 220ms FastOutSlowIn; respect reduced motion by disabling it. Insertions, loading icon, banner and sheet have no designed animation.

## Component inventory by Atomic Design level

**Atoms**

| Component | Purpose and variants | Size, padding, tokens | Accessibility |
|---|---|---|---|
| `StateGlyph` | Text glyph or equivalent 20dp vector with fixed shapes: idle circle, streaming open circle, completed check, failed exclamation, offline link break; row pending open circle, ready check, failed exclamation. | 20×20dp; corresponding primary/success/warning/error. | Decorative when adjacent to a spoken state label; never the sole status cue. No loading rotation. |
| `MockBadge` | Mock builds only; static `MOCK`. | 56×26dp, radius 7dp, primaryContainer/onPrimaryContainer (primary text); label 11sp. | Text is announced when focusing the app bar; no action. Production: component absent. |
| `SectionLabel` | `SECTION %1$d`; section number ascending. | Full width, min height 34dp; top text baseline 19dp; horizontal 4dp within list gutter; label 12sp muted. | Heading semantics; reads “Section N”, not individual capital letters. At 200% height grows to fit. |
| `ActionButton` | `Start stream`, `Restart stream`, `Retry`; one button according to lifecycle. | Full available width, min height 48dp, radius 12dp, primary/onPrimary; horizontal padding 16dp. | Button role, matching visible label, min target 48dp; disabled only during transition to streaming and then removed. |

**Molecules**

| Component | Purpose and variants | Size, padding, tokens | Accessibility |
|---|---|---|---|
| `SourceCounts` | Independent received counts: Numbers and Inputs; zero at idle, frozen at completed/failure/offline. These are counts of received array entries, including malformed entries. | 2 equal columns, 34dp high, start aligned; gap 8dp; 14sp onSurface; bottom 1dp outline divider 16dp below values. | Two separate semantics nodes, reading “Numbers received, N” then “Inputs received, N”; updates throttled for TalkBack, no live announcement per value. |
| `LifecycleBanner` | Idle, Streaming, Completed, Failed, Offline cached. Title and detail exactly from copy deck, leading `StateGlyph`. Offline is a presentation state layered over cached snapshot; retained stream lifecycle is not overwritten. | Width fill, min 78dp, padding 16dp; icon 20dp; icon/text gap 10dp; title 17sp, detail 13sp; container primaryContainer except completed successContainer, failed errorContainer. Text wraps and height grows. | One grouped announcement: title then detail. Failed banner uses assertive announcement once; other changes polite once. |
| `DiscardedSummary` | Visible when any snapshot exists, including count zero; invalid numbers never become rows. | Min height 41dp; left 4dp in list gutter; 13sp muted; pluralized resource. | Reads explicit count; updates are throttled and never steal focus. |
| `ResultRow` | Exactly four result variants: NotNeeded has title only and no result label; Pending `◌ Pending`; Ready `✓ result = 03`; Failed `! Failed` plus one of `Missing input`, `Invalid input`, `Computation failed`. | Fill width; min 62dp or 74dp failed; outline 1dp; radius 10dp; padding horizontal 14dp, top 15dp, bottom 12dp; title left 15sp; state right 13sp; failure reason next line left 12–13sp. At 200%, title, state, reason stack left aligned, min height 198dp in specimen, intrinsically sized in implementation. | One merged row semantics node, reading in order “Section N, Item M, [No result needed | Pending | Result zero three | Failed, reason]”. Keep section association in description even if header is offscreen. No click action. Result digits read individually (“zero three”), not as cardinal three. No live region on each row; preserve focus when moved. |

**Organisms**

| Component | Purpose and variants | Size, padding, tokens | Accessibility |
|---|---|---|---|
| `StatusPanel` | `LifecycleBanner`, `SourceCounts`, and lifecycle action; idle start, completed/offline restart, global failure retry, streaming no action. | Banner margin 16dp; counters margin top 16dp; divider margin bottom 24dp; action below divider 48dp with 16dp horizontal margin; 20dp after action. | Focus order: banner, Numbers count, Inputs count, action. Global failure keeps cached rows below the action. |
| `GroupedResultList` | Virtualized sections 1–4, ascending. Within section sort item number ascending, then result sort key when item tie occurs; stable source index resolves remaining ties. Header per present section only. Empty variant `EmptyContent`. | Lazy list, 16dp horizontal content padding, 6dp row gap, 4dp header inset; no nested vertical scrolling. | Heading semantics for `SectionLabel`; each row merged as specified; stable keys use source index. Item placement is the sole animation. |
| `EmptyContent` | No valid rows during idle or early stream. | Centered in remaining viewport, 19sp title, 14sp support, 8dp gap, 24dp side padding. | Title then support; not a button. Idle copy differs from streaming empty copy. |
| `MockScenarioSheet` | Mock builds only; six single-select scenarios in fixed order, current selection marked with radio. | Modal bottom sheet, surface, 22dp top radius, 4×42dp grabber, title padding top 32dp / sides 24dp, each option min 72dp high, side padding 24dp, radio 24dp, 1dp separator. Scrollable at 200%. | Modal heading then options in order; each is radio role with selected state and 48dp minimum tap region. Dismiss by back/scrim/swipe; selection updates mock scenario and closes sheet. No production entry point. |

**Template**

| Component | Purpose and variants | Size, padding, tokens | Accessibility |
|---|---|---|---|
| `StreamScreenTemplate` | One screen: system status bar, fixed app bar, scrollable status panel + discard summary + grouped list, system navigation area; optional modal sheet. Portrait and landscape use same single-column order. | App bar 64dp under status bar, 20dp horizontal padding; content max width 600dp centered on wider landscape, minimum 16dp side gutter; content uses one lazy scroll container. Bottom system inset added to list padding. | Linear traversal app bar → status → counts → action → discarded count → sections/rows. On 200% font scale all cards grow, list scrolls, no horizontal scroll. |

**Page**

| Component | Purpose and variants | Size, padding, tokens | Accessibility |
|---|---|---|---|
| `StreamMonitorPage` | Idle, Streaming, Completed, Failed, Offline cached; plus dark theme, large font, and mock sheet presentations. | Uses `StreamScreenTemplate`; title `Stream monitor` and optional `MockBadge` in app bar. Mock badge acts as the sheet launcher with an invisible expanded 48×48dp tap target around it; production omits it. | Launcher semantics “MOCK, choose scenario, button”; sheet receives focus when opened and returns focus to launcher when closed. System back dismisses sheet. |

The badge launcher is an exception to the badge’s static visual appearance: its visual badge is static, while its parent 48dp control opens the sheet. Do not expose nested accessibility nodes. Rows have stable identity by source index, with a derived sort key; keep source index and result state separate so a result update can move the existing keyed row.

## Screen layouts

- **a — Streaming:** `StreamMonitorPage` with mock launcher, `StatusPanel` in Streaming state, `SourceCounts` 12/9, `DiscardedSummary` 2, and `GroupedResultList` sections 1–4. Rows demonstrate NotNeeded, Pending, Ready `03`, Failed missing input, Ready `48`, Failed invalid input. Visible rows below viewport continue on scroll.
- **b — Completed:** completed banner and counts 14/14, `Restart stream`, discarded summary 2, grouped rows. A completed snapshot may still include individual failed results from bad or absent inputs.
- **c — Global failure:** failed banner stating network error after retries, counts 12/9, `Retry`, discarded summary and already cached rows. Retry resumes/restarts according to data contract; UI never erases cached rows before new values arrive.
- **d — Initial/empty:** idle banner, counts 0/0, `Start stream`, `EmptyContent` and no discard summary because no snapshot exists.
- **e — Cached offline:** offline banner, last received counts 12/9, `Restart stream`, discarded summary and saved rows; preserved statuses are those in the cache. Restart attempts the network and keeps rows visible.
- **f — Dark streaming:** same data and structure as a with dark tokens.
- **g — 200% type:** scrolled crop consisting of one `SectionLabel` and one Failed `ResultRow`. This intentionally illustrates intrinsic height and stacked row content; it is a specimen rather than another page.
- **h — Mock scenario selector:** mock streaming screen under a modal `MockScenarioSheet`; selected option Slow & chunked. The underlying screen is inaccessible until dismissal.

The list is virtualized. Only currently visible rows and headers are composed. A row may change order when its result sort key changes; the item placement motion applies to the moved row only. No charts, progress bars, loading shimmer, illustration, or per-row action.

## Copy deck

All entries are Android string resources; `%1$d` means integer formatting. Plurals use Android quantity resources with the displayed count argument. Glyphs/icons are assets, not text resources.

| Key | English value / format arguments |
|---|---|
| `app_title` | `Stream monitor` |
| `mock_badge` | `MOCK` |
| `mock_launcher_accessibility` | `MOCK, choose scenario` |
| `lifecycle_idle` | `Idle` |
| `lifecycle_idle_detail` | `No stream started` |
| `lifecycle_streaming` | `Streaming` |
| `lifecycle_streaming_detail` | `Receiving values` |
| `lifecycle_completed` | `Completed` |
| `lifecycle_completed_detail` | `Stream finished` |
| `lifecycle_failed` | `Stream failed` |
| `lifecycle_failed_detail` | `Network error after retries` |
| `lifecycle_offline` | `Offline · cached data` |
| `lifecycle_offline_detail` | `Previously received values` |
| `numbers_received` | `Numbers  %1$d` |
| `inputs_received` | `Inputs  %1$d` |
| `numbers_received_accessibility` | `Numbers received, %1$d` |
| `inputs_received_accessibility` | `Inputs received, %1$d` |
| `start_stream` | `Start stream` |
| `restart_stream` | `Restart stream` |
| `retry` | `Retry` |
| `discarded_count` | plural: `one: %1$d discarded invalid number`; `other: %1$d discarded invalid numbers` |
| `section_title` | `SECTION %1$d` |
| `section_accessibility` | `Section %1$d` |
| `item_title` | `Item %1$d` |
| `result_pending` | `Pending` |
| `result_ready` | `result = %1$s` (two-character zero-padded string `00`–`99`) |
| `result_failed` | `Failed` |
| `failure_missing_input` | `Missing input` |
| `failure_invalid_input` | `Invalid input` |
| `failure_computation` | `Computation failed` |
| `result_not_needed_accessibility` | `No result needed` |
| `result_ready_accessibility` | `Result %1$s` (speak each digit) |
| `empty_title` | `No items yet` |
| `empty_idle_detail` | `Start the stream to receive values.` |
| `empty_streaming_detail` | `Waiting for valid numbers.` |
| `mock_scenario_title` | `Mock scenario` |
| `mock_happy` | `Happy path` |
| `mock_slow` | `Slow & chunked` |
| `mock_flaky` | `Flaky network` |
| `mock_malformed` | `Malformed data` |
| `mock_unbalanced` | `Unbalanced streams` |
| `mock_large` | `Large stream (50k)` |

## Assumptions and open questions

- The prompt defines six possible item titles and four sections but not the function assigning a valid number to either. The mock data assumes the server/domain layer provides item and section identifiers 1–6 and 1–4. The UI does not derive these from the numeric value.
- “Within a section, rows sorted by item, then by result” assumes a deterministic domain sort key for result, including Pending/Failed/NotNeeded, supplied by the data layer. The pictured order uses different item numbers, so it does not imply an unprovided cross-state ordering. Stable source index is the final tie breaker.
- A stream can fail globally after retries while retaining rows from its partial snapshot. Cached offline is a connection presentation over the last lifecycle snapshot. Exact semantics of retry versus restart, and whether the stream resumes or starts over, remain data-layer questions.
- Counts report raw received array elements independently. Discarded counts reflect invalid parsed numbers; missing/invalid inputs are row failures and do not increase discarded numbers. A pending row can remain while the inputs stream lags.
- Mock selection changes the next mock run; current stream is not silently restarted. Production removes both badge/launcher and selector. UI language is English because all example strings in the request are English; resource keys permit localization.
- The use of `MOCK` as a 48dp launcher is a deliberate accessible access point for the required selector without adding a separate visible control. The only sheet options are the six requested scenarios.
- The exact item/result grouping data shown in mockups is illustrative. All component states and format constraints are normative; spec controls if visual examples differ.