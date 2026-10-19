# Web Notifications API — use cases

A standalone Spring Boot demo of native OS notifications shown by a Vaadin Flow
app while its tab is open, using the browser's
[Notifications API](https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API).
Every view uses Flow's `WebNotification` API
(`com.vaadin.flow.component.webnotification`): it asks for permission inside the
click, exposes the permission as a signal and reports notification clicks back
to the server.

The API is not in the mainline snapshot yet, so this module builds against the
`25.4.web-notification-api-SNAPSHOT` Flow build from
[vaadin/flow#26495](https://github.com/vaadin/flow/pull/26495).

| # | View | What it shows |
| - | ---- | ------------- |
| UC1 | Ask for permission | Asks for permission only when the user clicks, and shows the permission (prompt, granted, denied, unsupported) live, including changes made later in the site settings. |
| UC2 | Task finished | A ten-second server job; when it finishes, the user gets an OS notification, or an in-app one without permission. The click that starts the job also asks for permission. |
| UC3 | Click to open | A ticket assigned after five seconds raises a notification; clicking it brings the tab back and opens the ticket on the server. |
| UC4 | Collapse repeated alerts | With a `tag`, repeated messages replace one "N new messages" notification instead of stacking; untick the option to compare. |
| UC5 | Only when hidden | Combined with `Page#pageVisibilitySignal()`: an OS notification when the user is in another tab or window, an in-app one when they are looking. |
| UC6 | Denied permission | After a denial, explains how to re-enable notifications, falls back to in-app messages, and switches back when the permission is granted again. |

## Run

```
./mvnw -pl common install -DskipTests
./mvnw -pl web-notifications spring-boot:run
```

Open <http://localhost:8080/>.

Notifications only work in a secure context (HTTPS or `localhost`). Your
operating system must also allow notifications from the browser.

## API gaps

See [`API-GAPS.md`](API-GAPS.md) for what the API still leaves open.
