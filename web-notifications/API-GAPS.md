# Web Notifications API — API gaps discovered while building the demos

The browser [Notifications API](https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API)
(`Notification.requestPermission()`, `new Notification(title, options)`,
`Notification.permission`, `onclick` / `onclose`) shows native OS
notifications from a page while its tab is open. Vaadin Flow has no API for
it. Every view in this module goes through a local shim,
`src/main/java/com/example/MissingAPI.java`, built from public Flow
primitives only (`Element#executeJs` calls into
`src/main/frontend/web-notifications.ts`, DOM event listeners with
`addEventDetail()`, a per-UI `ValueSignal` stored with `ComponentUtil`).
Each entry below names the use case that exposed the gap.

## 1. No Flow API for the Notifications API at all

**Where it bites:** every view — UC1 / PermissionStateView.java through
UC6 / DeniedFallbackView.java
**Symptom:** `flow-server` 25.4 has no class or client code that calls
`Notification.requestPermission()` or `new Notification(...)`. (Checked
with `jar tf` and a search of the jar for `Notification.permission` /
`requestPermission`; the only hit anywhere in Flow is in `flow-webpush`,
see below.) An app developer who just wants "tell the user the report is
ready" has to write and maintain their own JavaScript bridge, including
the Safari callback form of `requestPermission`, secure-context
detection, and event wiring back to the server.
**Workaround used:** `MissingAPI.showNotification(UI, String,
NotificationOptions)` sends the title and a Jackson-serialised options
record to the `web-notifications.ts` module and returns a
`ShownNotification` handle.
**Suggested API:**

```java
// Same shape as WebShare / pageVisibilitySignal: a small entry point on Page
// (or a WebNotifications facade) with a typed options builder.
WebNotification n = ui.getPage().showNotification("Your report is ready",
        NotificationOptions.create().body("Q3 sales").tag("report")
                .icon(iconResource).requireInteraction(true));
n.addClickListener(e -> ...);
n.addCloseListener(e -> ...);
n.close();
```

Upstream: https://github.com/vaadin/flow/issues/26493

## 2. Permission request has to run inside the click gesture

**Where it bites:** UC1 / PermissionStateView.java, UC2 / LongTaskView.java
(the "Generate report" click also asks for permission)
**Symptom:** Browsers only show the prompt when `requestPermission()` is
called during a user gesture (Firefox and Safari enforce it strictly;
Chrome quietly blocks sites that prompt without one). A normal server-side
`ClickListener` that calls `executeJs` runs after a round-trip, when the
gesture is gone. There is no public Flow way to say "on click of this
button, run this browser action".
**Workaround used:** `MissingAPI.requestPermissionOnClick(Component)` adds a
native `click` listener on the component's element (from `whenAttached`,
so it is added again when Flow recreates the element), which
calls `requestPermission()` on the client and reports the answer back as a
DOM event. The returned `Registration` removes it.
The trigger/action API (`com.vaadin.flow.component.trigger.internal`, used by
`WebShare.onClick` and `RequestFullscreenAction`) would be the natural home,
but it is internal, so the shim does not use it.
**Suggested API:**

```java
// Mirrors WebShare.onClick(button).share(...): bound on the client, runs in
// the gesture, result observable on the server.
WebNotifications.onClick(button).requestPermission();
// or, as a public trigger action:
new ClickTrigger(button).triggers(new RequestNotificationPermissionAction());
```

## 3. No permission signal

**Where it bites:** UC1 / PermissionStateView.java (badge, enabling the
buttons), UC6 / DeniedFallbackView.java (help panel appears on denied and
disappears when re-enabled)
**Symptom:** The permission is state that changes outside the app: the user
answers the prompt, or later flips it in site settings. Flow has
`pageVisibilitySignal()` and `WebShare.supportSignal()` for comparable
browser state, but nothing for the notification permission. `flow-webpush`
has `WebPush#isNotificationGranted` / `isNotificationDenied`, but they are
one-shot async callbacks tied to the Web Push setup, not a signal, and
cannot express `default` or "unsupported".
**Workaround used:** `MissingAPI.permissionSignal(UI)` returns a read-only
per-UI signal (`UNKNOWN`, `DEFAULT`, `GRANTED`, `DENIED`, `UNSUPPORTED`). The
client reports on install, on `PermissionStatus.onchange` (Permissions API),
and re-checks on `visibilitychange` / window `focus` (Safari doesn't fire
`onchange` reliably). `MissingAPI.refreshPermission(UI)` forces a report
(UC6 "Check again").
**Suggested API:**

```java
Signal<NotificationPermission> Page#notificationPermissionSignal();
enum NotificationPermission { UNKNOWN, DEFAULT, GRANTED, DENIED, UNSUPPORTED }
```

## 4. No server-side feature detection

