# API gaps: collaboration on shared signals

What this module could not do with the signal API in Vaadin 25.4, or could do
only by writing code the framework should own. The reference point is
Collaboration Kit (CE): UC1–UC9 rebuild the
[Collaboration Engine Sampler](https://ce-sampler.demo.vaadin.com/) on
signals, so the comparison is exact.

The code that hits a gap refers to it as `API-GAPS.md #N`. The behaviour
claims are pinned down by
[`SharedSignalSemanticsTest`](src/test/java/com/example/collab/SharedSignalSemanticsTest.java),
so a framework change makes a test fail instead of making this file wrong.

## 1. Nothing removes a peer that disappears

**Where it bites:** UC1 / `PresenceView.java`, UC2 / `OptInPresenceView.java`, helper `Presence#bind`
**Symptom:** CE has `EntryScope.CONNECTION`: entries a connection wrote are
dropped when the connection ends, and `BeaconHandler` ends it when the tab
closes. Shared signals have no such scope. A killed tab, a killed session or a
crashed server leaves the peer in the map. The protocol already supports it:
`SignalCommand.InsertCommand` and `PutIfAbsentCommand` carry a `scopeOwner()`,
and `SignalCommand.ClearOwnerCommand` clears everything one owner wrote. But
every public insert/put passes `null` as the owner, and nothing public sends
`ClearOwnerCommand`.
**Workaround used:** `Presence#bind` removes the entry in the cleanup of a
`whenAttached` handler. For a vanished browser this relies on the UI being
closed after three missed heartbeats. With the 15-second interval set in
`application.properties`, a ghost avatar stays for up to about a minute; at
Vaadin's default interval it would stay for about 15 minutes.
**Suggested API:** an insert/put that takes an owner, and a way to end an
owner's scope, by default tied to the session or UI.
Upstream: https://github.com/vaadin/flow/issues/25867

## 2. Shared signals are single-node

**Where it bites:** every use case
**Symptom:** CE has a `Backend` abstraction with Hazelcast and Redis
implementations. A shared signal keeps its tree in the JVM that created it:
the public constructors (e.g. `SharedNodeSignal()`) use
`LocalAsynchronousSignalTree`, documented as "does not support clustering".
Nothing in this module works across two servers. This is the one gap that
stops an application from replacing CE today.
**Workaround used:** none. The module runs on a single node.
**Suggested API:** a pluggable cluster backend for the signal tree. The API
already expects one: the transaction error in #15 talks about cluster nodes.

## 3. Nothing expires

**Where it bites:** UC12 / `RoomsView.java`, `RoomsTopic#closeEmptyRooms`
**Symptom:** CE's `HasExpirationTimeout` drops a topic's data after a period
with nobody connected. A `SharedNodeSignal` keeps every child until something
removes it, so UC12's room tree grows forever.
**Workaround used:** a *Close empty rooms* button that calls
`RoomsTopic#closeEmptyRooms`. A demo can have a button; a chat with
per-conversation rooms cannot.
**Suggested API:** resolve a group of shared signals by key, and evict it
after a configurable idle time.
Upstream: https://github.com/vaadin/flow/issues/25865

## 4. No persistence hook

**Where it bites:** UC4 / `ChatView.java`, `ChatTopic.java`
**Symptom:** CE's `CollaborationMessagePersister` has a load callback and a
write-through callback, so chat history survives a restart. Signals have no
equivalent. The application must write to its store and to the signal
separately, and keep the two in step.
**Workaround used:** `ChatTopic` writes to both, and rebuilds the signal from
the store by hand.
**Suggested API:** a load and write-through hook on a shared signal, so the
store and the signal cannot drift apart.
Upstream: https://github.com/vaadin/flow/issues/25864

## 5. `Grid` has no signal-aware items API

**Where it bites:** UC8 / `CollaborativeGridView.java`, UC9 / `CollaborativeGridProView.java`, helper `SignalBindings`
**Symptom:** `AvatarGroup.bindItems` and `MessageList.bindItems` take a
`Signal<List<Signal<T>>>`, so UC1 and UC4 need three lines each. `Grid` takes
only a data provider. Two details make the glue more than boilerplate:

- the grid items must be the row *signals*, not the row values; otherwise
  every keystroke replaces every item and the selection is lost;
- the effect must read each row as well as the list; otherwise an edit inside
  a row never reaches the grid.

`Grid.setPartNameGenerator` is not reactive either, so a view that colours
rows by who is looking at them must call `refreshAll()` from its own effect.
(A `Grid.bindItems` existed briefly and was removed again in
vaadin/flow-components#8954.)
**Workaround used:** `SignalBindings` pushes the shared list into a
`ListDataProvider` from an effect; the views call
`getDataProvider().refreshAll()` from an effect for part names.
**Suggested API:** `Grid.bindItems(Signal<List<Signal<T>>>)` that refreshes
one row when one row signal changes, and a reactive part name generator.

## 6. The field highlighter has no public Java API

**Where it bites:** UC5 / `CollaborativeFormView.java`, helper `SignalFieldHighlighter`
**Symptom:** `vaadin-field-highlighter` ships with the platform, but the only
Java surface is `FieldHighlighterInitializer.init(Element)`, which is
`protected static`. CE's `FieldHighlighter` is not public either, but CE
exposes it through `CollaborationBinder`. On signals there is nothing. The
web component also only flashes a user's name tag for two seconds and then
shows it on hover; there is no option to keep it visible while the outline is.
**Workaround used:** `SignalFieldHighlighter` extends
`FieldHighlighterInitializer` to inherit its `@JsModule` and `@NpmPackage`,
and calls `setUsers` through `executeJs`. To keep the name tags visible it
opens them itself and patches the private tags element so they stay open
while anybody is in the field. Browserless tests cannot see any of this,
because it runs in the browser and there is no field highlighter tester, so
the pinned tags were checked by hand in a browser.
**Suggested API:** a public `FieldHighlighter` with e.g.
`bindUsers(HasValue<?, ?> field, Signal<List<FieldHighlighterUser>>)`, and
an option to show the name tags for as long as the highlight, and a tester
that reports the users a field is highlighted for.

## 7. No `Binder` integration

**Where it bites:** UC5 / `CollaborativeFormView.java`, helper `FormState`
**Symptom:** `CollaborationBinder` is a `Binder`, so a CE form keeps its
validators, converters, bean binding and `writeBean`. `Binder` can now expose
signals (`Binding.valueSignal()`, `validationStatusSignal()`), but it cannot
be backed by a shared signal: `bindValue` and `Binder` both want to own the
field value. A collaborative form on signals gives up `Binder`. For a form of
any size this is the most expensive gap here.
**Workaround used:** `FormState` binds each field with `bindValue`; UC5's
e-mail check is an effect that calls `setInvalid`.
**Suggested API:** a `Binder` whose bindings read and write a shared signal
(per property or per bean), keeping validators and converters.
Upstream: https://github.com/vaadin/flow/issues/23868

## 8. Validity is not bindable

**Where it bites:** UC5 / `CollaborativeFormView.java`
**Symptom:** `HasValue` binds value, read-only state and the required
indicator (`bindValue`, `bindReadOnly`, `bindRequiredIndicatorVisible`), but
`HasValidation` has no `bindInvalid` or `bindErrorMessage`. They were added in
vaadin/flow#23258 and reverted in vaadin/flow#23681.
**Workaround used:** an effect calls `setInvalid` and `setErrorMessage`.
**Suggested API:** `HasValidation.bindInvalid(Signal<Boolean>)` and
`bindErrorMessage(Signal<String>)`.

## 9. Shared state carries no actor, and effects are not change events

**Where it bites:** UC3 / `CustomUserListView.java`, UC6 / `FormEventsView.java`, helper `FieldValue`
**Symptom:** `SignalBinding.onChange` reports the old and new value, which is
enough for UC6's "name: Ada → Ada B". No API reports *who* wrote. For
collections, CE's `PresenceHandler` is called per user with a callback for
that user leaving. An effect over a presence map only sees the current set.
**Workaround used:** `FieldValue` carries its author as payload. UC3 and UC6
keep the last rendered set and diff against it, about fifteen lines per view.
**Suggested API:** the writer's identity on a change (e.g. in
`EffectContext` or the binding context), and per-entry added/removed
callbacks for `SharedMapSignal` and `SharedListSignal`.

## 10. A `SignalBinding` cannot be undone

**Where it bites:** UC12 / `RoomsView.java`, helper `FormState`
**Symptom:** `bindValue`, `bindText` and similar return a `SignalBinding`.
It is not a `Registration` and has no `remove()`, and bindings require a
non-null signal. A binding lives as long as its component. A field that must
switch to another signal (UC12 changing rooms) cannot be re-bound.
**Workaround used:** replace the component with a new one.
**Suggested API:** `SignalBinding.remove()`, or allow re-binding the same
property to a different signal.

## 11. `CommandValidator` does not know who writes, and refuses by throwing

**Where it bites:** UC7 / `FormAccessView.java`, `AccessTopic#formFor`; UC13 / `PendingStateView.java`, `PendingTopic.java`
**Symptom:** `CommandValidator.isValid(SignalCommand)` sees the command,
target node and value. That is enough for rules about *data* (UC13 refuses
labels over 12 characters in the shared state, which CE cannot do). It has no
principal, so a rule about *users* must close over the user. That makes the
validator a capability of one handle, not a rule on the data: another handle
to the same node without the validator writes freely. A refusal is also
shaped differently from other refusals: a failed condition gives a
`SignalOperation.Error`, but a validator returning `false` makes `submit`
throw `UnsupportedOperationException` with no message
(`AbstractSharedSignal#submit`).
**Workaround used:** `AccessTopic#formFor` wraps the signals with
`withValidator` per peer; UC7 and UC13 catch the exception.
**Suggested API:** a validator that receives the writing session or user, and
a refusal reported as a failed operation result with a reason.

## 12. `GridPro` does not say which cell is being edited

**Where it bites:** UC9 / `CollaborativeGridProView.java`, `CatalogTopic.java`
**Symptom:** `GridPro.CellEditStartedEvent` exposes only `getItem()`, and
there is no cell-edit-finished event. UC9 can highlight the row a peer edits,
not the cell, which CE's grid integration shows. The event is constructed
with the column path internally; it is just not exposed.
**Workaround used:** row-level highlighting only.
**Suggested API:** `CellEditStartedEvent.getColumn()` (or the path), and a
`CellEditFinishedEvent`.

## 13. A pending write cannot be kept pending

**Where it bites:** UC13 / `PendingStateView.java`
**Symptom:** `peekConfirmed()` and `get()` do differ: inside the callback
that called `set()`, this client sees the new value and the confirmed reading
does not, because the command is applied when the session flushes. But the
window closes before the response is rendered, so a UI cannot show a write as
in flight, grey out an unconfirmed row, or offer a retry. A test cannot
simulate a slow or failed confirmation either: the public constructors use
`LocalAsynchronousSignalTree`, and the constructors that take another tree
are `protected`. Optimistic UI is what `peekConfirmed()` exists for, and the
one thing it cannot be used for.
**Workaround used:** none. UC13 sets *Saving…* in the callback, which a user
never sees, and the outcome when the operation completes.
**Suggested API:** a way to observe pending commands from the UI, and a
test tree whose confirmation can be delayed or rejected.

## 14. An operation's result is not available to the code that submitted it

**Where it bites:** UC7 / `FormAccessView.java`, UC13 / `PendingStateView.java`, helper `Outcomes`
**Symptom:** `operation.result().getNow(null)` returns `null` in every UI
callback, for the same reason as #13. It is a quiet trap: the first version
of UC7 read it there and reported "not confirmed yet" for every claim.
**Workaround used:** `Outcomes.report` uses a future callback plus
`ui.access`, which every call site would otherwise repeat.
**Suggested API:** e.g. `operation.onOutcome(Component, consumer)` that
handles the hop back to the UI, like `Signal.effect(Component, …)`.

## 15. A transaction cannot span independent shared signals

**Where it bites:** any model that keeps related collections in separate shared signals; pinned in `SharedSignalSemanticsTest`
**Symptom:** `Signal.runInTransaction` across two top-level shared signals
throws, because each one commits separately and "may be owned by a different
cluster node" (`StagedTransaction`). The message says to keep the values in
one shared signal. That is a constraint on the *data model*: moving an item
between two lists atomically needs both lists to be children of one
`SharedNodeSignal`. An application that has already modelled its state as
several top-level signals must restructure.
**Workaround used:** keep collections that change together under one
`SharedNodeSignal`.
**Suggested API:** document the rule on `SharedNodeSignal` and
`runInTransaction`, or support transactions across trees.

## 16. Smaller edges

**Where it bites:** UC5 / helper `FormState#leave`; any view rendering a `SharedMapSignal` or counter
**Symptom:**

- `SharedListSignal.remove` takes the entry's signal; there is no
  remove-by-value.
- `SharedListSignal` has `getValues()` / `peekValues()`; `SharedMapSignal`
  has no equivalent.
- `SharedNumberSignal.getAsInt()` calls `get()` and so needs a reactive
  context; there is no `peekAsInt()`.
- `SharedMapSignal` does not document the order of its entries. The
  implementation keeps insertion order, but nothing promises it.

**Workaround used:**
`peek().stream().filter(…).findFirst().ifPresent(list::remove)`; unwrapping
map values by hand; `peek().intValue()`; views sort map entries by a key of
their own before rendering.
**Suggested API:** `SharedListSignal.removeIf(Predicate<T>)`,
`SharedMapSignal.getValues()` / `peekValues()`,
`SharedNumberSignal.peekAsInt()`, and a documented entry order.

## Not a gap

Expected to be problems, but were not:

- **Atomicity.** `replace`, `incrementBy`, `update` and `verifyChild` cover
  conflicts that CE's last-write-wins map cannot express, and they report a
  rejection instead of losing a write (UC10 saves with `replace`).
- **Derived state.** `Signal.computed` removes the hand-kept counters a CE
  application needs beside its data (UC11).
- **Topics.** A `SharedNodeSignal` with `putChildIfAbsent` is a better topic
  namespace than a topic id, because the room list is itself shared state
  (UC12).
- **Subscription lifecycle.** `Signal.effect(Component, …)` replaces
  `ConnectionContext` / `TopicConnection`, with less to get wrong.
