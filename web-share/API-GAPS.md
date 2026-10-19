# Web Share API — API gaps

Where the `com.vaadin.flow.component.webshare` API made a use case in this
module impossible or awkward. A share is bound to a click with
`WebShare.onClick(button).share(ShareContent)` (or the observed
`share(content, onShared, onError)` form). The payload is built with
`ShareContent.create().title(...).text(...).url(...)`. Support is read from
`WebShare.supportSignal()`.

## 1. No file sharing (Web Share Level 2)

**Where it bites:** a file-share use case — dropped, no view written
**Symptom:** the Web Share spec has a `files` member in `ShareData`. Mobile
Chrome and Safari use it to share photos, PDFs or a file made on the server
(e.g. an invoice). `ShareContent` has only `title`, `text` and `url`, so there
is no way to pass a file or a `StreamResource` to the native share sheet.
**Workaround used:** none. The use case was dropped instead of faking it with
an `executeJs` blob.
**Suggested API:** `ShareContent files(StreamResource... files)`. The bound
share calls the observed `onError` when `navigator.canShare` says the files
can not be shared. See the
[Web Share Level 2 draft](https://wicg.github.io/web-share/level-2/) and
[MDN: ShareData.files](https://developer.mozilla.org/en-US/docs/Web/API/Navigator/share#files).

## 2. No `navigator.canShare(data)` check for one payload

**Where it bites:** UC3 / `CustomMessageView.java`
**Symptom:** `WebShare.supportSignal()` only says whether the browser can
share at all, not whether it can share this payload. The browser's
`navigator.canShare(data)` returns `false` when, for example, the URL scheme
is blocked or the files are too large. UC3 enables the Share button even for
a URL the reader typed that the browser will reject.
**Workaround used:** the button is gated on `WebShare.supportSignal()` only.
A bad payload is found only when the share fails (the observed `onError`
form), which is too late for good UX.
**Suggested API:** `Signal<Boolean> canShare()` on `ShareContent`, backed by
`navigator.canShare(data)`, so the UI can update before the user clicks.

## 3. No browserless simulator for Web Share

**Where it bites:** every test of the module / `WebShareTestSupport.java`,
UC5 / `ShareFeedbackView.java`, `ShareFeedbackViewTest.java`
**Symptom:** browserless-test has no tester for Web Share. The tests set the
support state through `UIInternals#setWebShareSupport(WebShareSupport)`, a
public but framework-internal setter. The share itself is a client-side
action bound to the click (`WebShare.onClick(button).share(...)`), so a test
can not check which `ShareContent` was shared, and can not fire the
`onShared` or `onError` callback.
**Workaround used:** `WebShareTestSupport.setSupport` wraps the internal
setter. UC5 has "Simulate success / cancel / failure" buttons, in a box
apart from the feature, that call the same handlers as `onShared` and
`onError`. The test clicks them. They also let a reader whose browser has no
`navigator.share` see the log. Remove them once a simulator exists.
**Suggested API:** a browserless helper, e.g. `WebShareSimulator` with
`setSupport(WebShareSupport)`, `lastShareInvocation()` returning the
`ShareContent`, `completeLastShare()` and `failLastShare(String error)`.
Upstream: https://github.com/vaadin/browserless-test/issues/166

## 4. Share content can not come from a signal

**Where it bites:** UC1 / `ShareThisPageView.java`, UC2 /
`CopyLinkFallbackView.java`, UC6 / `ShareInviteLinkView.java`
**Symptom:** `ShareContent.title/text/url` take either a fixed `String` or a
`Component & HasValue<?, String>` whose `value` property is read in the
browser at click time. A value the server learns or changes after the share is
bound — the page address from `Page#fetchCurrentURL`, a newly generated
invite link — can not be passed directly, and the share can not be bound a
second time without adding a second click handler (`WebShareBinding#share`
returns `void`, so there is nothing to remove).
**Workaround used:** the value is put into a read-only `TextField` that the
reader can see, and the share is bound to that field.
**Suggested API:** `ShareContent url(Signal<String>)` (and the same for
`title` and `text`), backed by the internal
`com.vaadin.flow.component.trigger.internal.SignalInput` that already mirrors
a signal to the client for other trigger actions.

## 5. An empty field is shared as an empty string

**Where it bites:** UC3 / `CustomMessageView.java`
**Symptom:** a slot bound to a field is always sent:
`ShareAction#toPromiseJs` builds `navigator.share({title: …, text: …,
url: …})` with every slot that was set when binding, whatever its value at
click time. An empty title or text is shared as `""`, and the browser resolves
an empty URL against the page address, so it shares the current page instead
of no URL.
**Workaround used:** none. UC3's preview shows the empty strings and the view
text explains what an empty URL does.
**Suggested API:** leave out a slot whose value is empty at click time, or a
`ShareContent` option to do so (e.g. `omitEmpty()`).