**Where it bites:** UC1 / PermissionStateView.java ("Not supported" state),
all views' fallbacks
**Symptom:** The server can't tell up front whether the browser has a
`Notification` constructor, whether the page is a secure context (the API
is unavailable over plain HTTP except on localhost), or whether the
constructor is usable at all: Chrome on Android exposes `Notification` but
`new Notification()` throws `TypeError: Illegal constructor` — only
`ServiceWorkerRegistration.showNotification()` works there.
**Workaround used:** The permission signal folds "no API" and "insecure
context" into `UNSUPPORTED`. The Android case is only discovered when
showing; the shim reports it via `ShownNotification#onError(reason)` and the
views fall back to an in-app Vaadin `Notification` (UC2, UC5, UC6).
**Suggested API:** Make `UNSUPPORTED` part of the permission signal (above)
and have the Flow implementation transparently use
`registration.showNotification()` when a service worker is registered and
the constructor is unavailable, so callers don't have to care.

## 5. Click back to the server needs hand-made wiring

**Where it bites:** UC3 / ClickToOpenView.java
**Symptom:** The main reason to click a notification is "take me to the
thing". That needs: `window.focus()` on the client (only allowed inside the
notification's click handler), closing the notification, and telling the
server *which* notification was clicked so it can open the item. None of
this is provided; `executeJs` return values can't carry later events.
**Workaround used:** The shim gives every notification a server-generated
id, keeps the handle in a per-UI map, and the client dispatches
`vaadin-web-notification-click` / `-close` / `-error` custom events with
`{id}` on the UI element; server listeners use `addEventDetail()` and
`allowInert()` (so clicks still arrive while a modal dialog is open) and
route to the handle's `onClick` / `onClose` / `onError`. Browsers don't
fire `close` for a notification replaced via `tag`, so the shim also drops
older handles with the same tag to avoid leaking them.
**Suggested API:** Listener methods on the returned handle
(`addClickListener`, `addCloseListener`, `addErrorListener`), with focusing
the tab as the default click behaviour (opt-out flag), and handles released
automatically on close, replace, or UI detach.

## 6. No test simulator

**Where it bites:** every test under `src/test/java/com/example/uc*`
**Symptom:** Browserless tests never run client JavaScript, so there is no
permission prompt, no notification and no click. Flow has nothing like
`GeolocationSimulator` for notifications, and a real Flow API would have the
same problem.
**Workaround used:** `src/test/java/com/example/WebNotificationSimulator.java`
fires the shim's DOM events directly through
`ElementListenerMap#fireEvent` (an `internal` class) to set the permission,
click, and fail notifications, and reads the live handles through a
package-private `MissingAPI.liveNotifications(UI)`. What remains untested:
the JavaScript itself (gesture handling, Safari callback form, tag
replacement, `renotify`, Android constructor failure). Visibility is
driven through reflection on `Page#setPageVisibility` (copied from the
page-visibility module), which is another missing test hook.
**Suggested API:** A browserless `WebNotificationSimulator` shipped with the
feature: `setPermission(...)`, `getShownNotifications()` (title, options),
`click(notification)`, `close(notification)`, plus a public way to drive
`pageVisibilitySignal()` in tests.

## 7. Overlap with Web Push and service-worker notifications

**Where it bites:** UC5 / HiddenTabOnlyView.java (compare page-visibility
UC3 / NotificationGatingView.java, which uses `flow-webpush`)
**Symptom:** There are three ways to put an OS notification on screen, and
Flow covers only one of them:

1. `new Notification()` in the page — works only while the tab is open,
   needs no service worker or VAPID keys. **Not covered** (this module).
2. `ServiceWorkerRegistration.showNotification()` from the page — also only
   while the tab is open, but required on Android and supports `actions`.
   **Not covered.**
3. Web Push: the server sends a push message, the service worker calls
   `showNotification()` — works with the tab closed. **Covered** by
   `flow-webpush` (`WebPush#subscribe`, `sendNotification`) plus `@PWA`.

All three share one permission. `WebPush#subscribe` calls
`requestPermission()` internally and throws away the result if the
subscription fails, so an app using both Web Push and in-page notifications
has two unrelated views of the same permission. The generated service
worker's `notificationclick` handler only focuses or opens a window; the
server never learns which notification was clicked, so the UC3 "open the
related item" scenario is not possible with Web Push either. And
`WebPushMessage` carries only `title` + options, so there's no typed place
for `tag`, `data`, or a URL to open on click.
**Workaround used:** None beyond choosing the channel per scenario: this
module uses in-page notifications (1) and points to the page-visibility
module for (3).
**Suggested API:** One notifications facade with a shared permission
signal (above), a single `NotificationOptions` type used by both
`showNotification` and `WebPushMessage`, and a click event that reaches the
server in both cases (for Web Push: let the service worker post the
notification's `data` to the focused client, which forwards it to the UI).
Upstream: https://github.com/vaadin/flow/issues/20803
