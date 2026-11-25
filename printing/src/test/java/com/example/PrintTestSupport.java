package com.example;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCall;

/**
 * What the views asked the browser to do. Printing itself cannot be observed
 * without a browser, but the JavaScript a view sends can — and since Flow has
 * no printing API, that JavaScript <em>is</em> the behaviour under test.
 * <p>
 * A test starts one with {@link #capture()} before it navigates, typically in a
 * {@code @BeforeEach} method. The checks look at everything sent to the browser
 * of the UI since then, including what the navigation sent.
 */
public final class PrintTestSupport {

    private final ExecutedJavaScript executed;

    private PrintTestSupport(ExecutedJavaScript executed) {
        this.executed = executed;
    }

    /**
     * Starts watching the JavaScript sent to the browser of the current UI.
     *
     * @return the checks of what was sent, not null
     */
    public static PrintTestSupport capture() {
        return new PrintTestSupport(ExecutedJavaScript
                .capture(Objects.requireNonNull(UI.getCurrent())));
    }

    /** Whether something opened the browser's print dialog. */
    public boolean printRequested() {
        return sentJsContains("window.print()");
    }

    /**
     * Whether any JavaScript expression sent to the browser contains the given
     * fragment.
     *
     * @param fragment
     *            the fragment to look for
     * @return {@code true} when a sent expression contains it
     */
    public boolean sentJsContains(String fragment) {
        return sent().stream().anyMatch(
                execution -> execution.getExpression().contains(fragment));
    }

    /**
     * Whether the JavaScript sent to the browser mentions every one of the
     * given fragments — in an expression, or in a function or a string passed
     * to one. Listeners installed through Flow's trigger API travel as
     * {@link JsFunction} arguments of a generic initializer, so their bodies
     * and event names are only visible this way.
     *
     * @param fragments
     *            the fragments to look for
     * @return {@code true} when each of them occurs somewhere in what was sent
     */
    public boolean sentJsMentions(String... fragments) {
        String sent = sent().stream()
                .flatMap(execution -> Stream.concat(
                        Stream.of(execution.getExpression()),
                        execution.getParameters().stream()
                                .flatMap(PrintTestSupport::text)))
                .reduce("", String::concat);
        return Arrays.stream(fragments).allMatch(sent::contains);
    }

    /**
     * The listeners installed in the browser, by the element that owns them.
     * Each listener is given as the plain values it carries, depth first and in
     * order — property names, literals and the event name, but neither elements
     * nor function bodies — so a test can tell which property a listener reads,
     * which it writes, and on which event.
     *
     * @return the values of each sent listener, keyed by its owner
     */
    public Map<Element, List<List<Object>>> sentListeners() {
        return sent().stream()
                .filter(execution -> execution.getParameters().stream()
                        .anyMatch(JsFunction.class::isInstance))
                .collect(Collectors.groupingBy(
                        ExecutedJavaScript.Execution::getElement,
                        LinkedHashMap::new,
                        Collectors.mapping(execution -> execution
                                .getParameters().stream()
                                .filter(JsFunction.class::isInstance)
                                .flatMap(PrintTestSupport::values).toList(),
                                Collectors.toList())));
    }

    private List<ExecutedJavaScript.Execution> sent() {
        return executed.getAll();
    }

    private static Stream<Object> values(Object value) {
        if (value instanceof JsFunction function) {
            return function.getCaptures().stream()
                    .flatMap(PrintTestSupport::values);
        }
        return value instanceof Element ? Stream.empty() : Stream.of(value);
    }

    @SuppressWarnings("removal")
    private static Stream<String> text(Object value) {
        if (value instanceof JsFunction function) {
            // A function declared by a JavaScript definition has no body:
            // its JavaScript is the declared expression, and its arguments
            // carry the rest, such as the event name.
            JsCall call = function.getDeclaredCall();
            Stream<String> code = call == null ? Stream.of(function.getBody())
                    : Stream.concat(Stream.of(call.getExpression()),
                            call.arguments().stream()
                                    .flatMap(PrintTestSupport::text));
            return Stream.concat(code, function.getCaptures().stream()
                    .flatMap(PrintTestSupport::text));
        }
        return value instanceof String string ? Stream.of(string)
                : Stream.empty();
    }
}
