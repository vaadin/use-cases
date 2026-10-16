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
runs the actions.
**Suggested API:** a public
`ShortcutTrigger(Component host, Key key, KeyModifier... modifiers)`, like the
one built on `KeyboardEventTrigger` on the `feature/triggers-actions` branch
of vaadin/flow.

## 2. No public `ClickAction`

**Where it bites:** no current use case; `ClickAction.java` is kept from an
earlier version of UC2, where an Enter shortcut clicked the Send button.
**Symptom:** no built-in `Action` calls `target.click()`.
`SetPropertyAction(target, "click", true)` does not work, because `click` is
not a property. A `CallbackAction` that runs the button's logic on the server
is a different thing: it does not fire the other button's own click handling.
**Workaround used:** a local `com.example.ClickAction` that runs
`$0.click()` on the target element.
**Suggested API:** a public `ClickAction(Component target)` /
`ClickAction(Element target)`.

## 3. No generic `PreventDefaultAction`

**Where it bites:** UC11 / RightClickCoordsView.java
**Symptom:** a `MouseEventTrigger(panel, "contextmenu")` has to suppress the
browser's context menu. `DomEventTrigger` and `MouseEventTrigger` have no way
to call `event.preventDefault()`.
**Workaround used:** a local `com.example.PreventDefaultAction` that runs
`event.preventDefault()`, wired as the first action of the trigger.
**Suggested API:** a public `PreventDefaultAction`, or a chainable
`DomEventTrigger#preventDefault()` like `KeyboardEventTrigger#preventDefault()`
on the `feature/triggers-actions` branch.

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
**Workaround used:** the browserless tests only check what is rendered; the
behaviour is checked in a browser.
**Suggested API:** an accessor on `Element` for the installed expressions
(to assert on the generated JS), or a browserless tester that fires a trigger
so its actions run without a browser.

## 8. Properties written by an action are invisible to the server

**Where it bites:** UC24 / DisableButtonGroupView.java
**Symptom:** `SetPropertyAction(sibling, "disabled", true)` disables the
sibling buttons at once, but the property is not synced back, so
`sibling.isEnabled()` stays `true`. When the slow work ends,
`sibling.setEnabled(true)` is then no change and sends nothing, so the
siblings would stay disabled in the browser. `Button#setDisableOnClick` does
not have this problem, because it also updates the enabled state on the
server, but it covers only the clicked button.
**Workaround used:** the click listener sets a `processing` signal, and an
effect mirrors it onto the enabled state of all three buttons. Clearing the
signal later is a real change that reaches the browser through Push. The
clicked button still uses `setDisableOnClick(true)`.
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
