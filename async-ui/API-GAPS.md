# Async UI & Performance — API gaps

Where Flow has no API for an asynchronous-UI or performance use case in this module, or makes one awkward enough to need a workaround.

The short version: every building block exists, but nothing connects them. `UI.access`, `@Push`, `CompletableFuture`, signals and lazy data providers are all there, yet each view has to write the same glue by hand: capture the `UI`, hop back onto it, ignore answers that arrive after the user moved on, and cancel work when the component goes away. In this module that glue lives in `MissingAPI` and `AsyncState` instead of being repeated in every view.

## 1. No asynchronous signal (loading / value / error)

**Where it bites:** UC2 / `ParallelDashboardView.java`, UC4 / `OrderDetailView.java`, UC11 / `GridSearchLoadingView.java`
**Symptom:** a value that arrives later has three states: loading, loaded and failed. The skeleton, the content and the error message all depend on them. A `ValueSignal` can hold such a state, but Flow has no type for it and nothing that fills a signal from a `CompletableFuture`. Each view would capture `UI.getCurrent()`, chain `whenComplete`, call `UI.access`, unwrap `CompletionException` and switch on the result.
**Workaround used:** `AsyncState<T>` (a sealed `Loading` / `Loaded` / `Failed` type) and `MissingAPI.load(owner, signal, loader)`. It sets `Loading` at once, and `Loaded` or `Failed` through `UI.access` later.
**Suggested API:** a built-in async signal, e.g. `Signal<AsyncValue<T>> Signal.async(Component owner, Supplier<CompletableFuture<T>> loader)`, with `isLoading()`, `value()`, `error()` and `reload()`. Bindings such as `bindVisible(signal.map(AsyncValue::isLoading))` then come for free.
Upstream: https://github.com/vaadin/flow/issues/16697

## 2. Background work is not tied to the component that asked for it

