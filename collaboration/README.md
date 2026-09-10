# Collaboration — use cases

A standalone Spring Boot demo module: the
[Collaboration Engine Sampler](https://ce-sampler.demo.vaadin.com/) rebuilt on
**shared signals only**, with no Collaboration Kit dependency.

The point is not a nicer sampler. Collaboration Kit is a commercial add-on with
its own topic, connection and backend machinery; shared signals
(`com.vaadin.flow.signals.shared`) are core Flow and cover much of the same
ground. This module works through what a developer actually has to write to
get each Collaboration Kit feature out of plain signals — and where they
cannot get it at all. The second half is collected in
[`API-GAPS.md`](API-GAPS.md), which is as much the deliverable as the views
are.

UC1–UC9 mirror the sampler's nine samples one for one, so the two apps can be
opened side by side. UC10–UC13 cover collaboration that Collaboration Kit
cannot express.

## The harness

Every view runs **several simulated users side by side in one page**, with an
*Add user* button that adds another and a *Disconnect* link that takes one
away — borrowed from the sampler, because it is how collaboration is shown
without asking the reader to open a second browser. Opening a second tab still
works, and shows that the state really is shared.

It also makes the repository's browserless testing convention able to assert
propagation: drive peer A's field, assert peer B's panel, one session, no
polling. `PeerRig`, `PeerPanel` and `Peer` in `com.example.collab` are that
harness; disconnecting a peer removes its content from the DOM, which is the
same detach sequence as closing a tab and is what the presence and
field-highlight cleanup hangs off.

## Use cases

| # | Route | What it shows |
| - | ----- | ------------- |
| — | `/` | Landing page and auto-generated index. |
| 1 | `presence` | **Show who is here.** `CollaborationAvatarGroup` as a `SharedMapSignal<Peer>` and `AvatarGroup.bindItems`. Joining and leaving is the part that moved into application code. |
| 2 | `presence-opt-in` | **Join and leave on purpose.** `markAsPresent(boolean)` as put/remove: attached and present are not the same thing. |
| 3 | `presence-custom` | **Render presence my own way.** The same map as a hand-built list, with a rename, and a join/leave log that has to be diffed because an effect reports state rather than events. |
| 4 | `chat` | **Chat in real time.** `MessageList.bindItems` over an append-only `SharedListSignal`, plus the persistence that `CollaborationMessagePersister` would have handled — here written twice by hand. |
| 5 | `form` | **Edit a form together.** `CollaborationBinder`'s two halves: per-field value sync through a shared map, and the platform's field highlighter driven by a shared list of editors per field. |
| 6 | `form-events` | **See the raw collaboration events.** `FormManager`'s handlers as a log: old and new values come from the binding, the author from the stored value, and editor arrivals from a diff. |
| 7 | `form-access` | **Control who may edit.** A write lock claimed with a compare-and-set, so a lost race is reported rather than silently won, and writes refused in the shared state by a validator rather than by a disabled field. |
| 8 | `grid` | **Collaborate in a Grid.** A shared row list behind a `Grid`, with each peer's selection visible to the others. The row *signals* are the grid's items, which is what makes identity survive an edit. |
| 9 | `grid-pro` | **Collaborate in a Grid Pro.** Cell editing as `SharedValueSignal.update`, so two peers editing different columns of one row both keep their change. |
| 10 | `conflicts` | **Make concurrent edits provably safe.** `set` against `replace` against `incrementBy`, and a two-list move inside one transaction. Beyond Collaboration Kit. |
| 11 | `computed` | **Derive shared state instead of storing it.** Every readout computed from one shared list, with no counter to keep in step. Beyond Collaboration Kit. |
| 12 | `rooms` | **Room per topic, created on demand.** A `SharedNodeSignal` as a topic namespace, so the room list is shared state a client can render. Beyond Collaboration Kit. |
| 13 | `pending` | **Pending versus confirmed state.** `get()` and `peekConfirmed()` disagreeing inside the callback that wrote, and what that means for reporting an operation's outcome. Beyond Collaboration Kit. |

Each view is also reachable as `/ucN`, and says in the app itself what it had
to write by hand and which gap that is.

## Run

```
mvn spring-boot:run -pl :collaboration-use-cases
```

Open <http://localhost:8080/>.

## Notes

- **`@Push` is not optional.** Every use case is one user's write arriving in
  another user's browser.
- **The heartbeat is turned down** (`vaadin.heartbeatInterval=15`). Nothing
  removes a peer whose browser vanished without detaching, so the module leans
  on the only cleanup that exists: Vaadin closes a UI after three missed
  heartbeats, and that detach runs the leave listeners. At the default five
  minutes a killed tab leaves ghost avatars for a quarter of an hour. See
  [`API-GAPS.md`](API-GAPS.md) #1.
- **No authentication.** Users are simulated with an *Add user* button, as in
  the sampler, so nothing here needs a login.
- **`vaadin` rather than `vaadin-core`**, because UC9 mirrors the sampler's
  Grid Pro sample and the field highlighter ships in the same artifact.
