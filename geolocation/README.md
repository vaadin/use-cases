# Geolocation API — use cases

A standalone Spring Boot demo of the Geolocation API in Vaadin Flow
(resolved from the parent pom's `vaadin.version` property). It builds on the
static `Geolocation` class: `Geolocation.getPosition(...)` reads the location
once, `Geolocation.watchPosition(owner)` returns a `GeolocationWatcher` that
keeps reporting new positions through a listener or a signal, and
`Geolocation.availabilityHintSignal()` tells whether location can be used and
what the permission state is. Each view shows one realistic scenario. Where
the API falls short is recorded in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Locate on click | A "Use my location" button reads the position once and drops a marker on a map. |
| UC2 | Live tracking | A Start/Stop toggle drives a `GeolocationWatcher`; signal bindings show the status, and an effect adds each reading to a history grid and a path on the map. |
| UC3 | Locate returning visitors | Fetches the location when the view opens, but only if permission was granted before; otherwise it waits for a click. The permission hint follows changes while the view is open. |
| UC4 | When location fails | Three cards: how the page renders for each availability, the message for each request error (both simulated), and a real request with the live availability. |
| UC5 | Detailed position data | Shows every field the browser reports (latitude, longitude, accuracy, altitude, heading, speed, timestamp); missing values show as "—". |
| UC6 | Precision versus battery | Four `GeolocationOptions` profiles run the same request, so you can compare response time and accuracy. |
| UC7 | Location in a form | A pothole report form: "Pin my location" stores the reading in a signal, and submit stays disabled until there is a description and a pin of 50 m accuracy or better. |
| UC8 | Route in a database | Like UC2, but each reading is saved with Spring Data JPA first; the grid and the map line are read back from the database, so the route is still there when you come back to the view. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl geolocation spring-boot:run
```

Open <http://localhost:8080/>. Browsers only allow the Geolocation API in a
secure context: HTTPS, or `localhost`.

## Browser notes

- The browser shows its own permission dialog on the first request; Flow
  can not style or suppress it.
- Safari does not report the permission state: `availabilityHintSignal()`
  shows `UNKNOWN` for granted, denied and prompt. Only `UNSUPPORTED` is
  reliable there.
- Firefox does not always pass changes made in the browser settings back to
  the page: the availability can be out of date until the next
  `getPosition` or `watchPosition` call.
