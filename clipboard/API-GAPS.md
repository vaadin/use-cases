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
`Clipboard.writeText()` returns an unbound `ClientAction` instead: whatever
renders the affordance binds it, such as `LitRenderer.withClientAction`, and the
action copies the first argument the template calls it with. That entry point
is not released yet (vaadin/flow#25366, vaadin/flow-components#10470).
`Clipboard.onPaste` and `Clipboard.onFilePaste` observe paste events.

Most entries come from UC8, the copy button in every grid row from the
[forum thread](https://vaadin.com/forum/t/clipboard-copy/164697/11). The use
case works, and simply: a `LitRenderer` column whose template calls
`copy(item.email)`, with `withClientAction("copy", Clipboard.writeText())`
binding the write once for the whole column. The gaps are about what that
client action can not do yet.

## 1. No copy feedback from a client action

**Where it bites:** UC8 / CopyFromGridView.java
**Symptom:** `Clipboard.writeText()` returns a `ClientAction` with no
`onCopied` / `onError` callbacks, unlike
`ClipboardBinding.writeText(value, onCopied, onError)`. The copy buttons in the
grid give no confirmation that anything was copied, and a rejected write (a
non-secure context, a denied permission) fails silently. The template can call
a `withFunction(...)` callback next to `copy(...)`, but that runs whether the
write succeeds or not.
**Workaround used:** none. UC8 shows no notification after a copy.
**Suggested API:** the callbacks `ClipboardBinding` already has, on the unbound
action:

```java
ClientAction Clipboard.writeText(SerializableConsumer<String> onCopied,
        SerializableConsumer<PromiseAction.Error> onError);
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
**Workaround used:** none in UC8, whose template passes the row's value to
the client action. A shared affordance outside the grid would
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

## 4. No browserless simulator for clipboard writes

**Where it bites:** every test under `src/test/java/com/example/uc*/`
**Symptom:** a write is a client-side action, bound to a click
(`Clipboard.onClick(button).writeText(...)`) or called from a template
(`Clipboard.writeText()` in UC8). A browserless test can not fire it, inspect
what it would write, or assert against a fake clipboard. `CopyFromGridViewTest`
checks that every row renders its own email, but not that the button copies
it: a template that called `copy` with the wrong property would pass.
**Workaround used:** the tests assert the rendered cells; the copy itself is
checked by hand in a real browser.
**Suggested API:** a simulator in browserless-test, following
`GeolocationSimulator`:

```java
ClipboardSimulator clipboard = ClipboardSimulator.install();
test(copyButton).click();
assertEquals("ada.lovelace1@northwind.example", clipboard.lastWrittenText());
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
