# Collaboration use cases — plan

A proposal for a new `collaboration` use-cases module: the
[Collaboration Engine Sampler](https://ce-sampler.demo.vaadin.com/) rebuilt on
**shared signals only**, with no Collaboration Kit dependency.

The point is not a nicer sampler. Collaboration Kit is a commercial add-on
built on its own topic/connection/backend machinery; shared signals
(`com.vaadin.flow.signals.shared`) are core Flow and cover much of the same
ground. This module answers, view by view, **what a developer has to write
today to get a CE feature out of plain signals, and where they cannot get it
at all** — the second half being the module's real output, in the tradition of
`observability/API-GAPS.md`.

## What the CE sampler actually contains

Nine samples (read off the running app, `aria-setsize="9"`), each one a
description plus the source of an `AbstractSample`:

- `advanced-managers` — *Manage form editing.* `PresenceManager` +
  `FormManager` combined into a form with controlled write access.
- `avatar-group` — *Show active users.* `CollaborationAvatarGroup`, an
  `AvatarGroup` that tracks who is present in a topic.
- `chat` — *Build a real-time chat.* `CollaborationMessageList` +
  `CollaborationMessageInput`.
- `collaboration-binder` — *Work on forms together.* `CollaborationBinder`:
  value synchronization plus field highlighting on top of `Binder`.
- `custom-avatar-group` — *Custom avatar group.* `PresenceManager` with
  programmatic join/leave (a checkbox), rendering a plain `AvatarGroup`.
- `custom-form` — *Custom form.* `FormManager` with
  `setPropertyChangeHandler` / `setHighlightHandler` writing to a log panel.
- `custom-user-list` — *Custom active user list.* `PresenceManager`'s presence
  handler driving a hand-built list of avatars.
- `grid` — *Collaborative Grid.*
- `grid-pro` — *Collaborative Grid Pro.* Cell editing between users.

Worth copying: **every sample runs several simulated users side by side in one
page.** An *Add user* button adds a panel; each panel has its own header
(name, avatar, a toggle that disconnects that user) and its own instance of the
sample. No second browser tab is needed to see collaboration happen — which is
also what makes the whole thing testable under this repo's browserless test
convention.

## CE concept → shared-signal equivalent

What the mapping looks like against the signal API actually present in
`flow-server` 25.3 (`SharedValueSignal`, `SharedListSignal`, `SharedMapSignal`,
`SharedNumberSignal`, `SharedNodeSignal`, plus `Signal.effect` / `computed` /
`runInTransaction`):

| Collaboration Kit | Shared signals |
| --- | --- |
| `CollaborationMap` | `SharedMapSignal<T>` — `put`, `putIfAbsent`, `remove`, `verifyHasKey` |
| `CollaborationList` | `SharedListSignal<T>` — `insertLast`, `insertAt`, `moveTo`, `remove` |
| `Topic` / `topicId` | a `SharedNodeSignal` child per topic (`putChildIfAbsent`, then `asMap`/`asList`) |
| `TopicConnection`, `ConnectionContext` | `Signal.effect(component, …)` — subscription tied to the component's attach state |
| `MapSubscriber`, `ListSubscriber`, `PropertyChangeHandler` | the body of an effect; the read *is* the subscription |
| `PresenceManager` + `markAsPresent` | `SharedMapSignal<Peer>` keyed by session+UI, written on attach and cleared on detach |
| `CollaborationAvatarGroup` | `AvatarGroup` + an effect over the presence map |
| `CollaborationMessageList` / `MessageInput` | `SharedListSignal<Message>` + `MessageList` / `MessageInput` (prototyped in `signals/muc01`) |
| `CollaborationBinder` | `Binder` + a `SharedMapSignal` of field values + `SignalFieldHighlighter` (prototyped in `signals/muc04`) |
| `FieldHighlighter`, `HighlightHandler` | `SignalFieldHighlighter` over a `SharedListSignal<User>` per field |
| CE's implicit last-write-wins | explicit: `SharedValueSignal.replace(expected, new)` (compare-and-set), `SharedNumberSignal.incrementBy`, `Signal.runInTransaction` |
| — (no CE equivalent) | `withValidator(CommandValidator)`, `asReadonly()`, `Signal.computed` over shared state, `peekConfirmed()` |
| `EntryScope.CONNECTION` (auto-removal when a connection dies) | **nothing** — see gaps |
| `Backend` (Hazelcast/Redis, clustering) | **nothing** — see gaps |
| `HasExpirationTimeout` (topic data expiry) | **nothing** — see gaps |
| `CollaborationMessagePersister` | **nothing** — no persistence hook on a shared signal |

The two columns are close enough that parity is a realistic goal, and the
"nothing" rows are precisely why the module is worth building.

## Proposed use cases

Phrased from the developer's perspective, in the style of the `observability`
module's plan.

### Parity with the sampler

**UC1 — Show who is here.** I open a view and see an avatar for every user
currently on it; avatars appear and disappear as users arrive and leave,
including on browser refresh and tab close. The signals version of
`CollaborationAvatarGroup`: one presence map, one effect, no manager.

**UC2 — Join and leave on purpose.** Presence is not always "I am on this
page" — I want a user to be listed only after opting in (a checkbox), and to
stay connected while not listed. Distinguishes *attached* from *present*, which
is the distinction `markAsPresent(boolean)` exists for.

**UC3 — Render presence my own way.** The same presence map drives a hand-built
list — avatar, name, an idle marker — proving that presence is data I can read,
not a component I have to accept.

**UC4 — Chat in real time.** A `MessageList` and a `MessageInput` over a shared
list of messages, with the author's avatar and colour. Adds what the sampler
does not show: a persistence hook (CE's `CollaborationMessagePersister`), so
history survives a restart.

**UC5 — Edit a form together.** Two users on one form: values synchronize
field by field while typing, and each field shows who else is in it. The
signals answer to `CollaborationBinder` — a `Binder` over a shared map plus
`SignalFieldHighlighter`. The heaviest view, and the one most likely to
surface binder-level gaps.

**UC6 — See the raw collaboration events.** The same form, with a log panel
printing every property change and every highlight change as it happens
(CE's `FormManager` handlers). Shows what an effect sees and, more usefully,
what it *cannot* see — signals report new state, not "user X changed field Y
from A to B".

**UC7 — Control who may edit.** One user holds write access; the others watch
until it is handed over. Where signals beat CE: `withValidator` rejects writes
in the shared tree itself rather than by disabling fields in each client, and
`asReadonly()` hands out a projection that cannot write at all. Claiming the
lock is a `replace(null, me)` compare-and-set, so two simultaneous claims
cannot both win.

**UC8 — Collaborate in a Grid.** A shared list of rows behind a `Grid`:
inserts, edits and removals from any user land in every client, and each row
shows who is looking at it.

**UC9 — Collaborate in a Grid Pro.** Cell-level editing: the cell being edited
is highlighted for the other users, and two users editing the same cell
resolve deterministically instead of flickering.

### Beyond the sampler

CE cannot express these, so they are the argument for signals rather than a
port of it.

**UC10 — Make concurrent edits provably safe.** A view built to lose races:
`set()` (last write wins) against `replace()` (rejected, retried) against
`incrementBy` (never lost), with each operation's `SignalOperation` result
displayed. Two related lists mutated inside one `runInTransaction` — either
both move or neither does.

**UC11 — Derive shared state instead of storing it.** `Signal.computed` over
shared signals: unread counts per room, "users editing right now", per-row
aggregates — all recomputed for every client from one source of truth, with no
propagation code. CE has no computed layer; every derived value is
hand-maintained in a map.

**UC12 — Room per topic, created on demand.** `SharedNodeSignal` +
`putChildIfAbsent` as a topic namespace: rooms appear when first joined, each
with its own presence and message list, and empty rooms are cleaned up. Covers
CE's `topicId` addressing and raises what CE solves with expiration timeouts.

**UC13 — Show pending versus confirmed state.** `peekConfirmed()` next to
`get()`: what this client has optimistically applied against what the server
has accepted, and what the UI should show while a write is in flight or the
connection is down.

Thirteen views is the full ambition, not the first PR — see *Phasing*.

## Module shape

Standard sibling module, nothing new invented:

- `collaboration/pom.xml` — `use-cases-common`, `vaadin`,
  `vaadin-spring-boot-starter`, `browserless-test-spring` (test). No
  `collaboration-engine` dependency, deliberately: nothing in this module may
  need a Pro key.
- `Application.java` with `@Push` and the Aura stylesheet, mirroring the
  siblings.
- `views/MainLayout extends BaseMainLayout("collaboration", "Collaboration Use Cases")`
  and `home/HomeView extends BaseHomeView` with `addMenuLinkList()`.
- `Dockerfile` + `fly.toml` (`app = 'collaboration-cases'`). Both CI workflows
  discover modules by globbing for `pom.xml` / `fly.toml`, so no workflow edit
  is needed.
- Register in the root `pom.xml` `<modules>` and in
  `common/AppCatalog.APPS` — the cross-app selector is hand-maintained.
- Routes named after what they do (`presence`, `chat`, `form`, `grid`), with
  `@RouteAlias("ucN")`, as `observability` does.

**Shared harness** in `com.example.collab`, this module's equivalent of
`observability`'s `com.example.acme`:

- `Peer` — id, display name, avatar, colour index (CE's `UserInfo`).
- `PeerPanel` / `PeerRig` — the sampler's *Add user* harness: N simulated peers
  side by side in one page, each with a disconnect toggle. Every view is built
  on it, so every view is demonstrable in one tab and assertable in one test.
- `Presence` — application-scoped bean holding the presence map per topic,
  with join/leave wired to attach/detach.
- `SignalFieldHighlighter` — promoted from `signals/muc04`, where it already
  exists as "a prototype for a future addition to Flow".

**Relationship to `signals/muc01–muc07`.** Those seven multi-user views overlap
this module (shared chat, cursors, click race, field-level locking, shared task
list). They are use cases *of the signal API* and belong where they are; this
module is a CE-parity study and will re-implement, not import, what it needs.
The alternative — moving them here and leaving `signals` single-user — is worth
a decision before UC4/UC5 are written, since both start from an existing MUC.

## Testing

Per the repo's `CLAUDE.md`, every view gets a `SpringBrowserlessTest` sibling.
The `PeerRig` harness makes the interesting assertion cheap: drive peer A's
field, assert peer B's panel in the same test, no second session needed. Each
view additionally gets the cross-session test the convention asks for — mutate
the application-scoped bean from the test thread, or
`cleanVaadinEnvironment()` / `initVaadinEnvironment()` between two
`navigate()` calls — because that is what catches a `ListSignal` where a
`SharedListSignal` was meant.

## Expected API gaps

Predictions to confirm or refute while building; this list is the deliverable
as much as the views are.

1. **No connection-scoped entries.** CE's `EntryScope.CONNECTION` drops a
   user's entries when their connection dies; a shared signal keeps them
   forever. A killed browser leaves a ghost in the presence map, so every
   presence use case needs detach handling plus a heartbeat, and neither
   survives a server crash.
2. **No cluster backend.** CE has `Backend` (Hazelcast, Redis). Shared signals
   are single-node, so nothing in this module works behind two instances —
   the largest parity gap, and one an application would hit before a demo does.
3. **No expiration.** CE's `HasExpirationTimeout` discards topic data after
   inactivity. A `SharedNodeSignal` topic tree grows without bound.
4. **No persistence hook.** No signal-side equivalent of
   `CollaborationMessagePersister`; UC4 will have to reload and re-seed by
   hand.
5. **No component bindings.** `signals/MissingAPI` already carries `bindItems`,
   `bindInvalid`, `bindBrowserTitle`; this module will want the same for
   `AvatarGroup`, `MessageList` and `Grid`.
6. **No public field-highlighter API.** `vaadin-field-highlighter` is reachable
   only through `FieldHighlighterInitializer` and `executeJs`, as
   `SignalFieldHighlighter` shows.
7. **No `Binder` integration.** Two-way field↔signal binding is hand-written
   per field today, and every field needs its own editor list.
8. **Awkward keyed access.** Removing an entry by predicate means
   `peek().stream().filter(…).findFirst().ifPresent(list::remove)` (see
   `MUC04Signals.stopEditing`), and `SharedMapSignal` has no `getValues()`
   although `SharedListSignal` does.
9. **No change detail.** An effect sees the new state; UC6 needs the *old*
   value and the acting user, which the signal has to carry in its payload.

## Out of scope

- Any Collaboration Kit dependency, and any side-by-side "CE vs signals"
  runtime toggle. The comparison lives in the docs, not in the app.
- Clustering. Gap 2 is recorded, not worked around.
- Authentication. The sampler simulates users with an *Add user* button and so
  should this — `signals`' Spring Security setup is not needed here.

## Phasing

1. **Scaffold** — module, layout, home, deploy, catalog entry, `PeerRig`
   harness, plus UC1 as the first view that uses it.
2. **Presence** — UC2, UC3. Closes the presence half of the gap list.
3. **Forms** — UC5, UC6, UC7, promoting `SignalFieldHighlighter`. The
   highest-risk block; UC5 alone may justify a framework issue.
4. **Chat and data** — UC4, UC8, UC9.
5. **Beyond parity** — UC10–UC13, plus `API-GAPS.md` written up from what the
   earlier phases actually hit.

One PR per view, as in the other modules.
