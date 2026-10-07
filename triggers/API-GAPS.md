# Trigger / Action API — gaps surfaced by the use-case module

This module exercises the public-facing trigger API at
`com.vaadin.flow.component.trigger.internal.*` in mainline
`25.2-SNAPSHOT`. The classes there carry "For internal use only. May be
renamed or removed in a future release." — using them is supported but
the shapes are still evolving. This file records the points where the
module hit a wall and what the workaround looks like, so the upstream
PR has a feedback trail.

## Status

| Gap | State | See |
|---|---|---|
| No public `ShortcutTrigger` | **Open** — local shim | UC1, UC2 |
| No public `ClickAction` | **Open** — local shim | UC2 |
| No generic `PreventDefaultAction` | **Open** — local shim | UC11 |
| `Action.Input#toJs` not reachable outside `internal` | **Open** | UC13 |
| `HandlerInput` not public | **Open** | UC5, UC7 |
| `PropertyInput` doesn't accept a raw `Element` | **Open** | — *(no current UC; see history note below)* |
| Server-side feature detection | **Open** | — |
| Public introspection of trigger wiring | **Open** | all tests |
| Client-side test simulator | **Open** | all tests |

## No public `ShortcutTrigger`

**Where it bit us:** UC1 (Ctrl+S save), UC2 (Enter → submit + disable).
**Symptom:** Mainline ships no keyboard-shortcut Trigger subclass. The
existing `com.vaadin.flow.component.Shortcuts` framework (and
`ShortcutRegistration`) handles keyboard shortcuts for component focus,
but isn't a `Trigger` and so can't be wired to actions through
`trigger.triggers(...)`.
**Workaround used:** A small `com.example.ShortcutTrigger` subclassing
the public `Trigger`. Listens on `window` in capture phase (the only
reliable way to win against the browser's built-in shortcut handler,
e.g. Ctrl+S Save Page), filters by exact modifier match, matches the
key against both `event.key` and `event.code`, and calls
`preventDefault()` + `stopPropagation()` before fanning out to the
actions.
**Suggested API:** The feature branch
`vaadin/flow:feature/triggers-actions` already has a `ShortcutTrigger`
built on a `KeyboardEventTrigger` parent. Once that lands in mainline,
delete our local file and switch the imports.

## No public `ClickAction`

**Where it bit us:** UC2 (the Enter shortcut chains a synthetic click
on the Send button followed by `SetPropertyAction(disabled)`).
**Symptom:** No built-in action calls `target.click()`. Alternatives are
all unattractive — `SetPropertyAction(target, "click", true)` doesn't
work because `click` isn't an assignable property, and inlining the
submit logic into a `CallbackAction` would change the demo's teaching
point from "fire another button's logic" into "the shortcut handler
does the work".
**Workaround used:** A 30-line `com.example.ClickAction`. Delete + import
once it lands in mainline.

## No generic `PreventDefaultAction`

**Where it bit us:** UC11. A right-click `MouseEventTrigger("contextmenu")`
needs to suppress the browser's native context menu before the coordinate
callback is useful.
**Symptom:** The framework's only `preventDefault` story is the
chainable `KeyboardEventTrigger.preventDefault()` on the feature branch
(and that's keyboard-only). For other DOM events, applications have to
write their own action.
**Workaround used:** A 10-line `com.example.PreventDefaultAction` that
emits `event.preventDefault()`. Wired as the first action in UC11's
trigger.
**Suggested API:** Either promote a generic `PreventDefaultAction` to
the public surface or add a chainable builder method on `DomEventTrigger`
mirroring `KeyboardEventTrigger.preventDefault()`.

## `Action.Input#toJs` is package-private

**Where it bit us:** UC13 wanted a `FilterListAction` that took an
`Action.Input<String>` as the query source — the natural shape mirroring
`SetPropertyAction(target, name, source)`. But
`Action.Input#toJs(Trigger)` has `protected` visibility, and the
surrounding `Action` class lives in
`com.vaadin.flow.component.trigger.internal`, so application code in any
other package can't call it.
**Symptom:** `source.toJs(trigger)` from a custom Action outside the
`internal` package fails to compile with "toJs has protected access in
Input".
**Workaround used:** Drop the Input parameter entirely. UC13's
`FilterListAction` is hard-wired to read its query from
`event.target.value` and only works when bound to a `DomEventTrigger` on
the relevant search field.
**Suggested API:** Promote `Input#toJs` to `public` (it is already
public by intent — every `Action` calls it). Or give Input a public
render method that wraps `toJs` for external callers.

## `HandlerInput` is not public

**Where it bit us:** UC5 (`IdleTrigger`), UC7 (`BroadcastChannelTrigger`).
Both expose event-scoped state through static `Action.Input` fields and
would naturally use `HandlerInput<>("event.detail.idle",
IdleTrigger.class)` — the exact pattern the built-in triggers use (see
`SizeTrigger.EventData`, `MouseEventTrigger.EventData`).
**Symptom:** `HandlerInput` is in the `internal` package and
package-private. Custom Triggers in any other package fall back to
declaring an anonymous `Action.Input<T>` per field with an inline
trigger type check.
**Workaround used:** Anonymous classes (see `IdleTrigger.EventData.idle`,
`BroadcastChannelTrigger.EventData.data`). Each is ~10 lines instead of
one.
**Suggested API:** Make `HandlerInput` `public` so add-on triggers can
reuse the canonical implementation.

## `PropertyInput` doesn't accept a raw `Element`

**Where it bit us:** *(historical — a now-dropped UC wrapped a native
`<select>` in a 12-line `@Tag("select")` Component subclass just to get
a target for `PropertyInput`; the UC was dropped because the
click-and-copy pattern is covered by the high-level Clipboard API.)*
**Symptom:** A raw `new Element("select")` cannot be passed to
`PropertyInput`. The only constructor takes a `Component` target. The
workaround is to wrap the element in a tiny `Component` subclass.
**Suggested API:** Add a `PropertyInput(Element target, String name,
Class<T> type)` overload. The body is identical to the current Component
overload after the `.getElement()` call.

## No server-side feature-detection signal

**Where it bit us:** Not blocking any current UC, but relevant for any
share / file-system / payment / clipboard-read variants an application
might want to write.
**Symptom:** A trigger always installs; if the browser lacks the API the
action targets, the call fails silently in JS. There is no server-side
way to ask "is this gesture-gated API supported here?" so the app could
proactively hide the relevant control.
**Suggested API:** A `Feature.detect(host, "clipboard.write")` returning
a `Signal<Boolean>` resolved per session.

## No public introspection of trigger wiring

**Symptom:** An add-on that wants to inspect or augment a trigger's
bindings after the fact has no API. The new architecture installs
handlers via `Element#addJsInitializer`, which exposes no introspection
surface.
**Suggested API:** A read-only view on `Element#getJsInitializers()` (or
similar accessor) that returns the install expressions and their
captures.

## No client-side test simulator / introspection

**Where it bit us:** Every browserless test in this module.
**Symptom:** Tests can verify view rendering and server-side state
changes (e.g. "clicking Send disables the button server-side") but
cannot inspect what JavaScript was actually emitted by a Trigger or
Action. Functional verification of the trigger-action wire (clipboard
write happens, shortcut fires, dblclick copies, resize updates classes)
requires a real browser.
**Workaround used:** Browserless tests are pure render assertions;
Playwright is the source of truth for behaviour.
**Suggested API:** Either a `JsInitializerSnapshot` accessor on
`Element` exposing the installed expressions (for assertions on the
generated JS), or a higher-level `TriggerTestKit` that mocks the
gesture path so actions can be fired headlessly.
