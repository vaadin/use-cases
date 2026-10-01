# Observability — use cases

A standalone Spring Boot demo module for observability use cases (request
tracing, metrics, structured logging of UI interactions, …). Each concrete use
case is a sibling `ucN` view, mirroring the layout of the other modules in this
repository, and the `HomeView` lists them via the auto-generated menu.

Use cases are chapters of one fictional application — the back office of
**Acme Supply Co.**, a wholesale hardware supplier. Each view opens with a
window showing the Acme screen its story happens on, and its route is named
after that screen (`orders`, not `uc8`), because the kit tags meters by the
primary route template and the telemetry should read like a real
application's. The numbered route stays available as a `@RouteAlias`, so
`/uc8` keeps working without ever appearing in the telemetry. The shared
building blocks live in `com.example.acme`: `AppWindow` (the window chrome),
`DemoRig` (the knobs that fake the story's problem, visibly not part of the
app), `Investigation` (the "What Observability Kit sees" readout — hidden
until the view reveals it, collapsible steps, refresh scheduling instead of a
refresh button), `InsightCard` and `MeterTable` (the readout's building
blocks), `Telemetry` and `Insights` (formatting and payload helpers), and
`AcmeCatalog` (the product catalog).

| # | View | What it shows |
| - | ---- | ------------- |
| — | Home | Landing page and auto-generated index of the use cases. |
| 1 | Finding which action is slow and where its time goes | "Working an invoice feels sluggish — which step, and where does the time go?" The view opens with a window showing Acme's invoicing desk with three actions of different server cost: saving a draft (nothing), applying discounts (a pricing lookup), issuing the invoice (the tax service, its latency in the demo rig, above the kit's UX budget by default). The first action reveals the investigation, live via a short poll so the browser-collected samples appear on their own: **2)** what the framework times — the three segments of a click: the server's `vaadin.request.duration` and `vaadin.rpc.duration`, the browser's own round trip `vaadin.client.request.duration` (the difference being the network's share, which the view works out) and the time the browser spent applying the response, `vaadin.client.render.duration`, plus the browser's navigation and paint signals — all tagged by type, outcome or route only, so they say *something* took over a second, not which button; **3)** the kit's verdict — the insights endpoint's `slow-user-interaction` findings for `route=invoices`, naming the component and the event with the timing against the budget; **4)** per action — the business-level timer the application records itself (`acme.invoice.action{action=…}`), because a business action name would make meter tags unbounded and is the application's to record. See [`API-GAPS.md`](API-GAPS.md) #8. |
| 2 | Finding the query behind an app-wide hiccup | "Every morning someone opens the catalog page and the whole app hiccups — is the app healthy, and what is it doing?" The view opens with a window showing Acme's inventory page; refreshing the catalog runs the classic N+1 join-table fetch (eager, unbatched `Product.category`). The first load reveals the investigation, which is live and refreshes every 2 s: **2)** the vital signs look fine — sessions, UIs, heap, server timings, the browser's own load and paint signals, error counters, all read from the application's `MeterRegistry`, plus the connection badge derived from the poll cadence (the server cannot see the browser's connection state, see [`API-GAPS.md`](API-GAPS.md)); **3)** the database gives it away — the kit's `vaadin.db.fetch.rows` summary (`vaadin.observability.database=true`), scoped to `route=inventory` and bracketed around the load, shows N+1 result-set fetches for N products; **4)** the fix, verified — a demo-rig switch join-fetches the categories with the products, and the load history shows the fetch count drop to one. The "flush client metrics" control also lives in the rig. |
| 3 | Knowing when to add another server | How much state the server is holding for live users, and which signals actually predict needing another instance. Reads the kit's counts (`vaadin.sessions.active`, `vaadin.ui.active`, session creation rate and lifetime, session-lock contention) together with its UI-state gauges (`vaadin.ui.state.nodes`, `.nodes.max`, `.components`, `.views`, `vaadin.session.state.nodes.max`, `vaadin.session.uis.max`, `vaadin.ui.state.sample.age.max`), which the kit publishes once `vaadin.observability.ui-state=true` — this used to be [`API-GAPS.md`](API-GAPS.md) #6 and the view had to measure it itself. What remains local is the byte conversion: the kit counts nodes and will not guess what one weighs, so a probe measures it and the view reports whether the configured `ui-state-bytes-per-node` still holds. |
| 4 | Finding the cause of a slow interaction | "Dispatching a shipment is sometimes instant and sometimes takes a second, and when it fails the desk only says the carrier refused — and the request returns successfully either way." The view opens with a window showing Acme's shipping desk: *Dispatch shipment* reserves the stock for each line (one counting query per line, so the datastore is on the trail), books a carrier through a backend service (its latency and its refusals in the demo rig), and prints a label. The carrier's default latency is deliberately *under* the kit's 1 s UX budget, so no threshold is crossed and nothing is retained as a slow interaction — the case only a trail explains. The first dispatch reveals the investigation, live via a short poll because the request's own span cannot be in the response it wraps: **2)** the trail — every span of that one interaction nested by parent on a shared timeline (the kit's `vaadin.request` and `vaadin.rpc`, the desk's own `acme.shipment.dispatch`, `acme.warehouse.reserve` and `acme.carrier.book`, and one `vaadin.db.query` per statement), under one trace id, which is also the `traceId` its log lines carry, plus the dispatches followed so far from every session, each marked as this session's or another's by the `vaadin.session.id` the kit puts on its request span (`vaadin.observability.traces-session-id=true`, masked on the page because it is the session cookie's value); **3)** where the time went and where it failed — the spans ranked by the time each spent *on its own* rather than waiting for something it called, next to what `vaadin.errors` and `vaadin.rpc.duration` made of the same interaction, which is that it succeeded; **4)** where the trail begins and ends — it starts on the server rather than at the click ([`API-GAPS.md`](API-GAPS.md) #3), carries the session on its root span only ([#14](API-GAPS.md), closed by the kit), and leaves the application to name its own hops. What makes any of it exist is a tracing bridge in `pom.xml`: the kit already drives its instrumentation through Micrometer's Observation API, and the bridge is what turns those observations into spans. `InteractionTrail` is a `SpanReporter` — the same SPI Zipkin, Tempo and the OTLP exporters implement — pointed at the page instead of out of the process. |
| 5 | Catching lost connections and browser errors | "The pickers say the app freezes on the floor, and that the stock chart never comes up — where do I even look?" The view opens with a window showing the picking screen Acme's warehouse crew works from on tablets: confirming a pick works, *Show stock levels* throws in the browser and *Sync stock from the API* leaves a promise rejected, and the demo rig takes the connection away the way the dead zone by the loading dock does. The first freeze or failed script reveals the investigation: **2)** the server-side suspects see nothing — a browser error never reaches `vaadin.errors`, and an unreachable tablet is a session that just goes quiet, with `vaadin.resync` (the messages a client re-sent and the state rebuilds it asked for, which Flow handles internally) the only trace; **3)** the kit's verdict — the `client-error` insights, *not* filtered to this route, since an error on any screen is one nobody is watching and the route is part of the grouping key; **4)** the raw client meters fleet-wide; **5)** what the numbers still cannot tell you. The problems that never reach a server log: a browser losing the connection and getting it back, and a script failing in a tab nobody is watching. The connection half is the kit's — its in-browser collector subscribes to Flow's `window.Vaadin.connectionState` and records `vaadin.client.connection` per transition and `vaadin.client.connection.downtime` for the time spent unreachable, so this view only reads them. What it makes visible is what those tags mean: downtime is tagged *per state*, because Flow enters `reconnecting` on the first failed request and `connection-lost` only after giving up retrying, so a short outage never leaves `reconnecting` and the whole outage is the two summed — which the readout does. Alongside them, `vaadin.resync` (the server side of a lost message, which Flow handles internally) and `vaadin.client.throttled`, which matters because one outage flushes as one batch. The errors are the kit's too: `vaadin.client.errors` only counts — a message on a tag would be one time series per message — so what identifies one is retained as a `client-error` *insight*, and this view reads those out of the endpoint payload UC6 renders in full. The kit parses the location out of the stack line and keeps it only when it is actually a location, groups by route, kind, source and frame with an occurrence count, gates the message and the function name behind `insights-details`, and reports `maxBufferedMs` — the offline time a report waited before it could be delivered. UC5 previously carried a shim for each half; both are deleted ([`API-GAPS.md`](API-GAPS.md) #5). Deliberately does not poll — a poll is a UIDL request, and one that gets through ends the outage as far as the browser is concerned — and since nothing signals that client samples have arrived, the readout is recomputed from the interactions themselves, plus a `@ClientCallable` the simulated recovery calls once the browser has the server back. |
| 6 | Tracing a failed action to the line of code | "Some returns blow up on the clerks — how do I get from 'something went wrong' to the line of code?" The view opens with a window showing Acme's returns desk, whose *Process return* handler fails for defective items (a missing inspection template), fails validation for a blank order number, and hangs on bank-transfer refunds (a slow lookup, its latency in the demo rig). The first bad return reveals the investigation: **2)** the error counter `vaadin.errors` knows *that* something failed and which exception, even the route and component class — not which handler, event or line; **3)** the kit's verdict — the insights endpoint's `user-interaction-error` and `slow-user-interaction` findings for this route, each naming the component and the event — a failure also the first application stack frame, a slow interaction its timing against the budget — with repeats grouped into one finding with an occurrence count; **4)** the whole payload of `GET /actuator/vaadin/observability` (this route's findings among every other route's), the contract an AI coding agent reads to jump to the offending line. The handler lets its exception propagate (the kit records a failure only when the invocation actually fails) and a session error handler shows the vague notification a clerk would see. See [`API-GAPS.md`](API-GAPS.md). |
| 7 | Getting the metrics into Prometheus and Grafana | The same meters followed *outward*: exported at `/actuator/prometheus`, scraped by Prometheus, charted by Grafana. Checks each hop separately (exported series, scrape target health, the dashboard's own PromQL) so an empty panel can be told apart from a metric that was never exported. `compose.yaml` runs the stack locally. |
| 8 | Finding why a lazy list is slow | "The product search is slow — how do I find out why?" The view opens with just the story: a window showing Acme's order desk (a lazy product `ComboBox` over the catalog, with the simulated backend latency as a demo rig attached to the window) and the instruction to take an order. The first catalog search reveals the investigation below, at the moment the wait has just been felt, and the readout keeps updating as the order grows — no refresh button: **2)** the interaction timers look innocent, because the data provider queries run *after* the RPC invocation that triggered them has returned; **3)** the kit's verdict — the insights endpoint's `slow-data-query` findings, grouped by (route, component, kind), which is what pinpoints the culprit in an application with a hundred views and a thousand lazy components; **4)** the raw meters as fleet-wide aggregates (`vaadin.data.count/fetch.duration` split by `filtered`, `vaadin.data.fetch.requested/rows` scoped to `route=orders`), the same numbers UC7's dashboard charts. Both tables are plain HTML tables, not `Grid`s, because the kit instruments in-memory data providers too and a `Grid` would record on this route while displaying it. |

## Run

```
mvn spring-boot:run -pl :observability-use-cases
```

Open <http://localhost:8080/>.

To also log UC2's N+1 as SQL on the console, activate the `sql-log` profile:

```
mvn spring-boot:run -pl :observability-use-cases -Dspring-boot.run.profiles=sql-log
```

The kit's insights endpoint is exposed alongside the views:

```
curl -s http://localhost:8080/actuator/vaadin/observability | jq
```

The kit withholds the session id, the exception message and the stack frames
unless `vaadin.observability.insights-details=true`, since that payload is meant
to be forwarded — into issue trackers, AI agents and log pipelines. This module
enables it so UC6 shows a complete insight; with it off the session id is a
short hash and the payload states that the message was withheld rather than
absent. A production application should leave it off until it has reviewed what
those fields can contain.

## Tracing (UC4)

The kit drives its request, RPC, navigation, data-query and JDBC instrumentation
through Micrometer's Observation API, but an observation is not a span until
something bridges the two. Nothing on the kit's own classpath does, so this
module adds the bridge — `spring-boot-micrometer-tracing-brave` for Boot's
auto-configuration and `micrometer-tracing-bridge-brave` for the implementation
it needs — and with it every interaction in every use case gets a trace id.
Without it the observations still run and produce no spans, silently, which is
worth knowing before concluding that tracing is not working.

Two properties matter. `management.tracing.sampling.probability=1.0` keeps every
interaction, because Boot's default of 0.1 would drop nine dispatches in ten and
a demo that loses the click you just made teaches the wrong lesson; a busy
deployment keeps a fraction instead. And `logging.pattern.correlation` reads the
trace and span id Brave puts in the SLF4J MDC into every log line, so the trail
UC4 draws and the log lines written inside it are joined by the same id:

```
2026-09-15T12:33:47.279+03:00 INFO [acme-supply,6aa910faa8c9dfac…,b35d9433a5…] …
```

UC4's collector, `InteractionTrail`, is a `SpanReporter` — the same interface
Zipkin, Tempo and the OTLP exporters implement — pointed at the page rather than
out of the process, so no tracing backend has to run for the use case to work.
Swapping it for a real exporter sends the same spans to a real trace UI, which
is also the only way to see the parts of a trail that live outside this process.

## Monitoring stack (UC7)

UC7 ships the module's metrics to the standard OSS stack. Start the app, then
from this directory:

```
docker compose up -d
```

- Prometheus <http://localhost:9090> — scrapes `/actuator/prometheus`
- Grafana <http://localhost:3000> — anonymous admin, dashboard provisioned

The dashboard's bottom rows chart the kit's UI-state gauges next to its
counts, which is where the difference shows: state climbing while the session
count is flat means capacity is going to what users have open, not to how many
of them there are. `vaadin_ui_state_size_bytes` exists only because this module
configures `vaadin.observability.ui-state-bytes-per-node`, and the last panel
tracks `vaadin.ui.state.sample.age.max` — how stale the oldest per-UI
measurement in the aggregate is, since a UI is measured on its own session's
thread.

Prometheus scrapes `host.docker.internal` on ports 8080 and 8082
(`prometheus/local.yaml`), so it finds the app on either; the unused one shows
as a down target. Stop it with `docker compose down`.

### Hosted

The hosted demo runs the same two services as sibling Fly apps in the same
organisation as `observability-cases`, so the three talk over Fly's private
network:

| App | Directory | Public | Reads |
|---|---|---|---|
| `observability-cases-prometheus` | `prometheus/` | <https://observability-cases-prometheus.fly.dev> | `observability-cases.internal:8080` by DNS service discovery (`prometheus/fly.yaml`) |
| `observability-cases-grafana` | `grafana/` | <https://observability-cases-grafana.fly.dev> | `observability-cases-prometheus.internal:9090` |

Both are built from the small Dockerfiles in those directories, from the
repository root like the module itself, and share the Grafana provisioning and
dashboard with the local stack; only the datasource URL differs, via the
`PROMETHEUS_URL` environment variable. The Fly Deploy workflow picks any
directory holding a `fly.toml` up to one level below a module, so the two
deploy like any other module and redeploy when their directory changes.
The apps have to exist once before the first deploy:

```
fly apps create observability-cases-prometheus
fly apps create observability-cases-grafana
```

Both are public and read-only: Grafana grants anonymous viewers with the login
form and basic auth off, so the default admin account is unreachable and the
dashboard is edited here and redeployed rather than in the UI; Prometheus runs
with its admin and lifecycle APIs off (its defaults). Prometheus keeps two days
of samples on the machine's ephemeral disk, so a redeploy starts it empty.

The UC7 view learns where the stack is from three properties, defaulting to the
compose services: `uc7.prometheus.url` and `uc7.grafana.url` are what the
browser follows, `uc7.prometheus.api-url` is what the server calls for the
scrape-target and query rows. `fly.toml` sets the first two to the public
hostnames and the third to the private one.

Why sibling apps rather than [Fly's multi-container
Machines](https://fly.io/docs/machines/guides-examples/multi-container-machines/),
which can run a Compose file in one Machine: that mode drops `volumes:`, which
is how this compose file feeds Prometheus its scrape config and Grafana its
provisioning, allows exactly one service to `build:` (the app), so the other
two would need pre-built config-bearing images anyway, and routes the Fly proxy
to a single container, so visitors could never open Grafana or Prometheus.

## Error reporting (Sentry)

The module can forward its errors to [Sentry](https://sentry.io). It is off
until a DSN is configured, so local runs and CI send nothing:

```
SENTRY_DSN=https://…@….ingest.sentry.io/… mvn spring-boot:run -pl :observability-use-cases
```

The hosted demo reads it from a Fly secret:

```
fly secrets set SENTRY_DSN=https://… -a observability-cases
```

What gets reported is every ERROR log line, through Sentry's Logback appender,
with the WARN and INFO lines before it as breadcrumbs. That is deliberate: a
failing Vaadin interaction still answers its RPC with 200, so Spring MVC's
exception handling (which the starter also hooks into) never sees it, but
Vaadin's default error handler logs it at ERROR. UC6's broken returns therefore
show up in Sentry as `IllegalStateException` and `IllegalArgumentException`
issues with their stack traces. `sentry.send-default-pii` stays off, because this module turns on
`insights-details` and `traces-session-id` and Sentry is a third party. Metrics
stay with Prometheus and Grafana (UC7), since Sentry does not ingest Micrometer
meters, and traces stay in-process (UC4) until the tracing bridge is switched
from Brave to OpenTelemetry, which is what Sentry's tracing integrates with.
