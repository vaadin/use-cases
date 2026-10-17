# Clipboard API — API gaps

Where the `com.vaadin.flow.component.clipboard` API made a use case in this
module impossible or awkward.

The API is a small static facade. `Clipboard.onClick(component)` returns a
`ClipboardBinding` whose `writeText` / `writeHtml` / `writeImage` /
`write(ClipboardContent)` methods bind a clipboard write to a click, so the
write runs inside the browser's click handler while the user gesture is still
valid. The value is either a `String` fixed at binding time or, for
`writeText` and `ClipboardContent.text` only, the `value` of a
`Component & HasValue<?, String>`, read on the client at click time.
`Clipboard.onPaste` and `Clipboard.onFilePaste` observe paste events.

Most entries come from UC8, the copy button in every grid row from the
[forum thread](https://vaadin.com/forum/t/clipboard-copy/164697/11). The use
case works, and simply: a `ComponentRenderer` column with
`Clipboard.onClick(button).writeText(customer.email())` per row. The grid only
builds the rows it renders, so the 500-row demo holds about ten copy buttons,
also after scrolling. The gaps are about the cost of the binding that comes
with each of those buttons.

## 1. No column-level binding — one trigger per rendered row

**Where it bites:** UC8 / CopyFromGridView.java
**Symptom:** the binding from `Clipboard.onClick` is per component instance.
A copy button in every row means, per rendered row, a `Button`, a
`ClickTrigger`, an `addJsInitializer` registration and a client-side
listener. All of them are torn down and rebuilt each time a row scrolls out of
the buffer and back, although the value has the same shape in every row. A
`LitRenderer` cannot help: its `withFunction(...)` callbacks run on the
server, and by then the browser's user gesture is gone, so
`navigator.clipboard.write*` rejects.
**Workaround used:** none needed for correctness. UC8 accepts the per-row
binding and documents the cost.
**Suggested API:** a client-side trigger action in a `LitRenderer` template,
so one binding serves the whole column and the value comes from the row's
client-side data:

```java
LitRenderer.<Customer> of("<span>${item.email}</span>"
                + "<button @click=${copy}>Copy</button>")
        .withProperty("email", Customer::email)
        .withClientAction("copy", Clipboard.write().text("email"));
```

## 2. No signal value source — the value must be known before the click

**Where it bites:** UC8 / CopyFromGridView.java — the alternatives to a
per-row button
**Symptom:** `ClipboardBinding.writeText` takes a `String` literal or a
`HasValue<?, String>` component. One shared affordance for many rows — a
toolbar "copy selected" button, a grid context menu, a shortcut — needs the
value chosen at click time. Binding again on each change does not work either:
each `writeText` call adds another binding, it does not replace the previous
one. The internal `com.vaadin.flow.component.trigger.internal.SignalInput`
already mirrors a server-side `Signal` to the client and reads it when the
trigger fires, but the public clipboard API does not use it. The
`CopyToClipboard(Supplier<String>)` helper from the
[forum thread](https://vaadin.com/forum/t/clipboard-copy/164697/8) had these
ergonomics, but its `executeJs` runs after a server round trip, when the
gesture is gone.
**Workaround used:** UC8 binds a literal per row. A shared affordance would
need a hidden component with a `value` property, filled on the server before
the click. The obvious choice, `com.vaadin.flow.component.html.Input`,
silently strips CR/LF from its value, which breaks multi-line copies.
**Suggested API:** a signal value source next to the `HasValue` overloads. A
`SerializableSupplier<String>` would not work, because it runs on the server
after the gesture is gone; a signal's value is already on the client.

```java
ClipboardBinding.writeText(Signal<String> value);
ClipboardBinding.writeText(Signal<String> value, SerializableConsumer<String> onCopied,
        SerializableConsumer<PromiseAction.Error> onError);
ClipboardContent.text(Signal<String> value);
```

## 3. `GridMenuItem` is not a `ClickNotifier`

**Where it bites:** UC8 / CopyFromGridView.java — tried as the per-row
affordance before the view settled on a button
**Symptom:** `Clipboard.onClick` requires `T extends Component &
ClickNotifier<?>`. `ContextMenu`'s `MenuItem` implements `ClickNotifier` (UC6
uses it), but `GridContextMenu`'s `GridMenuItem` only extends `MenuItemBase`.
So a grid context-menu copy item does not compile:

```java
GridMenuItem<Customer> copyEmail = grid.addContextMenu().addItem("Copy email");
Clipboard.onClick(copyEmail).writeText(email); // does not compile
```

**Workaround used:** none in the final view. Wrapping the item's label in a
`Span` (a `ClickNotifier`) and stretching it over the item with CSS works.
**Suggested API:** `GridMenuItem<T> implements ClickNotifier<GridMenuItem<T>>`
in `vaadin-grid-flow`, as `MenuItem` does. Or let `Clipboard.onClick` accept
any `Component` and listen to the DOM `click` event directly.

## 4. No browserless simulator for copies and pastes

**Where it bites:** every test under `src/test/java/com/example/uc*/` /
`ClipboardTestSupport.java`
**Symptom:** a write is a client-side trigger action bound to a click
(`Clipboard.onClick(button).writeText(...)`). A browserless test can not fire
the trigger, inspect what a component's binding would write, or assert against
a fake clipboard. `CopyFromGridViewTest` checks that every row renders its own
value and copy button, but not that the button is bound to that value: a bug
that bound every row to the first row's email would pass. Pastes are no
easier: `Clipboard.onPaste` listens to a DOM `paste` event whose data keys are
private JavaScript expressions in `Clipboard`, and `Clipboard.onFilePaste`
uploads each file over its own HTTP request, which browserless-test can not
send.
**Workaround used:** `ClipboardTestSupport` reaches into Flow internals. It
reports a copy outcome by invoking the return channel that `PromiseAction`
registers on the button's `StateNode`, fires a `paste` `DomEvent` with the
event-data keys copied from `Clipboard`, and feeds the `UploadHandler` that
`onFilePaste` stores on the element a synthetic `UploadEvent` with the
`X-Paste-Id` and `X-Paste-File-Count` headers. The copy itself is checked by
hand in a real browser.
**Suggested API:** a simulator in browserless-test, following
`GeolocationSimulator`:

```java
ClipboardSimulator clipboard = ClipboardSimulator.install();
test(copyButton).click();
assertEquals("ada.lovelace1@northwind.example", clipboard.lastWrittenText());

clipboard.pasteInto(dropZone).html("<table>…</table>").text("a\tb").simulate();
clipboard.pasteInto(dropZone).file("shot.png", "image/png", bytes).simulate();
```

Upstream: https://github.com/vaadin/browserless-test/issues/64

## 5. No clipboard availability signal

**Where it bites:** no view — the availability use case was dropped
**Symptom:** `Clipboard` has no way to tell whether the clipboard can be used
at all (non-secure context, an iframe permissions policy, a denied
`clipboard-write` permission) before the app offers a copy button. The only
signal is the `onError` callback, after the user has clicked. In a grid, 30
copy buttons all look enabled and all fail the same way.
**Workaround used:** none. The copy buttons are always shown, and a failure is
reported through `onError`.
**Suggested API:** a support or permission signal, like
`WebShare.supportSignal()` and `Fullscreen.stateSignal()`:

```java
Signal<ClipboardAvailability> Clipboard.availabilitySignal();
```

## 6. The error type of `onError` lives in an internal package

**Where it bites:** every copy with an `onError` callback (UC1–UC4, UC6, UC8)
**Symptom:** the `onError` consumer of `ClipboardBinding.writeText`,
`writeHtml`, `writeImage`, `write` and `read*` takes a
`com.vaadin.flow.component.trigger.internal.PromiseAction.Error`. The views
get away with a lambda, but code that names the type — a shared error
handler, a method reference to a helper, a field — has to import a package
marked internal, which may change without notice.
**Workaround used:** none; the views only use lambdas, so the type never
appears in the source.
**Suggested API:** a public type in the clipboard package, e.g.
`record ClipboardError(String name, String message)`, or a public
`PromiseError` next to the other trigger types.
