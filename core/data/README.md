# core:data

Boundary for raw stream data. `NumbersDataSource` and `InputsDataSource` return raw
`List<Int?>` batches; parsing and stream coordination remain in `core:domain`.

The remote sources expect `{"values":[1,null,150]}` from independently configured numbers
and inputs URLs. The Hilt graph selects mock or remote implementations from build properties,
so callers never branch on the active source:

```text
-PstreamDataSource=mock|remote
-PmockScenario=happy_path|malformed|unbalanced|large
-PnumbersEndpoint=https://example.test/numbers
-PinputsEndpoint=https://example.test/inputs
```

The default is `mock` with `happy_path`. Remote mode requires both endpoint properties.
Mock scenario definitions are centralized in `MockStreamDataSources.kt`; no application call
site knows which implementation is active.

The Hilt graph also exposes both sources as domain `StreamEndpoint`s, qualified with
`@NumbersEndpoint` and `@InputsEndpoint`, so the coordinator can be wired without knowing the
active implementation.

## Error classification

This module is the single owner of retry policy. Every transport and payload failure is
translated into `StreamEndpointException` before it reaches the domain:

| Failure | Retryable |
|---|---|
| HTTP 5xx | Yes |
| HTTP 408 and 429 | Yes |
| Connect, socket, and request timeouts | Yes |
| Other HTTP 4xx | No |
| Payload decoding failures | No |
| Any other transport failure | No |

## Known issues

These are accepted shortcuts for a one-day challenge; none of them block the current
single-session usage.

- The `large` scenario is generated twice because each data source resolves the scenario
  separately, so it allocates roughly twice the data it needs at startup.
- `MockNumbersDataSource` and `MockInputsDataSource` hold an unsynchronized cursor in an
  app-scoped singleton, so simultaneous stream sessions would interleave their batches.
- A mistyped `streamDataSource` or `mockScenario` property fails with a raw
  `IllegalArgumentException` at startup instead of a message naming the property.
- `buildConfigField` interpolates endpoint URLs without escaping, so a value containing a
  quote or backslash breaks the generated `BuildConfig`.
- Test `HttpClient` instances are not closed; harmless with `MockEngine` on the JVM.

