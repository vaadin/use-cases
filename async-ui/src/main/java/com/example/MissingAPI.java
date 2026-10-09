package com.example;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableSupplier;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Shims for asynchronous UI work that Flow does not provide yet. Each method
 * names the gap it covers; see API-GAPS.md for the full story and the API we
 * would rather call.
 */
public final class MissingAPI {

    /** The DOM event {@link #onFirstVisible} listens to. */
    static final String VISIBLE_EVENT = "async-ui-visible";

    private MissingAPI() {
    }

    /**
     * Loads a value in the background and publishes its progress to
     * {@code target}: {@link AsyncState.Loading} right away, then
     * {@link AsyncState.Loaded} or {@link AsyncState.Failed} through
     * {@link UI#access} when the future completes.
     * <p>
     * The result is dropped when {@code owner} has been detached in the
     * meantime, and detaching cancels the future. Removing the returned
     * registration cancels it too, which is how a caller that starts a newer
     * load makes sure an older answer can never overwrite it.
     * <p>
     * Gap: Flow has no asynchronous signal, no component-scoped {@code access}
     * that follows the component's lifecycle, and no cancellation tied to
     * detach (vaadin/flow#16697). Migrate to a built-in async signal once one
     * exists.
     *
     * @param owner
     *            the component whose lifecycle bounds the load; must be
     *            attached, or be created in a request of the current UI
     */
    public static <T> Registration load(Component owner,
            ValueSignal<AsyncState<T>> target,
            SerializableSupplier<CompletableFuture<T>> loader) {
        UI ui = Objects.requireNonNull(owner.getUI().orElseGet(UI::getCurrent),
                "load() needs an attached owner or a current UI");
        target.set(AsyncState.loading());
        // Cancelling the future is not enough: it may already have completed
        // with its UI update still queued. The flag is checked when that
        // update runs, on the UI thread.
        AtomicBoolean active = new AtomicBoolean(true);
        CompletableFuture<T> future = loader.get();
        Registration detach = owner.addDetachListener(event -> {
            active.set(false);
            future.cancel(false);
        });
        future.whenComplete((value, error) -> {
            if (future.isCancelled()) {
                return;
            }
            ui.accessLater(() -> {
                if (!active.get()) {
                    return;
                }
                detach.remove();
                if (error == null) {
                    target.set(new AsyncState.Loaded<>(value));
                } else {
                    target.set(new AsyncState.Failed<>(unwrap(error)));
                }
            }, null).run();
        });
        return () -> {
            active.set(false);
            detach.remove();
            future.cancel(false);
        };
    }

    /**
     * The cause a {@link CompletableFuture} stage wraps its failures in, or the
     * error itself.
     */
    public static Throwable unwrap(Throwable error) {
        if (error instanceof CompletionException && error.getCause() != null) {
            return error.getCause();
        }
        return error;
    }

    /**
     * Whether {@code error} is the cancellation of a future rather than a
     * failure of the work behind it.
     */
    public static boolean isCancellation(Throwable error) {
        return unwrap(error) instanceof CancellationException;
    }

    /**
     * Runs {@code action} once, the first time {@code component} scrolls into
     * the browser's viewport.
     * <p>
     * Gap: Flow has no server-side visibility (IntersectionObserver) event, so
     * deferring work until something is actually on screen needs this
     * {@code executeJs}. Migrate to a built-in event once one exists.
     */
    public static Registration onFirstVisible(Component component,
            Runnable action) {
        Registration[] registration = new Registration[1];
        registration[0] = ComponentUtil.addListener(component,
                BecameVisibleEvent.class, event -> {
                    registration[0].remove();
                    action.run();
                });
        component.getElement().executeJs("""
                const element = this;
                const observer = new IntersectionObserver(entries => {
                  if (entries.some(entry => entry.isIntersecting)) {
                    observer.disconnect();
                    element.dispatchEvent(new CustomEvent($0));
                  }
                });
                observer.observe(element);
                """, VISIBLE_EVENT);
        return registration[0];
    }

    /**
     * Fired by the browser when a component observed by {@link #onFirstVisible}
     * enters the viewport.
     */
    @DomEvent(VISIBLE_EVENT)
    public static class BecameVisibleEvent extends ComponentEvent<Component> {

        public BecameVisibleEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }
}
