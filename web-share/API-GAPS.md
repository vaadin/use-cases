# Web Share API — API gaps discovered while building the demos

Places where the `com.vaadin.flow.component.webshare` API either doesn't
cover a genuine Web Share use case or makes one awkward enough to work
around. Each entry is keyed to the use case that surfaced it. The API:
sharing is bound to a click trigger via
`WebShare.onClick(button).share(ShareContent)` (with an observed
`share(content, onShared, onError)` form), the payload is built with
`ShareContent.create().title(...).text(...).url(...)` (each accepting a String
literal or a `HasValue` source for live binding), and support is read from the
static `WebShare.supportSignal()` returning `Signal<WebShareSupport>`.

## File sharing (Web Share Level 2)

**Where it bit us:** dropped from the UC list — no view written
**Symptom:** The Web Share API spec defines a `files: File[]` member on
`ShareData` (the "Web Share Level 2" addition). Modern Chrome/Safari on
Android and iOS surface this when sharing photos, generated PDFs, or any
arbitrary blob produced server-side (e.g. a generated invoice). The
`ShareContent` builder (`title`/`text`/`url`) exposes no way to pass a file
or a `StreamResource`, so there is no path from a Flow `InputStream` to the
native share sheet.
**Workaround used:** None — we dropped the file-share use case rather than
faking it with an `executeJs` blob. The shim could be added later (see
suggested API below) but isn't yet justified by the rest of the demo.
**Suggested API:**

```java
// Builder method that accepts StreamResource attachments. The bound share
// would reject (and notify the observed onError) when navigator.canShare
// reports the files cannot be shared.
ShareContent files(StreamResource... files);
// e.g. WebShare.onClick(button).share(
//          ShareContent.create().title("Invoice").files(pdfResource));
```

References: [Web Share Level 2 draft](https://wicg.github.io/web-share/level-2/),
[MDN: ShareData.files](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share#files).

## No `navigator.canShare(data)` pre-check

**Where it bit us:** uc3 / CustomMessageView.java, uc4 / ShareListItemsView.java
**Symptom:** The browser's `navigator.canShare(data)` predicate lets a page
verify a payload before showing the share button, returning `false` when
e.g. files are too large, the URL scheme is blocked, or the data shape is
otherwise unshareable. You can only check whether *any* share is supported
(`WebShare.supportSignal()`), not whether *this specific payload* is. As a
result UC3 and UC4 enable the Share button even for payloads the browser
would reject.
**Workaround used:** Trust `WebShare.supportSignal()` to gate the button.
Per-payload validation falls back to the bound share rejecting at
invocation time (the observed `onError` form), which is too late for good UX.
**Suggested API:**

```java
// Per-payload predicate as a method on the ShareContent builder. Returns a
// signal/promise the caller can observe to update the UI before the user
// clicks. Backed by navigator.canShare(data).
Signal<Boolean> canShare(); // on ShareContent
// e.g. ShareContent.create().title(...).text(...).url(...).canShare()
```

## No test simulator / no browserless helper

**Where it bit us:** every UC test under `src/test/java/com/example/uc*/`
**Symptom:** there is no browserless test simulator. `WebShareTestSupport`
drives the support signal through
`UI.getCurrent().getInternals().setWebShareSupport(state)` — a public but
framework-internal setter that a third-party developer would have to read
flow-server source to find. The actual share invocation cannot be verified
browserless at all: it is a client-side trigger action bound to a button
click (`WebShare.onClick(button).share(...)`), so tests can only assert the
surrounding signal-driven UI state.
**Workaround used:** `web-share/src/test/java/com/example/WebShareTestSupport.java`
wraps the support setter.
**Suggested API:**

```java
// In browserless-test or a future flow-server test-fixtures jar.
public final class WebShareSimulator {
    /** Drives the support signal in the current UI. */
    public static void setSupport(WebShareSupport support);
    /** Records the last share bound/invoked so tests can assert on it. */
    public static ShareContent lastShareInvocation();
    /** Fires the observed onShared callback of the most recent bound share. */
    public static void completeLastShare();
    /** Fires the observed onError callback of the most recent bound share. */
    public static void failLastShare(String error);
}
```

Without the last three pieces, UC5's success/cancel/error branches can only
be exercised by reaching into package-private `handleSuccess` /
`handleError` methods on the view itself.

## Resolved: feature detection before the first view renders

**Where it bit us:** uc1 / ShareThisPageView.java, uc2 / CopyLinkFallbackView.java
**Original concern:** `WebShare.supportSignal()` reads `UNKNOWN` until the
client reports back, so the first view would render a "Detecting…" state and
then swap to the native-share or copy-link button.
**Resolution:** not a gap. The browser sends Web Share support with the
bootstrap (`v-r=init`) request, and Flow seeds the signal from it before the
router creates the first view, so a view in a real browser already reads
`SUPPORTED` or `UNSUPPORTED` on its first render. Flow has no server-side
rendering phase before that request. `supportSignal().peek()` is the
synchronous "seeded value, if any" accessor the original suggestion asked for.
`UNKNOWN` is only seen when no bootstrap request ran — in practice, browserless
tests — which is why the views keep an `UNKNOWN` branch.
