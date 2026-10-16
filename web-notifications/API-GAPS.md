# Web Notifications API — API gaps

Where Flow's `WebNotification` API
(`com.vaadin.flow.component.webnotification`, from
[vaadin/flow#26495](https://github.com/vaadin/flow/pull/26495)) made a use
case in this module awkward. The API wraps the browser
[Notifications API](https://developer.mozilla.org/en-US/docs/Web/API/Notifications_API):
`WebNotification.onClick(button).requestPermission()`,
`WebNotification.permissionSignal()`, `WebNotification.show(...)` returning a
`WebNotificationHandle`, and `WebNotification.closeByTag(...)`. Entries 1, 2, 3
and 5 were fixed by that API and are removed.

## 4. Unusable constructor only discovered when showing

**Where it bites:** UC2 / LongTaskView.java, UC5 / HiddenTabOnlyView.java,
UC6 / DeniedFallbackView.java (in-app fallbacks)
**Symptom:** Chrome on Android exposes `Notification` and can report
`granted`, but `new Notification()` throws `TypeError: Illegal constructor`.
Only `ServiceWorkerRegistration.showNotification()` works there.
`WebNotificationPermission.UNSUPPORTED` covers only a missing `Notification`
and an insecure context, so `WebNotification.permissionSignal()` says
`GRANTED`. The failure arrives later, from `WebNotification.show`, as a
`WebNotificationError` with `Reason.UNKNOWN`.
**Workaround used:** every view that shows a notification adds an error
listener to the `WebNotificationHandle` and falls back to an in-app Vaadin
`Notification`.
**Suggested API:** `WebNotification.show` uses
`registration.showNotification()` when a service worker is registered and the
constructor is not available. Otherwise it reports the case as `UNSUPPORTED`
up front.

## 6. No test simulator

**Where it bites:** every test under `src/test/java/com/example/uc*` /
`WebNotificationSimulator.java`, `PageVisibilityTestSupport.java`
**Symptom:** browserless tests never run client JavaScript, so there is no
permission prompt, no notification and no click. Flow has nothing like
`GeolocationSimulator` for notifications, and `WebNotificationHandle` has no
accessors for the title or options it was shown with. The JavaScript itself
(gesture handling, `renotify`, the Android constructor failure) stays
untested.
**Workaround used:** `WebNotificationSimulator` plays the browser through
framework internals. It sets the permission with
`UIInternals#setWebNotificationPermissionFromClient`. It reads the
`webNotification.show` / `close` / `closeByTag` calls from
`UIInternals#dumpPendingJavaScriptInvocations`, after running
`StateTree#runExecutionsBeforeClientResponse` (calls through a JavaScript
definition are queued only before a response). It reports clicks, closes and
failures through each notification's `ReturnChannelRegistration`. Like the
browser, it refuses to show without permission and closes a notification
that a new one with the same tag replaces. `PageVisibilityTestSupport` drives
the package-private `Page#setPageVisibility` through reflection.
**Suggested API:** a browserless `WebNotificationSimulator` shipped with the
feature: `setPermission(...)`, `getShownNotifications()` (title, options),
`click(notification)`, `close(notification)`, `fail(notification, reason)`.
Also a public way to drive `Page#pageVisibilitySignal()` in tests.

## 7. Overlap with Web Push and service-worker notifications

**Where it bites:** UC5 / HiddenTabOnlyView.java (compare page-visibility
UC3 / NotificationGatingView.java, which uses `flow-webpush`)
**Symptom:** there are three ways to put an OS notification on screen. Flow
covers two of them, with unrelated APIs:

1. `new Notification()` in the page: only while the tab is open, no service
   worker or VAPID keys. Covered by `WebNotification`.
2. `ServiceWorkerRegistration.showNotification()` from the page: only while
   the tab is open, but required on Android and supports `actions`. Not
   covered.
3. Web Push: the server sends a push message and the service worker calls
   `showNotification()`; works with the tab closed. Covered by `flow-webpush`
   (`WebPush#subscribe`, `WebPush#sendNotification`) plus `@PWA`.

All three share one permission. `WebPush#subscribe` calls
`requestPermission()` itself and reports the result only through one-shot
callbacks (`WebPush#isNotificationGranted` / `isNotificationDenied`). An app
that uses both has two unrelated views of the same permission. The generated
service worker's `notificationclick` handler only focuses or opens a window,
so the server never learns which notification was clicked; the UC3 "open the
related item" scenario is not possible with Web Push. `WebPushMessage` has
only `title` and untyped `options`, with no typed place for `tag`, `data` or a
URL to open on click, while `WebNotificationOptions` is a separate type.
**Workaround used:** none beyond choosing the channel per scenario: this
module uses in-page notifications (1) and points to the page-visibility module
for Web Push (3).
**Suggested API:** let `WebPush` read and update
`WebNotification.permissionSignal()`, share `WebNotificationOptions` with
`WebPushMessage`, and deliver a click to the server in both cases (for Web
Push, the service worker posts the notification's `data` to the focused
client, which forwards it to the UI).
Upstream: https://github.com/vaadin/flow/issues/20803

## 8. No way to re-check the permission on demand

**Where it bites:** UC6 / DeniedFallbackView.java (the help panel)
**Symptom:** after a denial the user re-enables notifications in the site
settings. The client publishes a new value to
`WebNotification.permissionSignal()` only on the Permissions API `change`
event and when the tab becomes visible again; window `focus` is not watched,
and the server can not ask for a re-check. Safari does not fire `change`
reliably, and the site settings popup next to the address bar never hides the
tab. So the view only notices after the user switches tabs and back. A "Check
again" button would have nothing to call; the help text tells the user to
come back to the tab instead.
**Workaround used:** none.
**Suggested API:** also re-check on window `focus`, and/or add
`WebNotification.refreshPermission()` that makes the client publish the
current permission.
