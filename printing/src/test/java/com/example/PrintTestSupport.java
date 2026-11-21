package com.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.browserless.BrowserlessTestConfig;
import com.vaadin.browserless.ExecutedJavaScript;
import com.vaadin.browserless.JavaScriptHandler;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.js.JsCall;

/**
 * What the views asked the browser to do. Printing itself cannot be observed
 * without a browser, but the JavaScript a view sends can — and since Flow has
 * no printing API, that JavaScript <em>is</em> the behaviour under test.
 * <p>
 * The checks look at everything sent to the browser of the current UI since it
 * was created, including what the navigation sent. They need the
 * {@link Browser} recorder, registered with
 * {@code @BrowserlessTestConfig(javaScriptHandlers = PrintTestSupport.Browser.class)}.
 */
public final class PrintTestSupport {

    /**
     * Records the JavaScript sent to the browser of each UI. Register it with
     * {@link BrowserlessTestConfig}.
     */
    public static final class Browser implements JavaScriptHandler {

        @Override
        public void handle(ExecutedJavaScript.Execution execution) {
            received(execution.getUI()).add(execution);
        }
    }

    private PrintTestSupport() {
    }

    /** Whether something opened the browser's print dialog. */
    public static boolean printRequested() {
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
    public static boolean sentJsContains(String fragment) {
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
    public static boolean sentJsMentions(String... fragments) {
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
    public static Map<Element, List<List<Object>>> sentListeners() {
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

    private static List<ExecutedJavaScript.Execution> sent() {
        UI ui = Objects.requireNonNull(UI.getCurrent());
        // Starting a capture sends whatever is still pending to the browser
        ExecutedJavaScript.capture(ui).stop();
        return received(ui);
    }

    @SuppressWarnings("unchecked")
    private static List<ExecutedJavaScript.Execution> received(UI ui) {
        List<ExecutedJavaScript.Execution> received = (List<ExecutedJavaScript.Execution>) ComponentUtil
                .getData(ui, Browser.class.getName());
        if (received == null) {
            received = new ArrayList<>();
            ComponentUtil.setData(ui, Browser.class.getName(), received);
        }
        return received;
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
