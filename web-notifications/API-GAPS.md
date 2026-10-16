# Web Notifications API — API gaps discovered while building the demos

The browser [Notifications API](https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API)
(`Notification.requestPermission()`, `new Notification(title, options)`,
`Notification.permission`, `onclick` / `onclose`) shows native OS
notifications from a page while its tab is open. Every view in this module
uses Flow's `WebNotification` API from
[vaadin/flow#26495](https://github.com/vaadin/flow/pull/26495):
`WebNotification.onClick(button).requestPermission()`,
`WebNotification.permissionSignal()`, `WebNotification.show(...)` returning a
`WebNotificationHandle` with click, close and error listeners, and
`WebNotification.closeByTag(...)`. That API closed the former entries 1
(no API at all), 2 (permission request inside the click gesture), 3 (no
permission signal) and 5 (click back to the server). Each entry below names
the use case that exposed a gap that remains.

## 4. Unusable constructor only discovered when showing

**Where it bites:** UC2 / LongTaskView.java, UC5 / HiddenTabOnlyView.java,
UC6 / DeniedFallbackView.java (in-app fallbacks)
**Symptom:** `WebNotificationPermission.UNSUPPORTED` covers a missing
`Notification` constructor and an insecure context, but not Chrome on
Android, which exposes `Notification` and can report `granted`, yet throws
`TypeError: Illegal constructor` from `new Notification()` — only
`ServiceWorkerRegistration.showNotification()` works there. The permission
signal says `GRANTED`, and the failure arrives later as a
`WebNotificationError` with `Reason.UNKNOWN`.
**Workaround used:** Every view that shows a notification adds an error
listener to the `WebNotificationHandle` and falls back to an in-app Vaadin
`Notification`.
**Suggested API:** Have `WebNotification.show` use
`registration.showNotification()` when a service worker is registered and the
constructor is unavailable, so callers don't have to care; otherwise report
the case as `UNSUPPORTED` up front.

## 6. No test simulator

**Where it bites:** every test under `src/test/java/com/example/uc*`
**Symptom:** Browserless tests never run client JavaScript, so there is no
permission prompt, no notification and no click. Flow has nothing like
`GeolocationSimulator` for notifications, and `WebNotificationHandle` has no
accessors for the title or options it was shown with.
**Workaround used:** `src/test/java/com/example/WebNotificationSimulator.java`
plays the browser through framework internals: it sets the permission with
the framework-only `UIInternals#setWebNotificationPermissionFromClient`,
reads the `webNotification.show` / `close` / `closeByTag` calls from
`UIInternals#dumpPendingJavaScriptInvocations` (after running
`StateTree#runExecutionsBeforeClientResponse`, since calls through a
JavaScript definition are queued only before a response), and reports
clicks, closes and failures through each notification's
`ReturnChannelRegistration`. It also mimics the browser by refusing to show
without permission and by closing a notification replaced through its tag.
What remains untested: the JavaScript itself (gesture handling, `renotify`,
Android constructor failure). Visibility is driven through reflection on
`Page#setPageVisibility` (copied from the page-visibility module), which is
another missing test hook.
**Suggested API:** A browserless `WebNotificationSimulator` shipped with the
feature: `setPermission(...)`, `getShownNotifications()` (title, options),
`click(notification)`, `close(notification)`, `fail(notification, reason)`,
plus a public way to drive `pageVisibilitySignal()` in tests.

## 7. Overlap with Web Push and service-worker notifications

**Where it bites:** UC5 / HiddenTabOnlyView.java (compare page-visibility
UC3 / NotificationGatingView.java, which uses `flow-webpush`)
**Symptom:** There are three ways to put an OS notification on screen, and
Flow covers two of them with unrelated APIs:

1. `new Notification()` in the page — works only while the tab is open,
   needs no service worker or VAPID keys. **Covered** by `WebNotification`
   (this module).
2. `ServiceWorkerRegistration.showNotification()` from the page — also only
   while the tab is open, but required on Android and supports `actions`.
   **Not covered.**
3. Web Push: the server sends a push message, the service worker calls
   `showNotification()` — works with the tab closed. **Covered** by
   `flow-webpush` (`WebPush#subscribe`, `sendNotification`) plus `@PWA`.

All three share one permission. `WebPush#subscribe` calls
`requestPermission()` internally and reports it through one-shot callbacks
(`WebPush#isNotificationGranted` / `isNotificationDenied`), so an app using
both Web Push and `WebNotification` has two unrelated views of the same
permission. The generated service worker's `notificationclick` handler only
focuses or opens a window; the server never learns which notification was
clicked, so the UC3 "open the related item" scenario is not possible with
Web Push. And `WebPushMessage` carries only `title` + options, so there's no
typed place for `tag`, `data`, or a URL to open on click, while
`WebNotificationOptions` is a separate type.
**Workaround used:** None beyond choosing the channel per scenario: this
module uses in-page notifications (1) and points to the page-visibility
module for (3).
**Suggested API:** Let `WebPush` read and update
`WebNotification.permissionSignal()`, share `WebNotificationOptions` with
`WebPushMessage`, and deliver a click to the server in both cases (for Web
Push: let the service worker post the notification's `data` to the focused
client, which forwards it to the UI).
Upstream: https://github.com/vaadin/flow/issues/20803

## 8. No way to re-check the permission on demand

**Where it bites:** UC6 / DeniedFallbackView.java (the help panel)
**Symptom:** After a denial the user re-enables notifications in the site
settings. The client publishes the new permission on the Permissions API
`change` event and when the tab becomes visible again, but there is no way to
ask for a re-check from the server, and window `focus` is not watched.
Safari does not fire `change` reliably, and the site settings popup next to
the address bar never hides the tab, so in that case the view only notices
after the user switches tabs and back. The former "Check again" button had
nothing to call and was removed; the help text tells the user to come back
to the tab instead.
**Workaround used:** None.
**Suggested API:** Also re-check on window `focus`, and/or add
`WebNotification.refreshPermission()` that makes the client publish the
current permission.
