# Page Visibility API — use cases

A standalone Spring Boot demo of the `Page#pageVisibilitySignal()` API in
Vaadin Flow (resolved from the parent pom's `vaadin.version` property). Each
view exercises a single realistic scenario. Where the API falls short is
recorded in [API-GAPS.md](API-GAPS.md).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Update when active | A live server clock and counter that pause when the tab is hidden or unfocused. Demonstrates that no traffic is sent over the websocket while the user can't see the page. |
| UC2 | Presence | Cross-UI avatar strip; each tab's avatar greys out for everyone else when that tab loses focus or is hidden. |
| UC3 | Notification gating | A 5-second delayed notification is delivered as an in-tab toast when the tab is visible, and as an OS-level web push notification otherwise. Leaving the view cancels it. |
| UC4 | Refresh stale data | A fake USD/EUR rate card auto-refreshes when the tab is visible again after being hidden for 5 seconds or more. Quick alt-tabs are ignored. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl page-visibility spring-boot:run
```

Open <http://localhost:8080/>.

## VAPID keys (UC3 only)

Web Push requires a VAPID key pair. The `application.properties` ships with
the literal marker `__GENERATE__`; on first start an ephemeral pair is
generated, the public key is logged at INFO level, and the private key is
logged only at DEBUG. Copy the printed public key (and the DEBUG-level
private key, if you've enabled it) into `application.properties` to keep
subscriptions across restarts, or generate your own:

```
npx web-push generate-vapid-keys
```

Web Push only works in secure contexts (HTTPS or `localhost`).
