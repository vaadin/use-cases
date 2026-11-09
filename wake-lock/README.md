# Screen Wake Lock API — use cases

A standalone Spring Boot demo of Flow's `WakeLock` API
(`com.vaadin.flow.component.wakelock.WakeLock`), which keeps the screen from
dimming while the user follows along. `WakeLock.request()` asks the browser
for the lock, `WakeLock.release()` drops it, and `WakeLock.activeSignal()`
shows whether the browser holds it right now. Each view shows one realistic
scenario. Where the API falls short is recorded in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Keep-awake toggle | A button that requests and releases the lock, with a badge that shows what the browser reports. The button is disabled where the API is not available, and leaving the page releases the lock. |
| UC2 | Step-by-step recipe | The lock is requested when the recipe opens and released when you leave, with nothing to switch on or off. |
| UC3 | Presentation slideshow | The lock is held only while a presentation runs: Start requests it; Stop, Next on the last slide or leaving the page releases it. |
| UC4 | Workout timer | A 30 s work / 10 s rest interval timer that holds the lock while it runs and releases it on pause, reset or leaving the page. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl wake-lock spring-boot:run
```

Open <http://localhost:8080/>.

Browsers grant the lock only to a page served over HTTPS or from
`localhost`, and only while the tab is visible.
