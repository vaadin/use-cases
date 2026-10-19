# Trigger / Action API — API gaps

Where the trigger API in `com.vaadin.flow.component.trigger.internal` made a
use case in this module impossible or awkward. The classes there are marked
"For internal use only. May be renamed or removed in a future release.", so
the shapes are still changing.

## 1. No public `ShortcutTrigger`

**Where it bites:** UC1 / ShortcutSaveView.java, UC21 / ShortcutDownloadView.java, `ShortcutTrigger.java`
**Symptom:** the trigger package has no keyboard-shortcut `Trigger`.
`Shortcuts` and `ShortcutRegistration` handle keyboard shortcuts, but they
are not a `Trigger`, so they can not be wired to actions through
`Trigger#triggers(...)`.
**Workaround used:** a local `com.example.ShortcutTrigger` that extends
`Trigger`. It listens on `window` in the capture phase (the only reliable way
to win against the browser's own shortcut, e.g. Ctrl+S "Save Page"), requires
an exact modifier match, matches the key against both `event.key` and
`event.code`, and calls `preventDefault()` and `stopPropagation()` before it
runs the actions. Because the match is exact, UC1 and UC21 create a second
trigger for the Cmd key on a Mac.
**Suggested API:** a public
`ShortcutTrigger(Component host, Key key, KeyModifier... modifiers)`, with a
way to accept Ctrl on Windows and Linux and Cmd on a Mac in one trigger.

## 3. No generic `PreventDefaultAction`

**Where it bites:** UC11 / RightClickCoordsView.java
**Symptom:** a `MouseEventTrigger(panel, "contextmenu")` has to suppress the
browser's context menu. `DomEventTrigger` and `MouseEventTrigger` have no way
to call `event.preventDefault()`.
**Workaround used:** a local `com.example.uc11.PreventDefaultAction` that runs
`event.preventDefault()`, wired as the first action of the trigger.
**Suggested API:** a public `PreventDefaultAction`, or a chainable
`DomEventTrigger#preventDefault()`.

## 4. `PropertyInput` does not accept an `Element`

**Where it bites:** no current use case. A dropped use case wrapped a native
`<select>` in a small `@Tag("select")` component only to get a target for
`PropertyInput`.
**Symptom:** the only constructor is
`PropertyInput(Component target, String propertyName, Class<T> type)`, so a
plain `new Element("select")` can not be read.
**Workaround used:** wrap the element in a tiny `Component` subclass.
**Suggested API:** a
`PropertyInput(Element target, String propertyName, Class<T> type)` overload.
`Trigger` already has a constructor for an `Element` host.

## 5. No server-side feature detection

**Where it bites:** no current use case; it matters for share, file system,
payment or clipboard-read actions.
**Symptom:** a trigger always installs. If the browser lacks the API that an
action calls (e.g. `ShareAction`, `ReadFromClipboardAction`), the call fails
in the browser without a message. The server can not ask whether the API is
supported, so it can not hide the control.
**Workaround used:** none.
**Suggested API:** e.g. `Feature.detect(host, "clipboard.write")` that returns
a `Signal<Boolean>`, resolved per session.

## 6. No introspection of trigger wiring

**Where it bites:** no current use case; it matters for add-ons that build on
triggers.
**Symptom:** triggers install their handlers with
`Element#addJsInitializer`, which returns only a `Registration`. Code that
wants to inspect or extend a trigger's bindings later has no API to read them.
**Workaround used:** none.
**Suggested API:** a read-only accessor such as `Element#getJsInitializers()`
that returns the install expressions and their arguments.

## 7. No browserless simulator for triggers and actions

**Where it bites:** every browserless test of the module
**Symptom:** tests can check the rendered view and server-side state (e.g.
that a button is disabled on the server), but not the JavaScript that a
`Trigger` or `Action` installs. Whether the clipboard write happens, the
shortcut fires, a double click copies or a resize changes the classes can
only be checked in a real browser.
**Workaround used:** the tests' `com.example.TriggerCallbacks` invokes the
return channel that `CallbackAction`, `SetSignalAction` and the outcome of
`WriteToClipboardAction` register on the trigger's host, as the browser would.
That covers the server half of UC3, UC4, UC5, UC7, UC8, UC11, UC18 and UC23.
It relies on Flow internals (`ReturnChannelMap`). Views whose actions run only
in the browser are tested for what they render, and checked in a browser.
**Suggested API:** an accessor on `Element` for the installed expressions
(to assert on the generated JS), or a browserless tester that fires a trigger
so its actions run without a browser.

## 8. Properties written by an action are invisible to the server

**Where it bites:** UC24 / DisableButtonGroupView.java, UC17 / AtomicResetView.java
**Symptom:** `SetPropertyAction(sibling, "disabled", true)` disables the
sibling buttons at once, but the property is not synced back, so
`sibling.isEnabled()` stays `true`. When the slow work ends,
`sibling.setEnabled(true)` is then no change and sends nothing, so the
siblings would stay disabled in the browser. `Button#setDisableOnClick` does
not have this problem, because it also updates the enabled state on the
server, but it covers only the clicked button.
The same happens to `SetPropertyAction(textField, "value", "")` in UC17: a
`TextField` synchronizes `value` on the `change` event by default, and a
script write does not fire it, so the server keeps the old value.
**Workaround used:** in UC24, the click listener sets a `processing` signal,
and an effect mirrors it onto the enabled state of all three buttons.
Clearing the signal later is a real change that reaches the browser through
Push. The clicked button still uses `setDisableOnClick(true)`. In UC17, a local
`com.example.uc17.FireChangeAction` fires `change` on each text field after
the writes, so the fields send their new value.
**Suggested API:** a `SetEnabledAction(HasEnabled...)` that disables in the
browser and marks the components disabled on the server when the event
arrives, or a `Button#setDisableOnClick(HasEnabled... alsoDisable)` overload.

## 9. `bindEnabled` can not be combined with disable-on-click

**Where it bites:** UC24 / DisableButtonGroupView.java
**Symptom:** each button is disabled on click, and it is also a sibling that
the other buttons disable. `Button#bindEnabled(Signal)` throws
`IllegalStateException` when disable-on-click is active, and
`Button#setDisableOnClick` throws when the enabled state is bound to a
signal. So the group can not use the signal binding that the rest of Flow
offers.
**Workaround used:** a `Signal.effect(view, ...)` that calls `setEnabled(...)`
on every button of the group.
**Suggested API:** let disable-on-click work on top of a bound enabled state,
e.g. disable only until the next update of the bound signal, so that
`bindEnabled(processing.map(p -> !p))` and `setDisableOnClick(true)` work
together.

## 10. No keyboard `Trigger`

**Where it bites:** UC22 / KeyEventLogView.java, `uc22/KeyboardEventTrigger.java`
**Symptom:** `DomEventTrigger(host, "keydown")` listens on the host element,
so it sees a key press only while the focus is inside the host. A view with
nothing focused never receives it. `DomEventTrigger` also has no `EventData`
inputs for `key`, `code` or the modifier flags.
**Workaround used:** a local `com.example.uc22.KeyboardEventTrigger` that
listens for `keydown` on `window` in the capture phase while the host is
attached, with `HandlerInput`s for `key`, `code`, `shiftKey`, `ctrlKey`,
`altKey` and `metaKey`.
**Suggested API:** a public `KeyboardEventTrigger(Component host)` with an
option to listen on the whole page, and `EventData` inputs like those of
`MouseEventTrigger`.

## 11. No trigger for a key sequence

**Where it bites:** UC23 / KonamiCodeView.java, `uc23/SequenceTrigger.java`
**Symptom:** reacting to an ordered sequence of keys (an Easter egg, a
"g then i" navigation shortcut) needs the keys pressed so far to be kept in
the browser. No trigger in `com.vaadin.flow.component.trigger.internal` keeps
state across events, and a server-side keydown listener would send every key
to the server.
**Workaround used:** a local `com.example.uc23.SequenceTrigger` that keeps the
last keys pressed in the browser and fires when they match the sequence.
**Suggested API:** a public `SequenceTrigger(Component host, Key... keys)`,
with an optional timeout between keys.
