# Observability Kit — API gaps

Where the Observability Kit and Flow made a use case in this module impossible,
awkward or surprising.

The module uses the Micrometer-based kit (`observability-kit-micrometer`,
`observability-kit-spring`, `observability-kit-starter`) on Flow 25.4. The kit
ships server-side binders (request, RPC, session, session lock, UI, navigation,
errors, data, database), a Micrometer Observation path for tracing, insights at
`/actuator/vaadin/observability`, and an in-browser collector
(`VaadinMetricsClient.js`). The collector posts its samples through the
`@ClientCallable recordSamples` of the `<vaadin-metrics-collector>` element.
The kit covers most of the use cases in
[#277](https://github.com/vaadin/use-cases/issues/277); the entries below are
what is still missing or awkward with the kit in place.

Meters referenced below use the kit's `MeterNames` constants. The server records
`vaadin.request.duration`, `vaadin.rpc.duration` (tagged `type` / `outcome`),
`vaadin.navigation`, `vaadin.errors`, `vaadin.sessions.*`, `vaadin.session.lock.*`
and `vaadin.ui.*`. The client collector records, into the *same* registry,
`vaadin.client.bootstrap.duration`, `vaadin.client.navigation.duration` (tagged
`route` / `trigger`), `vaadin.client.request.duration`,
`vaadin.client.render.duration`, `vaadin.client.web_vitals.lcp`,
`vaadin.client.web_vitals.fcp`, the `vaadin.client.errors` counter,
`vaadin.client.connection` and `vaadin.client.connection.downtime`.

**Reachability is not the problem.** `ClientMetricsBinder` records the browser's
samples into the same server-side `MeterRegistry` every other binder uses, so a
Flow view can inject the `MeterRegistry` and read the client meters like the
server ones. The gaps below are about what the client measures, granularity,
correlation and completeness, not about getting client data to the server.

## 1. No public client-side request-lifecycle hook

**Where it bites:** UC1 / InteractionLatencyView.java (following one click end to end); the kit's in-browser collector.
**Symptom:** application and add-on code cannot learn when a UIDL request
starts and ends in the browser, or which click started it. Flow's client
`RequestResponseTracker` (TypeScript since
[flow#24951](https://github.com/vaadin/flow/pull/24951)) only has
`startRequest()` and `endRequest()`, and nothing about requests is published
on `window.Vaadin.Flow.clients[id]`. The collector can time requests (#2), but
it cannot tell which request a timing belongs to, or which click caused it.
**Workaround used:** the kit reads the Resource Timing entry of each UIDL `POST`
for the round trip, `getProfilingData()` for the render time, and the loading
transitions of `window.Vaadin.connectionState` as the signal that a request
ended.
**Suggested API:** `window.Vaadin.Flow.addRequestListener({ onRequestStart, onResponseReceived, onRendered })`
with a correlation id, so a click can be followed from the browser to the
server.
Upstream: https://github.com/vaadin/flow/issues/7369

## 2. Client request timing misses WebSocket requests and the paint

**Where it bites:** UC1 / InteractionLatencyView.java (the browser's share of a click).
**Symptom:** the kit times each UIDL request in the browser as
`vaadin.client.request.duration`, from the Resource Timing entry of the UIDL
`POST`, and the response handling as `vaadin.client.render.duration`, from
`window.Vaadin.Flow.clients[id].getProfilingData()`. Two parts are still
missing. With `@Push(transport = WEBSOCKET)` the UIDL goes over the websocket,
leaves no Resource Timing entry, and the request meter records nothing. And the
render figure is Flow's processing time, not the time until the browser has
painted the result. The render figure also needs request timing on (#7).
**Workaround used:** the module uses `@Push` with the default transport, where
requests from the browser still go over XHR. UC1 reads the processing time as
the render time, and subtracts the server's
`vaadin.request.duration{vaadin_request_type="uidl"}` from the browser's round
trip to show the network's share.
**Suggested API:** a request timer that works on every transport (built on the
hook in #1), and a "painted" mark after a response has been applied.
Upstream: https://github.com/vaadin/observability-kit/issues/424

## 3. Client samples are aggregated and uncorrelated

**Where it bites:** UC1 / InteractionLatencyView.java (browser, network and server for one click), UC4 / InteractionTraceView.java (one trace), UC7 / MonitoringStackView.java.
**Symptom:** two limits.
1. `ClientMetricsBinder#ingest` records each browser sample into a `Timer`. The
   single values the collector sent (each with its own `ts` and `valueMs`) are
   lost. A view can read the count, mean and max of
   `vaadin.client.navigation.duration`, but never the value of one navigation.
2. Browser samples and the server's `vaadin.request.duration` /
   `vaadin.rpc.duration` share no id. The collector sends no W3C `traceparent`
   on the UIDL request, so the server trace (the `vaadin.request` observation)
   starts at the server, not at the click. Spring Boot would continue one:
   `management.tracing.propagation.consume` includes W3C by default. The
   missing half is on the client.
**Workaround used:** aggregates only. UC4 shows how far the server half reaches:
with a tracing bridge on the classpath (`spring-boot-micrometer-tracing-brave`
and `micrometer-tracing-bridge-brave`), the kit's request and RPC observations,
the application's own spans and every `vaadin.db.query` form one trace that
starts at the HTTP span. Without a bridge the observations produce no spans,
and nothing says so.
**Suggested API:** a correlation id per UIDL request, given to both the client
hook (#1) and the server, ideally a W3C `traceparent` that the browser creates
and the server continues, so browser, server and backend are one trace.
Upstream: https://github.com/vaadin/observability-kit/issues/423

## 4. Push transport is not instrumented on the client

**Where it bites:** UC1, UC2 / ApplicationHealthView.java, UC5 / ConnectionInsightsView.java (any app that uses `@Push`).
**Symptom:** `VaadinMetricsClient.js` watches the History API,
`PerformanceObserver`, global error events and the connection state, but not
the push connection. Server-initiated updates delivered over WebSocket or long
polling produce no client timing or outcome samples, so for a push-heavy app
the client view of responsiveness is blind.
**Workaround used:** none.
**Suggested API:** a client hook (#1) that also covers the push connection, so
the collector can time a push message from receipt until it is applied.
Upstream: https://github.com/vaadin/observability-kit/issues/424

## 5. No event when client samples arrive, and no whole-outage figure

**Where it bites:** UC5 / ConnectionInsightsView.java, UC2 / ApplicationHealthView.java
**Symptom:** the kit records the browser's connection state
(`vaadin.client.connection`, `vaadin.client.connection.downtime`) and keeps
browser errors as `client-error` insights. Two things are still left to the
application:
1. `ClientMetricsBinder#ingest` and the insight collectors raise no event an
   application can listen to, so a view that shows client data has to poll or
   be refreshed by hand. For UC5 polling is wrong: a poll is a UIDL request,
   and one that gets through ends the outage for the browser, so a polling tab
   reports less downtime than there was.
2. `vaadin.client.connection.downtime` is split by `state` (`reconnecting`,
   `connection-lost`). That is right, because Flow enters `connection-lost`
   only after its retries fail. But the length of a whole outage is the sum of
   both tags, and each consumer has to find that out.
3. `vaadin.client.connection` is tagged by `state` only, and the collector
   posts it once the browser has the server back, so nothing says what the
   last known connection state of one UI is.
**Workaround used:** UC5 recomputes its readout on the screen's own
interactions, and the script that simulates the recovery calls a
`@ClientCallable` on the view once the browser has the server back — a
stand-in for the missing event that a real outage would not make. UC5 adds the
two downtime tags itself. UC2's health badge, which is about one tab, reports
the cadence of that UI's own poll requests.
**Suggested API:** a service event when client samples are recorded (e.g. a
`ClientSamplesRecordedEvent`), a documented whole-outage meter or recording
rule, and the last reported connection state per UI (e.g. on the
`<vaadin-metrics-collector>` element).

## 6. UI-state size: bytes are configured, and there is no per-session view

**Where it bites:** UC3 / ScalingSignalsView.java, `HeapCostProbe.java`
**Symptom:** with `vaadin.observability.ui-state=true` the kit measures each
UI's state tree (`UiStateSampler`, `UiStateMetricsBinder`) and publishes
aggregate gauges such as `vaadin.ui.state.nodes`. What is still missing:
1. **Bytes are configured, not measured.** `vaadin.ui.state.size` exists only
   when the application sets `vaadin.observability.ui-state-bytes-per-node`.
   Nodes are a poor unit for it: a `Grid` with a hundred rows is a few nodes
   that carry most of the weight, so one constant fits a mixed application
   badly, and nothing notices when the views get heavier.
2. **No per-session breakdown.** `UiStateMetricsBinder` is package-private and
   keeps its per-UI figures to itself, so "which user is holding all that
   state?" cannot be answered in the app. The meters rightly stay aggregate.
3. **No way to request a measurement.** Sampling follows the kit's schedule,
   and the RPC hook runs when an invocation ends, so a click that grows the
   tree shows only on the next refresh.
4. **Off by default, and stale by design.** An idle user's state is from their
   last interaction. `vaadin.ui.state.sample.age.max` shows how stale the
   aggregate is, but consumers have to remember to read it.
**Workaround used:** UC3's `HeapCostProbe` measures the heap cost per node out
of band (settle the heap, keep a batch of views, read `MemoryMXBean`) and says
whether the configured value still holds. UC3's "grow this view's state"
button shows the lag instead of hiding it. UC3 had a per-session table and
dropped it.
**Suggested API:** a per-UI retained-size estimate from the kit (or a
documented calibration helper), a read-only per-session accessor for the
sampled figures, and a way to request a sample of the current UI.

## 7. Server request timing on the client is off in production

**Where it bites:** UC1 / InteractionLatencyView.java, UC2 / ApplicationHealthView.java
**Symptom:** Flow sends each request's server time to the browser in the UIDL
response (`timings`, from `VaadinSession#getLastRequestDuration()`), and the
client publishes `window.Vaadin.Flow.clients[id].getProfilingData()`, which
returns `[lastProcessing, totalProcessing, serverCumulative, serverLast, bootstrap]`.
Both only happen when `DeploymentConfiguration#isRequestTiming()` is true, and
that defaults to `!productionMode`. So in production the browser gets no server
time, and the kit's `vaadin.client.render.duration` is not recorded. On the
server the gap is closed: `RequestEndedEvent#getDuration()` (Flow 25.4,
[flow#25961](https://github.com/vaadin/flow/pull/25961)) gives each request's
server time to a listener.
**Workaround used:** `application.properties` sets `vaadin.requestTiming=true`.
**Suggested API:** profiling data that is safe to have on in production by
default, or a documented recommendation to set `vaadin.requestTiming=true`
together with the kit.

## 8. No business action name on the kit's meters

**Where it bites:** UC1 / InteractionLatencyView.java (which action is slow?)
**Symptom:** Flow's RPC invocation listener lets `RpcMetricsBinder` time each
invocation as `vaadin.rpc.duration`. The invocation's event (e.g. `click`) and
target component go on the RPC span (`ObservationNames.KEY_EVENT_NAME`,
`KEY_COMPONENT`) and into the interaction insights UC6 reads. But
`vaadin.rpc.duration` is tagged only by `type` and `outcome`, and
`vaadin.errors` only by exception type, to bound cardinality. A business action
("save order", not the `click` that carried it) is recorded nowhere.
**Workaround used:** UC1 records its own `acme.invoice.action{action=…}` timer.
**Suggested API:** an opt-in, bounded resolver the application supplies (e.g.
route plus a logical action name) that the kit applies as meter tags, so
dashboards can group by view or action without unbounded cardinality.
Upstream: https://github.com/vaadin/observability-kit/issues/427

## 9. Insights are an untyped map in process

**Where it bites:** UC5 / ConnectionInsightsView.java, UC6 / FailureInsightsView.java, UC8 / LazyListLatencyView.java
**Symptom:** `InsightsService#payload()` returns a `Map<String, Object>` of
nested maps and lists, shaped for the Actuator endpoint. A view that renders
insights in the app has to cast its way through it
(`(List<Map<String, Object>>) payload.get("insights")`, then
`(Map<String, Object>) insight.get("evidence")`), with unchecked casts and
string keys. `type` is a string each view must know, whether `message` or
`detail` is present says whether collection was on, and a field the browser
never sent is a `null` in a map rather than an empty `Optional`. The JSON shape
is a good contract for agents and a poor one for Java callers.
**Workaround used:** `com.example.acme.Insights` holds the casts and the
accessors once, with `@SuppressWarnings("unchecked")` — every application that
reads insights needs such a helper.
**Suggested API:** typed insight objects (e.g. `List<Insight>` with id,
severity, summary, evidence and examples) next to the JSON rendering.
Upstream: https://github.com/vaadin/observability-kit/issues/427

## 10. Dashboards cannot group by component or view

**Where it bites:** UC7 / MonitoringStackView.java (the external dashboard), UC1, UC6.
**Symptom:** for the same cardinality reason as #8, the component and event of
an interaction are only on spans and insights. An external dashboard can group
latency and errors by RPC type, route or exception, but never by component or
view. "Which button is slow?" is answerable in the app and in a trace backend,
but not in Grafana, which is where an operations team looks first.
**Workaround used:** UC7's dashboard groups RPC latency by `type`; per-component
questions are left to the in-app views.
**Suggested API:** the resolver from #8, applied to meter tags, so a dashboard
can group by a small set of logical action names.
Upstream: https://github.com/vaadin/observability-kit/issues/427

## 11. Percentiles are silently unavailable unless buckets are enabled per timer

**Where it bites:** UC7 / MonitoringStackView.java
**Symptom:** the kit's timers publish count, sum and max, but
`histogram_quantile` needs `_bucket` series, which only appear when
`management.metrics.distribution.percentiles-histogram.<meter>` is set for each
meter. A p95 panel on a kit timer is empty with no error anywhere: the metric
exists, the buckets do not. Neither the kit nor its documentation says so, and
the application has to repeat the meter names in its configuration.
**Workaround used:** UC7 enables buckets for `vaadin.request.duration`,
`vaadin.rpc.duration` and `vaadin.session.lock.wait`, and its readout checks
whether the bucket series exist.
**Suggested API:** a kit-level switch (e.g.
`vaadin.observability.percentiles=true`) that turns on histogram buckets for
the kit's own latency meters.

## 12. No public way to flush the in-browser collector

**Where it bites:** UC2 / ApplicationHealthView.java (the "flush client metrics now" button), UC5 / ConnectionInsightsView.java
**Symptom:** `VaadinMetricsClient.js` buffers its samples and posts them every
~5 s, so a view that reads `vaadin.client.*` right after load shows "no samples
yet" for several seconds. The collector has a drain,
`window.__vaadinMicrometer.flush()`, but only as an internal: the kit's source
comments it `// Expose for tests / dashboards (debug only)`. There is no
supported API, on either side, to say "send what you have now".
**Workaround used:** UC2 and UC5 call `window.__vaadinMicrometer.flush()` from
`executeJs`, guarded so it does nothing if the internal goes away.
**Suggested API:** a public flush on the client collector (e.g.
`window.Vaadin.observability.flush(): Promise<void>`) and/or a server-side
counterpart on the `<vaadin-metrics-collector>` element.
Upstream: https://github.com/vaadin/observability-kit/issues/427

## 14. The session id is only on the request span, and in the clear

**Where it bites:** UC4 / InteractionTraceView.java (following *this user's* interaction)
**Symptom:** with `vaadin.observability.traces-session-id=true`,
`RequestMetricsBinder` puts the HTTP session id on the `vaadin.request` span as
`vaadin.session.id` (`ObservationNames.KEY_SESSION_ID`). Two things fall short.
Only the request span has it: the RPC span and the application's own spans
below it do not, so a trace backend query for a session finds the trail's root
and reaches the rest by trace id. And it is the raw id: the insights payload
reduces it to a short one-way hash unless `insights-details` is on, the span
does not. So a trace backend with this switch on stores the values of live
session cookies.
**Workaround used:** UC4 marks which dispatches came from the reader's own
session and shows only the first characters of the id, since its trail is
readable from every session.
**Suggested API:** the session id on the RPC span too (or on every span of the
trace), and the same short one-way hash the insights use.

## 16. A client error's frame is reported minified — the sourcemaps are on the server

**Where it bites:** UC5 (the stock chart's error, on the hosted demo or any production
build).
**Symptom:** a `client-error` insight names the first stack frame as the browser wrote
it. In a production build that is the minified bundle — `frame` reads
`https://…/VAADIN/build/indexhtml-<hash>.js:1:48213` and `function` a one-letter name —
so the finding groups correctly and says nothing a developer can open. The payload is
meant to be forwarded to an issue tracker or an AI agent, and neither can do anything
with a column on line 1 of a hashed file that the next deploy renames. Nothing in the
kit reads sourcemaps, on either side.
**Why the kit is the place:** the maps and the reports end up in the same process.
A Vite build with `build.sourcemap: 'hidden'` writes a `.map` beside every chunk, the
Flow build packages them with the chunks under `META-INF/VAADIN/webapp/VAADIN/build/`,
and the insight is assembled on that same server — so resolving is a classpath lookup
and a VLQ decode, with no upload step, no external symbolication service, and no
browser that ever needs a map.
**Workaround used:** `com.example.acme.SourceMaps` resolves the retained frame against
the map on the classpath, and UC5's card shows the original file and line
under the kit's minified one, with the line of code quoted from `sourcesContent`. The
broken features moved out of `executeJs` strings into a bundled module
(`src/main/frontend/acme/stock-chart.ts`), because code compiled from a string in the
browser has no map to be resolved against. What the workaround cannot fix is grouping:
the kit groups by the minified frame, so one bug is one finding per build — a redeploy
renames the chunk and starts a new group. Nor does the card name the function: a v3
map's `names` hold the identifier at a position, not the function around it, so the
kit's `function` stays the minified one.
**Suggested API:** resolve `frame` against the build's maps when the insight is
captured, keeping the minified location alongside it — e.g. an `original` object in
the evidence with `source`, `line` and `column` — and group by the resolved
location, so a finding survives a redeploy; the quoted line of code, if any, behind
`insights-details` like the message, since `sourcesContent` is the application's code.
And a documented recommendation for `build.sourcemap: 'hidden'`, which is what makes
maps available without publishing them via a `sourceMappingURL`.

## Test-simulator note

Most client-side gaps are where the repo's browserless tests cannot exercise the JS
collector. Server-side binders *are* testable browserlessly (drive `navigate(...)` +
session/UI lifecycle and assert against a `SimpleMeterRegistry`), and the kit ships
exactly such tests. Gaps that live purely in the browser (#1–#4, #7 on the client, #12) have no
browserless simulator and would need an end-to-end test or a documented manual check.

**A kit-side feature is testable from the seam behind the browser**, which is how UC5 is
covered now that it measures nothing itself. Its tests record into
`vaadin.client.connection.downtime` exactly as the collector would, and capture browser
errors through the kit's own `ClientErrorCollector.capture(...)` into the buffer
`ObservabilityKit.getRecentClientErrors()` publishes — so the assertions run against real
frame parsing and real detail gating, not a hand-built payload. That is the pattern
wherever a client-side feature lands server-side through a seam an application can reach:
pick the seam nearest the browser and drive it with what the browser would send. The
collector's own half — the subscription to `window.Vaadin.connectionState`, the buffering
across an outage, the detail gathered there — is covered by the kit's `ClientProblemsIT`
and its `VaadinMetricsClient.test.js`.

**Watch the buffer's lifecycle in a browserless test.** `RecentClientErrors` is built by
the kit's service-init listener and published as a static, so `cleanVaadinEnvironment()` +
`initVaadinEnvironment()` replaces it and drops what the previous session captured — the
same harness artefact as UC3's gauges (#6). A production service is initialised once, so
the buffer really is application-wide; to exercise that, capture two reports without
re-initialising and assert they group into one finding, rather than trying to hand one
across a simulated session boundary.

**RPC-driven capture is also outside browserless reach.** Anything hooked on Flow's RPC
invocation listener — `vaadin.rpc.duration`, and UC6's interaction insights — is observed
only while handling a real UIDL request. A browserless `test(button).click()` invokes the
component listener directly, bypassing `ServerRpcHandler`, so no invocation is reported
and nothing is captured. UC6's test therefore covers rendering, wiring (the failing
action must let its exception propagate) and lifecycle, while the capture itself needs a
browser. A `SpringBrowserlessTest` hook to drive an invocation through the RPC pipeline
would close this.

**Tracing is the exception**, and it is worth knowing why. Everything driven through the
Observation API from ordinary application code — UC4's `acme.shipment.dispatch`,
`acme.warehouse.reserve` and `acme.carrier.book` spans, and the kit's `vaadin.db.query`
span per JDBC statement, since the `DataSource` proxy does not care how the call
arrived — is created on the calling thread, so a browserless click produces a real,
correctly nested trail. What a browserless test cannot produce is its *top*: the
`vaadin.request` and `vaadin.rpc` spans need a UIDL request, and Spring's HTTP span
needs a servlet one. So `InteractionTraceViewTest` asserts the trail's shape, its trace
id, the failing hop and the ranking for real, and only the framework's two outermost
spans are covered by using the running application (which is also where gap #14 was
measured).

Two specifics for the UI-state binder (#6), learned writing UC3's tests. Its UI-init and
after-navigation hooks do fire browserlessly, so `navigate(...)` after growing a tree is
enough to see `vaadin.ui.state.nodes` move; its RPC hook does not, because a browserless
click invokes the listener directly rather than through an RPC round trip, so the "state
grows on the next interaction" path has no browserless simulator. And the binder is
created by the service init listener while its gauges are registered against the shared
`MeterRegistry`: re-initialising the Vaadin environment inside one Spring context leaves
the first binder's gauges serving a service that no longer has any UIs, reporting zero.
A fresh context per test method (`@DirtiesContext`) is what keeps registry and binder in
step. This is a test-harness artefact — a production service is created once — but it is
invisible until a gauge silently reads zero.
