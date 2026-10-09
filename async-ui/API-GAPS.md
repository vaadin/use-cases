# Async UI & Performance — API gaps discovered while building the demos

Places where Flow has no API for a genuine asynchronous-UI or performance use case, or makes one awkward enough to need a workaround. Each entry names the use case that surfaced it.

The short version: **every building block exists, but nothing connects them.** `UI.access`, `@Push`, `CompletableFuture`, signals and lazy data providers are all there, yet each view in this module has to write the same glue by hand: capture the `UI`, hop back onto it, ignore answers that arrive after the user moved on, and cancel work when the component goes away. The open request for a better API, [vaadin/flow#16697](https://github.com/vaadin/flow/issues/16697), describes exactly that glue. In this module it lives in `src/main/java/com/example/MissingAPI.java` and `AsyncState.java` instead of being repeated in every view.

---

## No asynchronous signal (loading / value / error)

**Where it bit us:** uc2 / ParallelDashboardView.java, uc4 / OrderDetailView.java, uc11 / GridSearchLoadingView.java
**Symptom:** a value that arrives later has three states (loading, loaded, failed), and the skeleton, the content and the error message all depend on them. Signals can hold such a state, but Flow has no type for it and nothing that fills a signal from a `CompletableFuture`. Each view would capture `UI.getCurrent()`, chain `whenComplete`, call `ui.access`, unwrap `CompletionException` and switch on the result.
**Workaround used:** `AsyncState<T>` (a sealed `Loading` / `Loaded` / `Failed` type) and `MissingAPI.load(owner, signal, loader)`, which sets `Loading` at once and `Loaded` / `Failed` through `UI.access` later.
**Suggested API:** a built-in async signal, e.g. `Signal<AsyncValue<T>> Signal.async(Component owner, Supplier<CompletableFuture<T>> loader)`, with `isLoading()`, `value()` and `error()`, and a `reload()`. Bindings such as `bindVisible(signal.map(AsyncValue::isLoading))` then come for free.

## Background work is not tied to the component that asked for it

**Where it bit us:** uc3 / LongRunningJobView.java, uc2 / ParallelDashboardView.java, uc4 / OrderDetailView.java, uc7 / ThrottledFeedView.java
**Symptom:** `UI.access` is UI-scoped, not component-scoped. When the user leaves a view, its background work keeps running and its answers still arrive. Pushing them into a detached component costs nothing visible but wastes work, and a chain of steps (UC3's batches) keeps starting new steps nobody will see. Every view has to override `onDetach` or add a detach listener, cancel by hand, and keep the `UI` reference itself. `UI.accessLater(command, detachHandler)` helps with a detached *UI*, not a detached *component*.
**Workaround used:** `MissingAPI.load` cancels its future in a detach listener. UC3 cancels in `onDetach` and checks its state before starting the next batch. UC7 removes its feed subscription and stops its flush task in `onDetach`.
**Suggested API:** `Component#access(Command)` and `Component#runAsync(Supplier<T>, SerializableConsumer<T>)` that run only while the component is attached and cancel when it detaches (the idea behind the closed [vaadin/flow#14414](https://github.com/vaadin/flow/pull/14414) and the open [vaadin/flow#16697](https://github.com/vaadin/flow/issues/16697)).

## No "latest wins" for overlapping requests

**Where it bit us:** uc5 / LatestResponseWinsView.java, uc4 / OrderDetailView.java, uc2 / ParallelDashboardView.java
**Symptom:** search-as-you-type, switching between details and "reload" all start a new request while an older one is still out, and the answers can come back in any order. Nothing in Flow stops the late answer to the older request from overwriting the newer one. Cancelling the older `CompletableFuture` is not enough: it does not stop the backend from finishing, and the answer may already be queued in `UI.access`.
**Workaround used:** UC5 numbers its requests and drops every answer that is not for the newest one. UC2 and UC4 cancel the previous `MissingAPI.load` registration before they start a new load, and `load` ignores answers for a cancelled future.
**Suggested API:** "latest wins" built into the async signal above (a new load supersedes the previous one), or an explicit `AsyncValue#switchTo(Supplier<CompletableFuture<T>>)`.

## A data provider cannot answer asynchronously

**Where it bit us:** uc1 / MillionRowGridView.java
**Symptom:** `CallbackDataProvider`'s fetch and count callbacks must return a `Stream` / `int` synchronously. A slow page therefore blocks the request thread and every other interaction of that user, and the Grid cannot show a per-page loading state or an error for a failed page. Applications with reactive or remote backends end up calling `.block()` / `.join()` in the callback ([vaadin/flow#21865](https://github.com/vaadin/flow/issues/21865)).
**Workaround used:** none possible. UC1 blocks (`SimulatedLatency.block`) and keeps the cost down instead: an item count estimate removes the count query, and the backend does the sorting and filtering.
**Suggested API:** `setItems(AsyncFetchCallback<T>)` returning `CompletableFuture<List<T>>` (and an async count), with the Grid showing placeholders for pages still in flight and a failure hook for pages that fail.

## The Grid has no loading state the server can set

**Where it bit us:** uc11 / GridSearchLoadingView.java
**Symptom:** while a search runs in the background, the Grid should say so. The `<vaadin-grid>` web component has a `loading` attribute, but it only reflects the Grid's own page requests. The server cannot set it for data it is fetching itself, and the attribute only offers a styling hook, with no visual indicator. The Grid has an empty-state component, but there is no loading-state counterpart, and no built-in way to keep the previous rows visible but marked as outdated.
**Workaround used:** a `loading` CSS class bound to the search's `AsyncState` dims the previous rows. An indeterminate `ProgressBar` sits over the Grid, a spinner sits in the search field's suffix, and the empty-state component is swapped between skeleton rows (first load) and a "no matches" message.
**Suggested API:** `Grid#bindLoading(Signal<Boolean>)` / `setLoading(boolean)` with a built-in overlay and progress indicator, and `setLoadingStateComponent(Component)` next to `setEmptyStateComponent`. Together with an asynchronous data provider (see above), the Grid could manage all of this itself.

## No asynchronous navigation hook

**Where it bit us:** uc4 / OrderDetailView.java
**Symptom:** `setParameter` / `beforeEnter` are synchronous. Loading the route's data there freezes the navigation: the old view stays visible and the URL does not change until the lookup returns. Loading after navigation keeps the UI responsive, but then a missing record can no longer be turned into the application's real not-found page: the router has already committed to the route, so the view shows its own "does not exist" message instead.
**Workaround used:** render at once, load with `MissingAPI.load`, and show not-found inside the view.
**Suggested API:** a route-level loader, e.g. `BeforeEnterEvent#deferUntil(CompletableFuture<?>)`, which keeps the current view (with the loading indicator) until the future completes and still allows `rerouteToError` afterwards. An alternative is a `RouteDataLoader<T>` whose result the view receives once it is ready.

## No server-side visibility event

**Where it bit us:** uc6 / DeferredSectionsView.java
**Symptom:** tabs and `Details` have server-side events that make lazy building easy, but a section further down the page has no event for "scrolled into view". Deferring a heavy chart or table until the user scrolls to it needs an `IntersectionObserver` written in JavaScript.
**Workaround used:** `MissingAPI.onFirstVisible(component, action)`, which uses `executeJs` to install an `IntersectionObserver` that dispatches a custom DOM event, received as `MissingAPI.BecameVisibleEvent`.
**Suggested API:** `Component#addVisibleInViewportListener(...)` or a `Signal<Boolean> Element#inViewportSignal()`, consistent with the other `*Signal()` accessors such as `Element#sizeSignal()`.

## Push cannot be throttled or batched

**Where it bit us:** uc7 / ThrottledFeedView.java
**Symptom:** every `UI.access` from a background thread becomes its own round of change collection and its own push message. A feed of 100 events per second becomes 100 pushes per second per user, far more than anyone can read. Flow has no way to say "push at most every 250 ms" or "merge these updates".
**Workaround used:** UC7 keeps the latest value per key in a `ConcurrentHashMap` and flushes it from a fixed-rate `TaskScheduler` task, which does one `UI.access` per flush.
**Suggested API:** a minimum push interval on `PushConfiguration` (e.g. `setMinimumInterval(Duration)`) that merges the changes of several `access` calls into one message. Alternatively, `UI.accessCoalesced(Object key, Command)`, where a pending command with the same key is replaced instead of queued.

## Optimistic updates have no rollback helper

**Where it bit us:** uc8 / OptimisticSaveView.java
**Symptom:** to make an edit feel instant, the UI changes first and the save follows. If the save fails, the view must remember the last value the server confirmed, roll back to it, and ignore failures of saves a newer edit has already replaced. Signals have transactions, but they are synchronous and cannot be kept open across an asynchronous save.
**Workaround used:** per row, a version counter and the last confirmed value, managed by hand.
**Suggested API:** an optimistic write on a signal, e.g. `signal.setOptimistically(value, CompletableFuture<?> confirmation)`, which reverts automatically on failure unless a newer write has happened.

## The loading indicator only covers the request, not background work

**Where it bit us:** uc9 / SlowRequestFeedbackView.java, uc2 / ParallelDashboardView.java
**Symptom:** `LoadingIndicatorConfiguration` controls when the bar appears for a slow *request*. Once work moves to the background (UC2 to UC5), the request returns at once and the indicator never appears. Every view has to bring its own skeletons and spinners, and there is no global "the server is still working on something for you" state.
**Workaround used:** the skeleton CSS class in `styles.css`, bound to `AsyncState::isLoading`.
**Suggested API:** let background work report to the same indicator, e.g. `UI#trackBusy(CompletableFuture<?>)` / `Registration UI#showBusy()`.

## No skeleton component

**Where it bit us:** uc12 / StructuredSkeletonView.java, uc2 / ParallelDashboardView.java, uc4 / OrderDetailView.java, uc11 / GridSearchLoadingView.java
**Symptom:** a skeleton is the usual way to show that content is on its way, but Flow and the component set have none. Every view assembles placeholder bars from `Div`s, a shared CSS class and per-view sizes, and pairs each one with a `bindVisible` on the content's state. UC12 needs a shape per section (a heading line, a text line, a rating, list lines) and has to size each bar by hand so the card does not jump when the content replaces it.
**Workaround used:** the `skeleton` class in `styles.css`, plus `bar-*` variants in `uc12.css`, each bound to the matching part of the answer.
**Suggested API:** a `Skeleton` component with common shapes (`Skeleton.text(int lines)`, `Skeleton.circle(size)`, `Skeleton.rectangle(width, height)`) that follows the theme's typography, and a way to swap it for the real content when a signal has a value, e.g. `Skeleton.until(Signal<?> value, Component content)`.

## No streaming counterpart to the asynchronous signal

**Where it bit us:** uc12 / StructuredSkeletonView.java
**Symptom:** a structured answer that arrives part by part (a language model's output, a paged export, a server-sent event stream) is not a single `CompletableFuture`. Flow has nothing that feeds a stream into signals while the component is attached: the view has to capture the `UI`, hop onto it for every part, drop parts of a superseded request that are already queued, and stop the stream in `onDetach`.
**Workaround used:** `ReviewSummaries` delivers parts to a callback and returns a `Registration`; the view wraps each part in `UI.accessLater`, checks a generation counter, and removes the registration when it starts again or detaches.
**Suggested API:** `Signal.fromPublisher(Component owner, Flow.Publisher<T>)` / `ListSignal#appendFrom(Component owner, Flow.Publisher<T>)`, which deliver on the UI, cancel the subscription on detach, and expose a completed / failed state like the asynchronous signal above.

## Image has no load event and no placeholder

**Where it bit us:** uc13 / BlurHashPreviewView.java
**Symptom:** `Image` has no `addLoadListener` / `addErrorListener` and no placeholder. Showing a preview until the real image is complete needs a second `Image` stacked under the first, CSS to hide and fade the real one, and a generic DOM `load` listener. Because that listener is on the server, the fade only starts after a round trip; doing it in the browser alone would need `executeJs`. Sending a tiny preview with the page is also manual: `new Image(byte[], alt)` serves the bytes through a separate request, so the preview is turned into a `data:` URL by hand.
**Workaround used:** a frame with the photo's `aspect-ratio`, a preview `Image` with a `data:` URL decoded from the photo's BlurHash, the average colour as the frame's background, and a `loaded` class bound to a signal set by the `load` event.
**Suggested API:** `Image#addLoadListener` / `addErrorListener`, and `Image#setPlaceholder(String src)` / `setPlaceholderColor(String)` that the browser shows until the image has loaded and then fades out without a round trip. Optionally, `Image#setBlurHash(String)` with the decoding done in the browser.

## Browserless tests cannot control time or background threads

**Where it bit us:** every test in this module
**Symptom:** a browserless test runs on its own thread with the session lock held. Background answers queue up in `UI.access` and only run when the test calls `runPendingSignalsTasks()` (not `roundTrip()`, which might seem like the obvious choice). There is no simulated clock and no way to complete a "slow" call on demand, so testing "the third widget answers first" or "the older search answers last" would be a timing race.
**Workaround used:** all slow calls in the module go through the `SimulatedLatency` interface. The tests replace it with `ManualLatency` (a `@Primary` bean in the test sources), whose calls stay pending until the test completes them in any order it chooses. UC6 fires `BecameVisibleEvent` with `ComponentUtil.fireEvent`, because nothing in browserless testing can scroll.
**Suggested API:** a browserless `runPendingAccessTasks()` / `awaitAsync()` with an explicit name, and a test clock that `UI.accessLater` and scheduler-based helpers honour.
