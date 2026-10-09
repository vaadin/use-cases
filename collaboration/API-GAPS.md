# API gaps — collaboration on shared signals

What this module could not do with the signal API as it stands in Vaadin 25.3,
or could only do by writing something the framework should arguably own. The
reference point is Collaboration Kit, because UC1–UC9 are the
[Collaboration Engine Sampler](https://ce-sampler.demo.vaadin.com/) rebuilt on
signals and the comparison is therefore exact.

Every numbered item below is referenced from the code that hit it, and the
behavioural claims are pinned down by
[`SharedSignalSemanticsTest`](src/test/java/com/example/collab/SharedSignalSemanticsTest.java)
so that a framework change makes a test fail rather than making this document
quietly wrong.

## Parity blockers

**1. Nothing removes a peer that disappears.**
Collaboration Kit has `EntryScope.CONNECTION`: entries written by a connection
are dropped when that connection ends, and `BeaconHandler` ends it when the
browser closes the tab. A shared signal has no such notion, so every presence
use case here removes its own entry from an attach/detach listener
([`Presence#bind`](src/main/java/com/example/collab/Presence.java)) — and that
only covers the cases where a detach happens. A crashed server, a killed
session or an exception on the way out leaves the peer in the map for the
lifetime of the application.

The interesting part is that the protocol already supports it:
`SignalCommand.InsertCommand` carries a `scopeOwner()`, and
`SignalCommand.ClearOwnerCommand` exists to clear everything owned by one
owner. No public API sets an owner or clears one, so the capability is present
in the wire format and unreachable from an application.

*Wanted*: an insert/put that takes an owner, and a way to end an owner's
scope — ideally tied to the session by default, as Collaboration Kit does.

**2. Shared signals are single-node.**
Collaboration Kit ships a `Backend` abstraction with Hazelcast and Redis
implementations, so a collaborative view survives being deployed behind two
instances. Shared signals keep their tree in the JVM that created it. Nothing
in this module works across two servers, which is the one gap that would stop
an application from replacing Collaboration Kit today.

The API is clearly built with this in mind — the transaction restriction in #15
is phrased in terms of cluster nodes — but there is no way to plug a backend
in.

**3. Nothing expires.**
`HasExpirationTimeout` lets a CE topic drop its data after a period with nobody
in it. A `SharedNodeSignal` keeps every child until something removes it, so
UC12's room tree grows forever; the view has a *Close empty rooms* button
because the alternative was a leak
([`RoomsTopic#closeEmptyRooms`](src/main/java/com/example/uc12/RoomsTopic.java)).
A demo can press a button. A chat with per-conversation rooms cannot.

**4. No persistence hook.**
`CollaborationMessagePersister` gives CE a load callback and a write-through
callback, so a chat's history survives a restart without the view knowing.
There is no signal-side equivalent: UC4 writes to its store and to the signal
separately and reloads the signal from the store by hand
([`ChatTopic`](src/main/java/com/example/uc4/ChatTopic.java)). Two writes that
have to be kept in step is exactly the class of bug the rest of the signal API
is good at preventing.

## Component bindings

**5. `Grid` has no signal-aware items API.**
`AvatarGroup.bindItems` and `MessageList.bindItems` take a
`Signal<List<Signal<T>>>` and are a pleasure to use — UC1 and UC4 are three
lines each because of them. `Grid` takes a data provider, so UC8 and UC9 push
the shared list into a `ListDataProvider` from an effect
([`SignalBindings`](src/main/java/com/example/collab/SignalBindings.java)).
Two details make it more than boilerplate:

- the grid's items have to be the row *signals* rather than the row values, or
  every keystroke replaces every item and selection is lost;
- the effect has to read each row as well as the list, or an edit inside a row
  never reaches the grid.

`Grid.setPartNameGenerator` compounds it: a part name generator is not
reactive, so a view that colours rows by who is looking at them has to call
`refreshAll()` from an effect of its own.

**6. The field highlighter has no public Java API.**
`vaadin-field-highlighter` ships with the platform and is what makes UC5 look
like Collaboration Kit, but the only Java surface is
`FieldHighlighterInitializer.init(Element)` — `protected static`. Using it
means extending that class for its `@JsModule` and `@NpmPackage` annotations
and calling `setUsers` over `executeJs`
([`SignalFieldHighlighter`](src/main/java/com/example/collab/SignalFieldHighlighter.java)).
CE's `FieldHighlighter` is not public either, but CE at least exposes it
through `CollaborationBinder`; on signals there is nothing.

**7. No `Binder` integration.**
`CollaborationBinder` is a `Binder`, so a CE form keeps its validators,
converters, bean binding and `writeBean`. A signal binding and a `Binder` both
want to own a field's value, so a collaborative form on signals gives up the
`Binder` half — UC5's e-mail check is an effect that calls `setInvalid`
([`CollaborativeFormView`](src/main/java/com/example/uc5/CollaborativeFormView.java)).
For a form of any size this is the most expensive gap in the list.

**8. Validity is not bindable.**
`HasValue` binds value, read-only state and the required indicator to signals,
but not validity: there is no `bindInvalid` or `bindErrorMessage`, so a
validation result derived from shared state has to be applied imperatively from
inside an effect.

**9. Shared state carries no actor, and effects are not change events.**
`SignalBinding.onChange` does report the old and the new value, which is more
than an effect can see and enough for UC6's "name: Ada → Ada B". What no API
reports is *who* wrote — so `FieldValue` carries its author as payload
([`FieldValue`](src/main/java/com/example/collab/FieldValue.java)) and every
application that wants "Ada changed the address" pays for it in its data model.

The same shape appears for collections. CE's `PresenceHandler` is called per
user with a callback for that user leaving, so join and leave fall out of the
API; an effect over a presence map is called with the current set, so UC3 and
UC6 keep the previously rendered set and diff against it. It works, it is
about fifteen lines per view, and every view writes it again.

**10. A `SignalBinding` cannot be undone.**
`bindValue`, `bindText` and friends return a `SignalBinding`, which is not a
`Registration` and has no `remove()`. A binding therefore lives as long as its
component, which is usually what you want and occasionally not — a field that
is re-bound to a different signal (UC12 switching rooms) has to be replaced
with a new component instead.

## Semantics worth documenting

**11. `CommandValidator` does not know who is writing, and refuses by
throwing.**
`SignalCommand` exposes the command id, the target node and — for the value
commands — the value, which is enough to enforce rules about the *data*
(UC13 refuses labels over twelve characters in the shared state itself, which
Collaboration Kit cannot do at all). It carries no principal, so an access rule
about *users* can only be written by closing over the user, as
[`AccessTopic#formFor`](src/main/java/com/example/uc7/AccessTopic.java) does.
That makes a validator a capability handed to one peer rather than a rule on
the data: a second handle to the same node, without the validator, writes
freely.

The refusal is also shaped differently from every other refusal in the API. A
failed condition comes back as `SignalOperation.Error` on the operation; a
validator that returns `false` makes `submit` throw
`UnsupportedOperationException` with no message. A view that wants to report
"you may not do that" has to catch an exception in one case and inspect an
operation result in the other.

**12. `GridPro` does not say which cell is being edited.**
`CellEditStartedEvent` exposes `getItem()` and nothing else, and there is no
cell-edit-finished event, so UC9 can highlight the row a peer is editing but
not the cell — which is what Collaboration Kit's grid integration shows. The
event is constructed with a path internally; it is simply not exposed.

**13. A pending write cannot be kept pending.**
`peekConfirmed()` and `get()` do genuinely differ, and UC13 shows it: inside
the callback that called `set()`, this client's reading has moved on and the
confirmed reading has not, because the command is applied when the session
flushes. But the window closes before the response is rendered, so a UI cannot
*show* a write as in flight, grey out an unconfirmed row, or offer to retry
one. Nor can a test simulate a slow or failed confirmation, since the only
publicly constructible tree is the synchronous one. Optimistic UI is the use
case `peekConfirmed()` exists for and the one thing it cannot be used for.

**14. An operation's result is not available to the code that submitted it.**
`operation.result().getNow(null)` returns `null` in every UI callback, for the
same reason as #13 — the command has not been applied yet. It is a quiet trap:
the first version of UC7 read it there and reported "not confirmed yet" for
every claim, winning or losing. The correct form is a future callback plus
`ui.access`, which every call site has to remember
([`Outcomes`](src/main/java/com/example/collab/Outcomes.java)).

*Wanted*: something like `operation.onOutcome(Component, consumer)` that
handles the session hop, in the spirit of `Signal.effect(Component, …)`.

**15. A transaction cannot span independent shared signals.**
`Signal.runInTransaction` across two top-level shared signals throws, because
each one commits separately and may be owned by a different cluster node. The
message says so plainly and recommends keeping the values in one shared signal.
That is sound, and it is a constraint on the *data model* rather than on the
call: UC10 moves an item between two lists atomically, and its two lists are
children of one `SharedNodeSignal` for that reason alone
([`RaceTopic`](src/main/java/com/example/uc10/RaceTopic.java)). An application
that discovers this after modelling its state as several top-level signals has
to restructure.

**16. Smaller edges.**

- `SharedListSignal` has no remove-by-value: removing an entry means
  `peek().stream().filter(…).findFirst().ifPresent(list::remove)`, because
  `remove` takes the entry's signal
  ([`FormState#leave`](src/main/java/com/example/collab/FormState.java)).
- `SharedListSignal` has `getValues()` / `peekValues()`; `SharedMapSignal` has
  no equivalent, so its values are unwrapped by hand.
- `SharedNumberSignal.getAsInt()` goes through `get()` and therefore needs a
  reactive context; there is no `peekAsInt()`, so reading a counter outside an
  effect means `peek().intValue()`.
- `SharedMapSignal.get()` returns the entries in an unspecified order, so any
  view that renders them sorts by something of its own first — otherwise two
  clients can list the same users differently.

## Not a gap

Worth recording, since they were expected to be problems and were not:

- **Atomicity.** `replace`, `incrementBy`, `update` and `verifyChild` cover the
  conflict cases Collaboration Kit's last-write-wins map cannot express, and
  they report rejection rather than losing a write silently (UC10).
- **Derived state.** `Signal.computed` removes the hand-maintained counters a
  CE application needs beside its data (UC11).
- **Topics.** A `SharedNodeSignal` with `putChildIfAbsent` is a better topic
  namespace than a topic id, because the list of rooms is itself shared state a
  client can render (UC12).
- **Subscription lifecycle.** `Signal.effect(Component, …)` replaces
  `ConnectionContext` / `TopicConnection` entirely, and is less to get wrong.
