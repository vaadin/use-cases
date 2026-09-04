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
| 1 | Interaction latency | Where an interaction's time goes: server request handling, the per-RPC server invocation (`vaadin.rpc.duration`), a per-action timer, and the browser's page-load signals (navigation timing, web vitals) — all read from the app's `MeterRegistry`. See [`API-GAPS.md`](API-GAPS.md). |
| 2 | Application health | A live readout of the app's own signals (sessions, UIs, memory, timings, connection), plus a database-health demo: a button that loads a product catalog and surfaces the classic N+1 join-table fetch — N products cost N+1 single-row fetches — through the Observability Kit's own `vaadin.db.fetch.rows` meter (`vaadin.observability.database=true`). Adding `@BatchSize` to `Product.category` collapses it, exactly as in the bookstore-example. See [`API-GAPS.md`](API-GAPS.md). |
| 3 | Capacity & scaling | How much state the server is holding for live users, and which signals actually predict needing another instance. Reads the kit's counts (`vaadin.sessions.active`, `vaadin.ui.active`, session creation rate and lifetime, session-lock contention) together with its UI-state gauges (`vaadin.ui.state.nodes`, `.nodes.max`, `.components`, `.views`, `vaadin.session.state.nodes.max`, `vaadin.session.uis.max`, `vaadin.ui.state.sample.age.max`), which the kit publishes once `vaadin.observability.ui-state=true` — this used to be [`API-GAPS.md`](API-GAPS.md) #6 and the view had to measure it itself. What remains local is the byte conversion: the kit counts nodes and will not guess what one weighs, so a probe measures it and the view reports whether the configured `ui-state-bytes-per-node` still holds. |
| 6 | Failure insights | "Some returns blow up on the clerks — how do I get from 'something went wrong' to the line of code?" The view opens with a window showing Acme's returns desk, whose *Process return* handler fails for defective items (a missing inspection template), fails validation for a blank order number, and hangs on bank-transfer refunds (a slow lookup, its latency in the demo rig). The first bad return reveals the investigation: **2)** the error counter `vaadin.errors` knows *that* something failed and which exception, even the route and component class — not which handler, event or line; **3)** the kit's verdict — the insights endpoint's `user-interaction-error` and `slow-user-interaction` findings for this route, each naming the component, the event and the first application stack frame, with repeats grouped into one finding with an occurrence count; **4)** the same findings as the JSON payload of `GET /actuator/vaadin/observability`, the contract an AI coding agent reads to jump to the offending line. The handler lets its exception propagate (the kit records a failure only when the invocation actually fails) and a session error handler shows the vague notification a clerk would see. See [`API-GAPS.md`](API-GAPS.md). |
| 7 | Monitoring stack | The same meters followed *outward*: exported at `/actuator/prometheus`, scraped by Prometheus, charted by Grafana. Checks each hop separately (exported series, scrape target health, the dashboard's own PromQL) so an empty panel can be told apart from a metric that was never exported. `compose.yaml` runs the stack locally. |
| 8 | Slow product search | "The product search is slow — how do I find out why?" The view opens with just the story: a window showing Acme's order desk (a lazy product `ComboBox` over the catalog, with the simulated backend latency as a demo rig attached to the window) and the instruction to take an order. The first catalog search reveals the investigation below, at the moment the wait has just been felt, and the readout keeps updating as the order grows — no refresh button: **2)** the interaction timers look innocent, because the data provider queries run *after* the RPC invocation that triggered them has returned; **3)** the kit's verdict — the insights endpoint's `slow-data-query` findings, grouped by (route, component, kind), which is what pinpoints the culprit in an application with a hundred views and a thousand lazy components; **4)** the raw meters as fleet-wide aggregates (`vaadin.data.count/fetch.duration` split by `filtered`, `vaadin.data.fetch.requested/rows` scoped to `route=orders`), the same numbers UC7's dashboard charts. Both tables are plain HTML tables, not `Grid`s, because the kit instruments in-memory data providers too and a `Grid` would record on this route while displaying it. |

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

Prometheus scrapes `host.docker.internal` on ports 8080 and 8082, so it finds
the app on either; the unused one shows as a down target. Stop it with
`docker compose down`.

This stack is developer tooling: the module deploys as a single container, so the
hosted demo runs without it and UC7 degrades to its export column.
