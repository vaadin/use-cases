# Views

## Structure

A use case is one Flow view:

```java
/**
 * UC1 — Update when active.
 * <p>
 * A live server clock updates every second while the tab is visible. ...
 */
@Route(value = "uc1", layout = MainLayout.class)
@UseCaseDescription("Pausing periodic updates while the browser tab is hidden")
@Menu(order = 1, title = "UC1 — Update when active")
@StyleSheet("uc1.css")
public class UpdateWhenActiveView extends VerticalLayout {
```

- Package `com.example.uc<N>`, class `<Descriptive>View`, route `uc<N>`. A
  module whose story calls for meaningful routes (e.g. `orders`) keeps
  `uc<N>` as a `@RouteAlias`.
- An `H1` with the same text as the menu title, then an intro paragraph.
- Spring beans the view needs (a `TaskScheduler`, an application-scoped
  service) come in through the constructor.
- Helpers only one view needs live in the view's package. Promote them to a
  module-level package when a second view needs them, and to `common` when a
  second module does.

## Lifecycle

A view starts its work when it is attached and stops it when it is detached.
Anything that keeps running after the user navigates away — a scheduled task,
a poll, a listener on a shared bean — leaks for the rest of the session and
changes what other views show.

Use `whenAttached` for this. It keeps the setup and the matching cleanup in one
place, runs again on every re-attach, and needs no fields to carry state from
`onAttach` to `onDetach`:

```java
public LiveClockView(TaskScheduler taskScheduler) {
    timeLabel.bindText(time.map(TIME::format));

    whenAttached(ui -> {
        ScheduledFuture<?> tick = taskScheduler.scheduleAtFixedRate(
                () -> time.set(LocalTime.now()), Duration.ofSeconds(1));
        return () -> tick.cancel(false);
    });
}
```

- Return a cleanup that releases everything the handler started: the
  `Registration` of a listener, the `ScheduledFuture` of a task.
- `Signal.effect(this, ...)` and the `bind*` methods are tied to the
  component's attach state already and need no cleanup.
- Override `onAttach`/`onDetach` only when `whenAttached` does not fit. Then
  set a "scheduled" or "running" flag only once the work is actually
  scheduled: if the view is not attached, nothing will ever clear the flag
  again.
- Many existing views still use `onAttach`/`onDetach` and `UI#access`. They
  predate `whenAttached`; follow this chapter for new code and when you
  rework a view, not the older views.
- Write state-changing logic so it can be unit tested without a UI: a small
  package-private static method (e.g. deriving a status from two timestamps)
  is easier to test than the view that calls it.

## Session- and application-wide state

Do not change state outside the view to make a demo work: the session error
handler, a global listener, a static field. Other views, other tabs of the
same session and the framework itself (which may wrap or replace the same
object) all see the change, and restoring it on detach is easy to get wrong.
Handle what the view needs in the view's own listeners instead. If the view
needs to show an error, catch it where it happens, show it, and let it
propagate so the framework still logs it.

State that several users or tabs share lives in an application-scoped Spring
bean and uses a shared signal (`SharedValueSignal`, `SharedListSignal`,
`SharedMapSignal`). A local signal (`ValueSignal`, `ListSignal`) in a shared
bean is a bug that only shows up in the second tab.

## Asynchronous updates

Let background work write to signals, and let the view bind its components to
those signals, instead of changing components through `UI#access`. A signal can
be set from any thread; the bindings and effects that read it update the UI
under the session lock and the change is pushed to the browser:

```java
private final ValueSignal<LocalTime> time = new ValueSignal<>(LocalTime.now());

// in the background task
time.set(LocalTime.now());

// in the view
timeLabel.bindText(time.map(TIME::format));
```

- Set immutable values (`set`, `update`, `replace`). `modify` on a local
  signal is not safe from a background thread.
- Use `UI#access` only when there is no state to put in a signal, e.g. a
  one-off notification.
- Add `@Push` to the app shell when a view updates without a user action, and
  `@EnableScheduling` when it uses Spring's `TaskScheduler`.
- Simulated latency belongs in a helper the tests can switch off, so tests do
  not wait.

## Client-side code

Prefer a Flow API to JavaScript. When a use case needs JavaScript, keep more
than a few lines out of `executeJs` strings: put it in a TypeScript module under
`src/main/frontend/` and call it from the view. Run it from `whenAttached`, so
it runs again when Flow recreates the element.

## Security and guest access

Use cases must be browsable without logging in. A module that uses Spring
Security:

- annotates its views `@AnonymousAllowed`, and gives guests a usable identity
  where the use case needs one;
- permits the static resources views load directly (`/*.css`,
  `/og-image.jpg`), otherwise Spring Security redirects them to the login page
  and the view renders unstyled;
- returns the user to the view they were on after logging in.

## Data

Read payloads from a feature through a small helper that renders a missing
value as "—" rather than `null`, so a change in the payload shape does not show
"null ms" to the reader.
