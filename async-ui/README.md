# Async UI & Performance — use cases

A standalone Spring Boot demo of keeping a Vaadin Flow UI fast while the work behind it is slow: loading data page by page, answering in the background through server push, building only what the user looks at, and sending the browser no more than it needs. Each view exercises a single realistic scenario; `API-GAPS.md` records what Flow does not cover yet.

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Browse a million rows | A lazy Grid over a million generated orders, sorted and filtered in the backend, working from an item count estimate instead of an exact count. |
| UC2 | Parallel dashboard | Four widgets whose slow queries start together; each card fills in through push when its own answer arrives, and a failing card offers a retry. |
| UC3 | Job with progress | A batch import running in the background with a progress bar and Cancel; leaving the view cancels it too. |
| UC4 | Detail that streams in | A detail route that renders at once and loads its record afterwards; switching orders drops the older answer, and unknown ids become a not-found message. |
| UC5 | Latest response wins | Search-as-you-type where a slower, older answer must not overwrite a newer one, with the naive behaviour one click away. |
| UC6 | Build when needed | Tabs, a `Details` and a below-the-fold heatmap whose content is built the first time it is needed. |
| UC7 | Throttle a live feed | A 100-ticks-per-second price feed pushed tick by tick or batched every 250 ms, with counters for the difference. |
| UC8 | Optimistic save | Checkboxes that change at once and roll back with a notification when the background save fails. |
| UC9 | Slow request feedback | Disable-on-click against double submits, the loading indicator's delay, and lazy versus eager value change mode. |
| UC10 | 100,000 cards | A `VirtualList` with a `LitRenderer` next to 2,000 cards built as server-side components. |
| UC11 | Grid search loading | A slow Grid search that shows a spinner, a progress bar and dimmed previous results while it runs, skeleton rows on the first load, and its own states for no matches and failure. |

All slow backend calls go through `com.example.backend.SimulatedLatency`. The tests replace it with `ManualLatency`, so each test decides when, and in what order, the "slow" answers arrive.

## Run

```
cd async-ui
mvn spring-boot:run
```

Open <http://localhost:8080/>.
