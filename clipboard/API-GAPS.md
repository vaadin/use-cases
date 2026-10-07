# Clipboard API — API gaps discovered while building the demos

Places where the `com.vaadin.flow.component.clipboard` API either doesn't
cover a genuine clipboard use case or makes one awkward enough to work
around. Each entry is keyed to the use case that surfaced it.

The API is a small static facade:

- `Clipboard.onClick(component)` returns a `ClipboardBinding` whose
  `writeText` / `writeHtml` / `writeImage` / `write(ClipboardContent)`
  methods bind a clipboard write to a click trigger, so the write runs inside
  the browser's own click handler while the user gesture is still valid; the
  `read` / `readText` / `readHtml` methods do the same for reading. Every
  variant has an observed overload taking `onCopied` / `onError`.
- The value written is either a literal `String` fixed at binding time, or —
  for `writeText` and `ClipboardContent.text` only — the `value` property of a
  `Component & HasValue<?, String>`, read on the client at click time.
- `Clipboard.onPaste(component, listener)` and
  `Clipboard.onFilePaste(component, handler)` observe browser paste events.

The gaps below were found while building **UC8 — a copy button in every grid
row** (`uc8/CopyFromGridView.java`), the use case from the
[forum thread](https://vaadin.com/forum/t/clipboard-copy/164697/11): "a button
next to the value (for example addresses) inside a table to copy the value",
asked with the worry of "instantiating million of components and instances of
the clipboard helper".

The good news first: the use case **works, and works simply**. A
`ComponentRenderer` column that does
`Clipboard.onClick(button).writeText(customer.email())` per row is the entire
implementation, and the grid only ever materialises the rows it renders —
measured in a browser, the 500-row demo holds ten copy buttons, and scrolling
to row 400 leaves it at ten, with the recycled rows' buttons copying their own
values. So the answer to the thread's worry is "the components are bounded by
the viewport". Everything below is about the cost of the *binding* that comes
with each of those buttons.

## No column-level or renderer-level binding — one trigger per rendered row

**Where it bit us:** uc8 / CopyFromGridView.java — the forum question itself
**Symptom:** the clipboard binding is per component instance. A copy button in
every row therefore means, per rendered row: a `Button`, a `ClickTrigger`, an
`addJsInitializer` registration, and a client-side listener — all torn down and
rebuilt every time the row scrolls out of the buffer and back. The value being
copied is the same shape for every row (one column's value for one item), yet
there is no way to say that once for the column. The scroll churn is the part
that has no answer today: the binding is re-created per row per scroll, where
one binding for the whole column would do.

`LitRenderer` — the tool that exists precisely to avoid per-row components —
cannot help here. Its `withFunction(...)` callbacks are server round trips, so
by the time the server sees the click the browser's transient activation is
gone and `navigator.clipboard.write*` rejects. Client-side trigger actions and
lightweight renderers are simply disjoint today.

**Workaround used:** none needed for correctness — UC8 accepts the per-row
binding. The demo documents the cost rather than hiding it.
**Suggested API:** a way to attach a client-side trigger action to a renderer
template, so one binding serves the whole column and the value comes from the
row's own client-side data:

```java
// The action is rendered into the LitRenderer template and fires on the
// client; the item's serialised properties are the value source.
LitRenderer.<Customer> of("<span>${item.email}</span>"
                + "<button @click=${copy}>Copy</button>")
        .withProperty("email", Customer::email)
        .withClientAction("copy", Clipboard.write().text("email"));
```

## No dynamic value source — the copied value must be known before the click

**Where it bit us:** uc8 / CopyFromGridView.java — the alternatives to a
per-row button
**Symptom:** `Clipboard.onClick(...)` has to be given the value at binding
time. The two sources are a `String` literal and the `value` property of a
`HasValue<?, String>` component. The per-row button gets away with a literal
only because the renderer re-runs for every row; anything that reuses *one*
affordance across rows — a single toolbar "copy selected" button, a grid
context menu, a copy shortcut — needs the value chosen at click time and has
nowhere to put it. Re-binding on each change is not an option either: each
`writeText` call adds another wiring (`Trigger.triggers` appends, it does not
replace).

This is also a regression against what the community had already settled on.
The reusable helper the forum thread converged on
([post 8](https://vaadin.com/forum/t/clipboard-copy/164697/8)) is
`CopyToClipboard(Supplier<String> text)` — a copy icon whose value is resolved
at click time — used as `new CopyToClipboard(() -> textField.getValue())`. That
helper is unsafe for other reasons (the `executeJs` it wraps runs after a
server round trip, so the gesture is gone), but its *ergonomics* are the ones a
grid wants, and they are what the new API dropped: it can bind a value, but not
a way to compute one.

The trigger machinery already supports it internally — the internal
`com.vaadin.flow.component.trigger.internal.SignalInput` mirrors a server-side
`Signal` into a client-side property and reads it when the trigger fires. It is
simply not reachable from the public clipboard API.

**Workaround used:** UC8 sidesteps it by binding a literal per row. The
workaround for the shared-affordance case is an off-screen component with a
`value` property, filled server-side before the click can happen — with the
trap that the obvious choice, `com.vaadin.flow.component.html.Input`, silently
strips CR/LF from its value (HTML value sanitisation), mangling any multi-line
copy.
**Suggested API:** a signal-backed value source on every write, mirroring the
existing `HasValue` overloads:

```java
// The signal is mirrored to the client on every change; the write reads the
// mirrored value at click time. Backed by the existing SignalInput.
ClipboardBinding.writeText(Signal<String> value);
ClipboardBinding.writeText(Signal<String> value, SerializableConsumer<String> onCopied,
        SerializableConsumer<PromiseAction.Error> onError);
ClipboardContent.text(Signal<String> value);
```

A plain `SerializableSupplier<String>` overload would read closer to the
thread's helper, but it cannot work on its own: the supplier would have to run
on the server, and the gesture is gone by then. A signal is the version of that
idea that survives the gesture constraint, because its value is already on the
client when the click happens.

## `GridMenuItem` is not a `ClickNotifier`

**Where it bit us:** uc8 / CopyFromGridView.java — hit while trying a grid
context menu as the per-row affordance, before settling on the button the
forum thread actually asked for
**Symptom:** `Clipboard.onClick` requires `T extends Component &
ClickNotifier<?>`. `ContextMenu`'s `MenuItem` implements `ClickNotifier` (UC6
relies on it), but `GridContextMenu`'s `GridMenuItem` does not — it only
extends `MenuItemBase`. So the natural per-row copy affordance for a grid does
not compile:

```java
GridMenuItem<Customer> copyEmail = grid.addContextMenu().addItem("Copy email");
Clipboard.onClick(copyEmail).writeText(email); // does not compile
```

**Workaround used:** none in the final view. While exploring, wrapping the
item's label in a `Span` (an `HtmlContainer`, so a `ClickNotifier`) and
stretching it over the item with CSS does work.
**Suggested API:** make `GridMenuItem` implement
`ClickNotifier<GridMenuItem<T>>` for parity with `MenuItem` — a
`vaadin-grid-flow` change rather than a clipboard one. Failing that,
`Clipboard.onClick` could accept any `Component` and attach the DOM `click`
listener directly instead of going through `ClickNotifier`.

## Clipboard writes cannot be asserted in browserless tests

**Where it bit us:** every UC test under `src/test/java/com/example/uc*/`
**Symptom:** the write is a client-side trigger action bound to a click
(`Clipboard.onClick(button).writeText(...)`), so nothing about it is
observable from a browserless test: there is no way to fire the trigger, no
way to inspect what a component's bindings would write, and no fake clipboard
to assert against. `CopyFromGridViewTest` can check that every row renders its
own value and its own copy button, but not that the button is bound to that
value — a regression that bound every row to the first row's email would pass.
**Workaround used:** assert the rendered cell contents, and verify the actual
copy by driving the running app in a real browser.
**Suggested API:** a browserless simulator, e.g.

```java
// In browserless-test or a flow-server test-fixtures jar.
ClipboardSimulator clipboard = ClipboardSimulator.install();
test(copyButton).click();
assertEquals("ada.lovelace1@northwind.example", clipboard.lastWrittenText());
```

## Detecting clipboard availability

**Where it bit us:** dropped from the UC list — no view written
**Symptom:** there is no way to ask whether the clipboard is usable at all
(non-secure context, restrictive iframe permissions policy, denied
`clipboard-write` permission) before offering a copy affordance. No
`availabilityHintSignal()` or `ClipboardAvailability` type exists in this
build; the only signal is an `onError` callback firing after the user has
already clicked. In a grid this is per-row noise: 30 copy buttons that all
look enabled and all fail the same way.
**Workaround used:** none — the copy buttons are always offered, and a failure
is reported through `onError`.
**Suggested API:** a support/permission signal mirroring
`WebShare.supportSignal()` and `Fullscreen.stateSignal()`:

```java
Signal<ClipboardAvailability> Clipboard.availabilitySignal();
```
