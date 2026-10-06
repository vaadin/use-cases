package com.example;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * What the views asked the browser to do. Printing itself cannot be observed
 * without a browser, but the JavaScript a view queues can — and since Flow has
 * no printing API, that JavaScript <em>is</em> the behaviour under test.
 */
public final class PrintTestSupport {

    private PrintTestSupport() {
    }

    /** Whether something opened the browser's print dialog. */
    public static boolean printRequested() {
        return pendingJsContains("window.print()");
    }

    /**
     * Whether any JavaScript queued for the browser contains the given
     * fragment.
     *
     * @param fragment
     *            the fragment to look for
     * @return {@code true} when a pending invocation contains it
     */
    public static boolean pendingJsContains(String fragment) {
        return Objects.requireNonNull(UI.getCurrent()).getInternals()
                .containsPendingJavascript(fragment);
    }

    /**
     * Whether the JavaScript about to be sent to the browser mentions every one
     * of the given fragments — in an expression, or in a function or a string
     * passed to one. Listeners installed through Flow's trigger API travel as
     * {@link JsFunction} arguments of a generic initializer, so their bodies
     * and event names are only visible this way.
     * <p>
     * Takes the queue the way writing the response would, so call it once per
     * round trip.
     *
     * @param fragments
     *            the fragments to look for
     * @return {@code true} when each of them occurs somewhere in the queue
     */
    public static boolean queuedJsMentions(String... fragments) {
        String queued = Objects.requireNonNull(UI.getCurrent()).getInternals()
                .dumpPendingJavaScriptInvocations().stream()
                .flatMap(pending -> Stream.concat(
                        Stream.of(pending.getInvocation().getExpression()),
                        pending.getInvocation().getParameters().stream()
                                .flatMap(PrintTestSupport::text)))
                .reduce("", String::concat);
        return Arrays.stream(fragments).allMatch(queued::contains);
    }

    /**
     * The listeners about to be installed in the browser, by the element that
     * owns them. Each listener is given as the plain values it carries, depth
     * first and in order — property names, literals and the event name, but
     * neither elements nor function bodies — so a test can tell which property
     * a listener reads, which it writes, and on which event.
     * <p>
     * Takes the queue the way writing the response would, so call it once per
     * round trip.
     *
     * @return the values of each queued listener, keyed by its owner
     */
    public static Map<Element, List<List<Object>>> queuedListeners() {
        return Objects.requireNonNull(UI.getCurrent()).getInternals()
                .dumpPendingJavaScriptInvocations().stream()
                .filter(pending -> pending.getInvocation().getParameters()
                        .stream().anyMatch(JsFunction.class::isInstance))
                .collect(Collectors.groupingBy(
                        pending -> Element.get(pending.getOwner()),
                        LinkedHashMap::new,
                        Collectors.mapping(pending -> pending.getInvocation()
                                .getParameters().stream()
                                .filter(JsFunction.class::isInstance)
                                .flatMap(PrintTestSupport::values).toList(),
                                Collectors.toList())));
    }

    private static Stream<Object> values(Object value) {
        if (value instanceof JsFunction function) {
            return function.getCaptures().stream()
                    .flatMap(PrintTestSupport::values);
        }
        return value instanceof Element ? Stream.empty() : Stream.of(value);
    }

    private static Stream<String> text(Object value) {
        if (value instanceof JsFunction function) {
            return Stream.concat(Stream.of(function.getBody()), function
                    .getCaptures().stream().flatMap(PrintTestSupport::text));
        }
        return value instanceof String string ? Stream.of(string)
                : Stream.empty();
    }
}
