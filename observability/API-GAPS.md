# API gaps — Observability Kit

This module is a *consumer-side* exploration of observability for Vaadin Flow.
The baseline is **Observability Kit 5.0** (`com.vaadin:observability-kit`, the
Micrometer rewrite — `observability-kit-micrometer` + `observability-kit-spring`
+ `observability-kit-starter`), built on **Flow 25.3**, which merged the two SPIs
the kit needs:

- session lock request/acquire/release events ([flow#24498](https://github.com/vaadin/flow/pull/24498)), and
- the RPC invocation listener ([flow#24499](https://github.com/vaadin/flow/pull/24499)).

The kit ships server-side binders (request, RPC, session, session-lock, UI,
navigation, errors), a Micrometer Observation/tracing path, and an in-browser
collector (`VaadinMetricsClient.js`) injected per UI and bridged to the server
through the `<vaadin-metrics-collector>` element's `@ClientCallable recordSamples`.
The Spring Boot starter wires a `MeterRegistry` and enables the instrumentation
out of the box.

The kit is good and covers most of the use cases in
[#277](https://github.com/vaadin/use-cases/issues/277). The gaps below are what
remains *missing or awkward even with the kit in place* — i.e. the things the
demo can't do cleanly, and the Flow / kit API changes they argue for. A reusable
shim, where one is possible, would live in `src/main/java/com/example/MissingAPI.java`.

Meters referenced below use the kit's `MeterNames` constants. The server records
`vaadin.request.duration`, `vaadin.rpc.duration` (tagged `type` / `outcome`),
`vaadin.navigation`, `vaadin.errors`, `vaadin.sessions.*`, `vaadin.session.lock.*`
and `vaadin.ui.*`. The client collector records, into the *same* registry,
`vaadin.client.bootstrap.duration`, `vaadin.client.navigation.duration` (tagged
`route` / `trigger`), `vaadin.client.web_vitals.lcp`, `vaadin.client.web_vitals.fcp`
and the `vaadin.client.errors` counter.

**Reachability is not the problem.** The in-browser collector POSTs its samples
back via the `<vaadin-metrics-collector>` `@ClientCallable`, and `ClientMetricsBinder`
records them into the *same server-side `MeterRegistry`* every other binder uses. So
the client meters above are readable by application code exactly like the server
meters — a Flow view can inject the `MeterRegistry` and read them today. The gaps
below are therefore about **what the client measures, granularity, correlation, and
completeness**, not about getting client data to the server.

> **Note:** the kit once declared a `vaadin.client.rpc.duration` constant that was
> never emitted. It is gone. The browser's per-interaction timers are
> `vaadin.client.request.duration` and `vaadin.client.render.duration` (see gap #2,
> now closed); UC1 reads both.

## 1. No public client-side request-lifecycle hook

**Where it bites:** UC1 (attributing one click end to end); the whole client collector.
**Symptom:** there is no supported way for application/add-on code to learn when a
UIDL request starts and ends in the browser. The `RequestResponseTracker` fires
`RequestStartingEvent` / `ResponseHandlingEndedEvent`, now in TypeScript
([flow#24951](https://github.com/vaadin/flow/pull/24951)), but they are not
published on `window.Vaadin.Flow.clients`. The kit's collector no longer needs the
hook for *timing*: it reads each UIDL `POST`'s Resource Timing entry for the round
trip and Flow's published `getProfilingData()` for the render time (gap #2). What
it still cannot do without the hook is tell *which* request a timing belongs to, or
attribute a request to the click that started it.
**Workaround used:** Resource Timing for the wire, `getProfilingData()` for the render,
the connection store's loading transitions as the "a request ended" signal.
**Suggested API:** publish the tracker's events, as
[flow#7369](https://github.com/vaadin/flow/issues/7369) has asked since 2020:
`window.Vaadin.Flow.addRequestListener({ onRequestStart, onResponseReceived, onRendered })`
with a correlation id, so a click can be followed from the browser to the server.

## 2. Per-interaction client timing — closed

**Where it bit:** UC1 (the browser segment of perceived latency).
**Status: closed by Observability Kit 5.0**
([observability-kit#396](https://github.com/vaadin/observability-kit/pull/396)).
The collector times each UIDL request from the browser's end as
`vaadin.client.request.duration`, read off the Resource Timing entry every UIDL `POST`
leaves behind, and the time Flow's client spent applying the response as
`vaadin.client.render.duration`, read from the profiling data Flow publishes on
`window.Vaadin.Flow.clients[id].getProfilingData()`. Both are tagged by `route`. The
render figure exists only when Flow's `requestTiming` setting is on — the default
outside production mode; a production deployment sets `vaadin.requestTiming=true`, as
this module does. Subtracting `vaadin.request.duration{vaadin_request_type="uidl"}`
from the browser's round trip gives the network's share, which UC1 shows.
**What remains:** the render figure is Flow's processing time, not "click to painted";
and the three segments are aggregates that cannot be stitched per click (gap #3). The
request meter needs the default transport: with `@Push(transport = WEBSOCKET)` the UIDL
rides the websocket and leaves no Resource Timing entry.

## 3. Client samples are aggregated and uncorrelated — no per-interaction value

**Where it bites:** UC1 (browser/network/server breakdown), UC4 (single trace).
**Symptom:** two distinct limitations, both about *granularity / stitching*:
1. **Aggregated on ingest.** `ClientMetricsBinder.ingest` folds each browser sample into
   a rolling `Timer` (`registry.timer(name, tags).record(...)`); the per-sample values the
   JS buffers (each with its own `ts`/`valueMs`) are discarded. So we can read the *mean/
   max/count* of, say, `vaadin.client.navigation.duration`, but never *this navigation's*
   value.
2. **No correlation id.** The client navigation/vitals samples and the server
   `vaadin.request.duration` / `vaadin.rpc.duration` timers share no identifier, so a
   single slow interaction can't be stitched into one client → server picture — only
   aggregates exist. The collector also sends no W3C `traceparent` on the UIDL request
   (`recordSamples` carries only name/tags/value/ts), so the server-side trace
   (`vaadin.request` observation) doesn't descend from a browser-rooted span — the trace
   effectively starts on the server, not at the click.
**Workaround used:** aggregates only; no per-interaction stitch.
**Suggested API:** a per-UIDL-request correlation id exposed to both the client hook
(gap #1) and the server request interceptor, ideally as a W3C `traceparent` the client
injects and the server continues, so browser → server → backend is one trace.

## 4. Push / WebSocket transport is not instrumented on the client

**Where it bites:** UC1, UC2 (apps using `@Push`).
**Symptom:** the client collector hooks the History API, `PerformanceObserver` and
global error events — nothing for the push connection. So server-initiated updates
delivered over WebSocket/long-poll (`@Push`) produce no client-side timing or outcome
samples. For push-heavy apps the client view of responsiveness is blind.
**Workaround used:** none.
**Suggested API:** the client hook (#1) should be transport-agnostic, covering the
push connection, not just navigations.

## 5. No connection-state metric, despite the client API existing

**Where it bites:** UC5 (connection lost / reconnecting), UC2 (the health badge, which
can therefore only report the cadence of its own poll requests, never the browser's
connection state).
**Symptom:** the kit collects client *errors* but not connection-state
transitions. Yet `window.Vaadin.connectionState` already exposes
`online`/`offline`, a `state` (`CONNECTED` / `CONNECTION_LOST` / `RECONNECTING`), and
`addStateChangeListener(...)`. The information is one listener away but is neither
collected by the kit nor surfaced as a meter.
**Workaround used (possible):** an app-level shim that subscribes to
`window.Vaadin.connectionState.addStateChangeListener` and reports transitions to the
server via a `@ClientCallable`, recorded as a `vaadin.client.connection.state` meter —
a candidate for `MissingAPI`.
**Suggested API:** have the collector record connection-state transitions out of the
box (e.g. a `vaadin.client.connection` gauge/counter tagged by state), since the
client store already drives them.

## 6. UI-state size — **resolved in the kit**, with bytes left to the application

**Where it bit:** UC3 (capacity & scaling), UC2 (the health readout counted sessions
and UIs but could not show what each one costs).
**Status: closed.** The kit now measures per-UI state size and publishes the
aggregate, behind an opt-in switch:

```properties
vaadin.observability.ui-state=true
```

`UiStateSampler` walks a UI's state tree, `UiStateMetricsBinder` has every UI report
itself (at UI init, after each navigation, and on RPC completion throttled by
`ui-state-sample-interval`) because a tree may only be read under its own session
lock, and the gauges are `vaadin.ui.state.nodes`, `vaadin.ui.state.nodes.max`,
`vaadin.ui.state.components`, `vaadin.ui.state.views`,
`vaadin.session.state.nodes.max`, `vaadin.session.uis.max` and
`vaadin.ui.state.sample.age.max` — aggregates only, never per-session series, for the
same cardinality reason as #8/#9. UC3 previously carried its own sampler, registry and
`uc3.*` gauges; all of that is deleted and the view now just reads the registry like
every other use case in this module.

Two findings from the original consumer-side implementation stand, and the kit's
version reflects both: the accurate count has to come from
`com.vaadin.flow.internal.StateNode` (`visitNodeTree`) rather than an
`Element.getChildren()` walk, because virtual children are unreachable that way and in
Flow 25 the route target is itself attached as a virtual child — a public walk of a
live UI finds **2 nodes and no view at all** where the state tree finds **109**; and
bytes are not derivable from a tree walk at all.

**What the kit still leaves to the application:**

1. **Bytes are configured, not measured.** `vaadin.ui.state.size` is published only
   once the app sets `vaadin.observability.ui-state-bytes-per-node`. Refusing to guess
   is the right call, but the number then has to be produced out of band — the kit's
   README describes the method (settle the heap, retain a batch of representative
   views, read `MemoryMXBean`) and UC3 implements it as `HeapCostProbe`, reporting
   whether the configured value still holds. Nothing reconciles the two, so a
   configured constant cannot notice that the views got heavier. Worse, *nodes* is a
   lossy unit for this: a component's data is not in the node count, so a `Grid` with a
   hundred rows is a handful of nodes carrying most of the weight, and bytes-per-node
   comes out several times higher for a data-bound view than for a form. One global
   constant can only approximate a mixed application. A per-UI retained-size estimate
   from the kit, or a documented calibration helper, would close this properly.
2. **Aggregates only, so no in-app breakdown.** "Which user is holding all that
   state?" cannot be answered from application code: the binder knows per-UI figures
   but keeps `tracked` private, and rightly does not tag meters per session. A
   read-only per-session accessor would make an in-app breakdown possible without
   touching meter cardinality. UC3 used to render exactly that table and had to drop it.
3. **No way to request a measurement.** Sampling happens on the kit's schedule, so a
   view that wants to show what *this* tab costs right now has to wait for the next
   interaction — and because the RPC hook fires when an invocation *ends*, a click that
   changes the tree is reflected only on the following refresh. UC3's "grow this view's
   state" button demonstrates the lag rather than hiding it.
4. **Off by default, and stale by design.** The feature costs a tree walk per sampled
   interaction, so it is opt-in; and an idle user contributes their state as of their
   last interaction. `vaadin.ui.state.sample.age.max` makes that staleness visible,
   which is the right answer, but consumers have to remember to read it.

## 7. Server request timing is already computed and transported — but unusable

**Where it bites:** UC1, UC2.
**Symptom:** `UidlWriter` already attaches `"timings": [cumulative, last]` to the UIDL
response (from `VaadinSession#getLastRequestDuration()`), and the client `MessageHandler`
receives it — but only `Console.debug`-logs it into a private field, and the whole thing
is gated by `isRequestTiming()`, which defaults to `!productionMode` (**off in
production**). So the server's own measurement of each request, already shipped to the
browser, is reachable by neither application server code (cleanly) nor client code, and
the kit re-measures instead of reusing it.
**Workaround used:** the kit's `RequestMetricsBinder` measures server time independently;
the client never sees a server duration.
**Suggested API:** a production-safe, public accessor for per-request server duration on
both sides (e.g. include it in the client hook's response event), so collectors need not
re-measure or parse debug logs.

## 8. Interaction attribution — now on insights, still absent from metrics

**Where it bites:** UC1 (which action is slow?), UC6 (errors by view/action).
**Status: largely addressed by the kit.** Flow's RPC invocation listener
([flow#24499](https://github.com/vaadin/flow/pull/24499)) lets the kit's `RpcMetricsBinder`
time *individual* RPC invocations as `vaadin.rpc.duration`, tagged by `type` and
`outcome` (`success`/`error`). The kit now also records the invocation *name* (the DOM
event, e.g. `click`) and the *targeted component class* — as high-cardinality span
attributes, and as fields of the interaction insights it captures for failed and
over-budget interactions. UC6 reads those insights: each names the route, the component,
the event and the first non-framework stack frame, so "the `click` on `Button` throws
`IllegalStateException` at `FailureInsightsView.java:114`" is a groupable finding rather
than something to reconstruct from a log.
**What still bites:** the attribution lives on **spans and insights, not on meter tags**.
`vaadin.rpc.duration` is still tagged only by `type` / `outcome`, and `vaadin.errors`
only by exception type — deliberately, to bound cardinality. So a Prometheus/Grafana
dashboard still cannot group latency or errors by component or view; only the in-process
insights (or a tracing backend) carry that. A *business* action name ("save order", as
opposed to the `click` that carried it) also remains the application's own to record.
**Workaround used:** UC1 keeps its own `acme.invoice.action{action=…}` timer for
per-action metric granularity; UC6 uses the insights for per-component attribution.
**Suggested API:** an opt-in, cardinality-bounded resolver the application supplies (e.g.
route plus a logical action name) that the kit may apply as *meter* tags, so dashboards
can group by view/action without unbounded cardinality.

## 9. Insights are consumable in-process only as an untyped JSON map

**Where it bites:** UC6.
**Symptom:** the kit's interaction insights are shaped for the Actuator endpoint:
`InsightsService.payload()` returns a `Map<String, Object>` of nested maps and lists. An
application that wants to render insights in its own UI — as UC6 does, rather than have
the app call its own HTTP endpoint — has to cast its way through that map
(`(List<Map<String, Object>>) payload.get("insights")`, then
`(Map<String, Object>) insight.get("evidence")`), with unchecked casts, string keys and
no compile-time contract. The JSON shape is a good published contract for *agents*; it is
a poor one for Java callers.
**Workaround used:** UC6 flattens the map into a view-local `Row` record, with
`@SuppressWarnings("unchecked")`.
**Suggested API:** typed insight objects (e.g. `List<Insight>` exposing id, severity,
summary, evidence and examples) alongside the JSON rendering, so in-app consumers get a
compile-checked contract and JSON stays a serialization concern.

## 10. Metric tags cannot express which component or view an interaction touched

**Where it bites:** UC7 (the external dashboard), UC1, UC6.
**Symptom:** the kit deliberately keeps high-cardinality attribution off meter
tags: `vaadin.rpc.duration` carries `type` and `outcome`, `vaadin.errors` carries the
exception type, and the invocation name and target component live only on spans (and,
where a build has them, on interaction insights). That is the right call for
cardinality, but it means an external dashboard can group latency and errors by RPC
type, route or exception and *never* by component or view. "Which button is slow" is
answerable from inside the app and in a tracing backend, but not in Grafana — which is
where an operations team looks first.
**Workaround used:** UC7's dashboard groups RPC latency by `type`; per-component
questions are left to the in-app views.
**Suggested API:** the cardinality-bounded resolver suggested in gap #8, applied to
*meter* tags — e.g. an application-supplied mapping from component/event to a small set
of logical action names — so a dashboard can group by action without unbounded series
growth.

## 11. Percentiles are silently unavailable unless buckets are enabled per timer

**Where it bites:** UC7.
**Symptom:** the kit's timers publish count/sum/max, but `histogram_quantile` needs
`_bucket` series, which only appear when
`management.metrics.distribution.percentiles-histogram.<meter>` is set for each meter.
A p95 panel built on a kit timer therefore renders empty with no error anywhere — the
metric exists, the buckets do not. Nothing in the kit or its documentation surfaces
this, and the meter names have to be repeated in application configuration.
**Workaround used:** UC7 enables buckets for `vaadin.request.duration`,
`vaadin.rpc.duration` and `vaadin.session.lock.wait`, and its readout explicitly checks
whether the bucket series exist.
**Suggested API:** a kit-level switch (e.g. `vaadin.observability.percentiles=true`)
that turns on histogram buckets for the kit's own latency meters, so percentile
dashboards work without the application naming each meter.

## 12. No public way to flush the in-browser collector

**Where it bites:** UC2 (the "flush client metrics now" button); any view that wants to
show client-collected numbers on demand.
**Symptom:** `VaadinMetricsClient.js` buffers its samples and POSTs them on a ~5 s timer,
so a view that reads `vaadin.client.*` right after load shows "no samples yet" for
several seconds. The collector *can* be drained immediately — it exposes
`window.__vaadinMicrometer.flush()` — but only as an internal: the kit's own source
comments it `// Expose for tests / dashboards (debug only)`, and the `__` prefix says the
same. There is no supported API, on either side, to say "send what you have now".
**Workaround used:** UC2 calls `window.__vaadinMicrometer.flush()` from `executeJs`,
guarded so it degrades to a no-op if the internal goes away. It works, and it is exactly
the kind of dependency that should not be necessary.
**Suggested API:** a public flush on the client collector (e.g.
`window.Vaadin.observability.flush(): Promise<void>`) and/or a server-side counterpart on
the `<vaadin-metrics-collector>` element, so application code can request a drain without
reaching for a debug hook.

## 13. Database meters are cumulative — no per-interaction attribution

**Where it bites:** UC2 (the N+1 join-table demo).
**Symptom:** with `vaadin.observability.database=true` the kit records every JDBC
result-set fetch into the `vaadin.db.fetch.rows` `DistributionSummary` (and a
`vaadin.db.query` span per query, nested under the request span). The summary is a
rolling aggregate over the whole application, tagged by route but not by interaction, so
there is no way to ask "how many fetches did *this click* cost" — which is the question
an N+1 is diagnosed by. The spans carry that attribution, but only in the tracing
backend; nothing on the meter side exposes it to application code.
**Workaround used:** UC2 *brackets* the meter — it reads the fetch count and row total
immediately before and after `loadCatalog()` and reports the delta (see
`ApplicationHealthView#loadCatalog`). That is correct for a single-threaded click handler
on one server, and quietly wrong under concurrent traffic, since another request's
fetches land in the same summary in between.
**Suggested API:** per-interaction DB attribution readable from application code — e.g. a
scoped accessor for the fetches recorded during the current RPC invocation (the
correlation id from gap #3 would carry it), or a per-request `vaadin.db.fetch.count`
exposed alongside `vaadin.rpc.duration`.

## Test-simulator note

Most client-side gaps are where the repo's browserless tests cannot exercise the JS
collector. Server-side binders *are* testable browserlessly (drive `navigate(...)` +
session/UI lifecycle and assert against a `SimpleMeterRegistry`), and the kit ships
exactly such tests. Gaps that live purely in the browser (#1–#5, #7-client, #11) have no
browserless simulator and would need an end-to-end test or a documented manual check.

**RPC-driven capture is also outside browserless reach.** Anything hooked on Flow's RPC
invocation listener — `vaadin.rpc.duration`, and UC6's interaction insights — is observed
only while handling a real UIDL request. A browserless `test(button).click()` invokes the
component listener directly, bypassing `ServerRpcHandler`, so no invocation is reported
and nothing is captured. UC6's test therefore covers rendering, wiring (the failing
action must let its exception propagate) and lifecycle, while the capture itself needs a
browser. A `SpringBrowserlessTest` hook to drive an invocation through the RPC pipeline
would close this.

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