**Where it bites:** UC3 / `LongRunningJobView.java`, UC2 / `ParallelDashboardView.java`, UC4 / `OrderDetailView.java`, UC7 / `ThrottledFeedView.java`
**Symptom:** `UI.access` is scoped to the UI, not to a component. When the user leaves a view, its background work keeps running and its answers still arrive. A chain of steps (UC3's batches) keeps starting new steps nobody will see. Every view has to keep the `UI` reference, override `onDetach` or add a detach listener, and cancel by hand. `UI.accessLater(command, detachHandler)` helps with a detached UI, not with a detached component.
**Workaround used:** `MissingAPI.load` cancels its future in a detach listener. UC3 cancels in `onDetach` and checks its state before it starts the next batch. UC7 removes its feed subscription and stops its flush task in `onDetach`.
**Suggested API:** `Component#access(Command)` and `Component#runAsync(Supplier<T>, SerializableConsumer<T>)` that run only while the component is attached and cancel when it detaches. The closed pull request https://github.com/vaadin/flow/pull/14414 had the same idea.
Upstream: https://github.com/vaadin/flow/issues/16697

## 3. No "latest wins" for overlapping requests

**Where it bites:** UC5 / `LatestResponseWinsView.java`, UC4 / `OrderDetailView.java`, UC2 / `ParallelDashboardView.java`
**Symptom:** search-as-you-type, switching between details and "reload" all start a new request while an older one is still running. The answers can come back in any order, and nothing in Flow stops a late answer to the older request from overwriting the newer one. Cancelling the older `CompletableFuture` is not enough: the backend still finishes, and the answer may already be queued in `UI.access`.
**Workaround used:** UC5 numbers its requests and drops every answer that is not for the newest one. UC2 and UC4 remove the registration of the previous `MissingAPI.load` before they start a new one, and `load` ignores answers for a cancelled load.
**Suggested API:** "latest wins" built into the async signal of #1 (a new load replaces the previous one), or an explicit `AsyncValue#switchTo(Supplier<CompletableFuture<T>>)`.

## 4. A data provider cannot answer asynchronously

**Where it bites:** UC1 / `MillionRowGridView.java`
**Symptom:** the fetch and count callbacks of `CallbackDataProvider` must return a `Stream` / `int` at once. A slow page blocks the thread, and the Grid cannot show a loading state or an error for one page. Applications with reactive or remote backends end up calling `.block()` / `.join()` in the callback. `DataCommunicator#enablePushUpdates(Executor)` moves the call to another thread, but the callback still blocks that thread and still has no way to report a failed page.
**Workaround used:** none. UC1 blocks (`SimulatedLatency.block`) and keeps the cost down instead: an item count estimate removes the count query, and the backend does the sorting and filtering.
**Suggested API:** `setItems(AsyncFetchCallback<T>)` that returns `CompletableFuture<List<T>>` (and an async count). The Grid shows placeholders for pages that are still loading and has a failure hook for pages that fail.
Upstream: https://github.com/vaadin/flow/issues/21865 (closed by its reporter without a fix)

## 5. The Grid has no loading state the server can set

**Where it bites:** UC11 / `GridSearchLoadingView.java`
**Symptom:** while a search runs in the background, the Grid should say so. The `<vaadin-grid>` web component has a `loading` attribute, but it only reflects the Grid's own page requests, and it is only a styling hook with no visual indicator. `Grid` has `setEmptyStateComponent`, but no loading-state counterpart, and no way to keep the previous rows visible but marked as outdated.
**Workaround used:** a `loading` CSS class bound to the search's `AsyncState` dims the previous rows. An indeterminate `ProgressBar` sits over the Grid, a spinner sits in the search field's suffix, and the empty-state component switches between skeleton rows (first load) and a "no matches" message.
**Suggested API:** `Grid#bindLoading(Signal<Boolean>)` / `setLoading(boolean)` with a built-in overlay and progress indicator, and `setLoadingStateComponent(Component)` next to `setEmptyStateComponent`. Together with #4, the Grid could manage all of this itself.
Upstream: https://github.com/vaadin/web-components/issues/1559

## 6. No asynchronous navigation hook

**Where it bites:** UC4 / `OrderDetailView.java`
**Symptom:** `HasUrlParameter#setParameter` and `BeforeEnterObserver#beforeEnter` are synchronous. Loading the route's data there freezes the navigation: the old view stays visible and the URL does not change until the lookup returns. Loading after navigation keeps the UI responsive, but then a missing record can no longer go to the application's real not-found page. The router has already committed to the route, so the view shows its own "does not exist" message.
**Workaround used:** render at once, load with `MissingAPI.load`, and show the not-found message inside the view.
**Suggested API:** a route-level loader, e.g. `BeforeEnterEvent#deferUntil(CompletableFuture<?>)`, which keeps the current view (with the loading indicator) until the future completes and still allows `rerouteToError` afterwards. An alternative is a `RouteDataLoader<T>` whose result the view receives once it is ready.

## 7. No server-side visibility event

**Where it bites:** UC6 / `DeferredSectionsView.java`
**Symptom:** tabs and `Details` have server-side events that make lazy building easy, but a section further down the page has no event for "scrolled into view". `Component` and `Element` have no viewport listener or signal. Deferring a heavy chart or table until the user scrolls to it needs an `IntersectionObserver` written in JavaScript.
**Workaround used:** `MissingAPI.onFirstVisible(component, action)`. It uses `executeJs` to install an `IntersectionObserver` that dispatches a custom DOM event, received as `MissingAPI.BecameVisibleEvent`.
**Suggested API:** `Component#addVisibleInViewportListener(...)` or `Signal<Boolean> Element#inViewportSignal()`, like the other `*Signal()` accessors such as `Element#sizeSignal()`.

## 8. Push cannot be throttled or batched

**Where it bites:** UC7 / `ThrottledFeedView.java`
**Symptom:** every `UI.access` from a background thread becomes its own round of change collection and its own push message. A feed of 100 events per second becomes 100 pushes per second per user, far more than anyone can read. `PushConfiguration` has no way to say "push at most every 250 ms" or "merge these updates".
**Workaround used:** UC7 keeps the latest value per key in a `ConcurrentHashMap` and flushes it from a fixed-rate `TaskScheduler` task, with one `UI.access` per flush.
**Suggested API:** a minimum push interval, e.g. `PushConfiguration#setMinimumInterval(Duration)`, that merges the changes of several `access` calls into one message. Or `UI.accessCoalesced(Object key, Command)`, where a pending command with the same key is replaced instead of queued.

## 9. Optimistic updates have no rollback helper

**Where it bites:** UC8 / `OptimisticSaveView.java`
**Symptom:** to make an edit feel instant, the UI changes first and the save follows. If the save fails, the view must remember the last value the server confirmed, roll back to it, and ignore failures of saves that a newer edit has already replaced. `Signal.runInTransaction` is synchronous, so a transaction cannot stay open during an asynchronous save.
**Workaround used:** per row, a version counter and the last confirmed value, managed by hand.
**Suggested API:** an optimistic write on a signal, e.g. `signal.setOptimistically(value, CompletableFuture<?> confirmation)`, which reverts on failure unless a newer write has happened.

## 10. The loading indicator only covers the request, not background work

**Where it bites:** UC9 / `SlowRequestFeedbackView.java`, UC2 / `ParallelDashboardView.java`
**Symptom:** `LoadingIndicatorConfiguration` controls when the bar appears for a slow request. Once work moves to the background (UC2 to UC5), the request returns at once and the indicator never appears. Every view has to bring its own skeletons and spinners, and there is no global "the server is still working for you" state.
**Workaround used:** the `skeleton` CSS class in `styles.css`, bound to `AsyncState::isLoading`.
**Suggested API:** let background work report to the same indicator, e.g. `UI#trackBusy(CompletableFuture<?>)` / `Registration UI#showBusy()`.

## 11. No skeleton component

**Where it bites:** UC12 / `StructuredSkeletonView.java`, UC2 / `ParallelDashboardView.java`, UC4 / `OrderDetailView.java`, UC11 / `GridSearchLoadingView.java`
**Symptom:** a skeleton is the usual way to show that content is on its way, but Flow and the component set have none. Every view builds placeholder bars from `Div`s, a shared CSS class and its own sizes, and pairs each one with a `bindVisible` on the content's state. UC12 needs a shape per section (a heading line, a text line, a rating, list lines) and sizes each bar by hand so the card does not jump when the content replaces it.
**Workaround used:** the `skeleton` class in `styles.css`, plus `bar-*` variants in `uc12.css`, each bound to the matching part of the answer.
**Suggested API:** a `Skeleton` component with common shapes (`Skeleton.text(int lines)`, `Skeleton.circle(size)`, `Skeleton.rectangle(width, height)`) that follows the theme's typography, and a way to swap it for the real content when a signal has a value, e.g. `Skeleton.until(Signal<?> value, Component content)`.

## 12. No streaming counterpart to the asynchronous signal

**Where it bites:** UC12 / `StructuredSkeletonView.java`
**Symptom:** an answer that arrives part by part (a language model's output, a paged export, a server-sent event stream) is not a single `CompletableFuture`. Flow has nothing that feeds a stream into signals while the component is attached. The view has to capture the `UI`, hop onto it for every part, drop already queued parts of a replaced request, and stop the stream in `onDetach`.
**Workaround used:** `ReviewSummaries` delivers parts to a callback and returns a `Registration`. The view wraps each part in `UI.accessLater`, checks a generation counter, and removes the registration when it starts again or detaches.
**Suggested API:** `Signal.fromPublisher(Component owner, Flow.Publisher<T>)` / `ListSignal#appendFrom(Component owner, Flow.Publisher<T>)`. They deliver on the UI, cancel the subscription on detach, and expose a completed / failed state like the async signal of #1.

## 13. Image has no load event and no placeholder

**Where it bites:** UC13 / `BlurHashPreviewView.java`
**Symptom:** `Image` has no `addLoadListener` / `addErrorListener` and no placeholder. Showing a preview until the real image is complete needs a second `Image` under the first, CSS to hide and fade the real one, and a generic DOM `load` listener. That listener is on the server, so the fade only starts after a round trip; doing it in the browser alone would need `executeJs`. Sending a tiny preview with the page is also manual: `new Image(byte[], alt)` serves the bytes through a separate request, so the view builds a `data:` URL by hand.
**Workaround used:** a frame with the photo's `aspect-ratio`, a preview `Image` with a `data:` URL decoded from the photo's BlurHash, the average colour as the frame's background, and a `loaded` class bound to a signal that the `load` event sets.
**Suggested API:** `Image#addLoadListener` / `addErrorListener`, and `Image#setPlaceholder(String src)` / `setPlaceholderColor(String)`, which the browser shows until the image has loaded and then fades out without a round trip. Optionally `Image#setBlurHash(String)`, decoded in the browser.

## 14. Browserless tests cannot control time or background threads

**Where it bites:** every test in this module
**Symptom:** a browserless test runs on its own thread and holds the session lock. Background answers queue up in `UI.access` and only run when the test calls `runPendingSignalsTasks()`. `BaseBrowserlessTest#roundTrip()`, the obvious choice, does not run them. There is no simulated clock and no way to complete a slow call on demand, so testing "the third widget answers first" or "the older search answers last" would be a timing race.
**Workaround used:** all slow calls go through the `SimulatedLatency` interface. The tests replace it with `ManualLatency` (a `@Primary` bean in the test sources), whose calls stay pending until the test completes them in any order. UC6 fires `BecameVisibleEvent` with `ComponentUtil.fireEvent`, because nothing in browserless testing can scroll.
**Suggested API:** a browserless `runPendingAccessTasks()` / `awaitAsync()` with a clear name, and a test clock that `UI.accessLater` and scheduler-based helpers follow.
Upstream: https://github.com/vaadin/browserless-test/issues/282 (the pending access tasks; there is no issue for the test clock)
