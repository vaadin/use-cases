package com.example;

import java.util.Optional;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.shared.HasTextSelection;
import com.vaadin.flow.dom.DebouncePhase;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.JsCall;

/**
 * Text selection in browserless tests, which have no simulator for it
 * ({@code API-GAPS.md} #2). {@link #selectInBrowser} plays the browser's part
 * of {@link MissingAPI#selectionSignal}; {@link #clickAndReadSelection} reads
 * the selection that the server asked the browser to make.
 */
public final class TextSelectionTestSupport {

    /**
     * A selection the server asked the browser to make. {@code selectAll()}
     * asks for {@code 0} to {@link Integer#MAX_VALUE}.
     */
    public record Requested(int start, int end) {
    }

    private TextSelectionTestSupport() {
    }

    /**
     * Acts as if the user selected the characters from {@code start} to
     * {@code end} of the field's value; equal indexes place the cursor.
     */
    public static <F extends Component & HasTextSelection & HasValue<?, String>> void selectInBrowser(
            F field, int start, int end) {
        String value = field.getValue() == null ? "" : field.getValue();
        ObjectNode data = JacksonUtils.createObjectNode();
        data.put(MissingAPI.START, start);
        data.put(MissingAPI.END, end);
        data.put(MissingAPI.CONTENT, value.substring(start, end));
        // The listener is debounced, so it only takes the event the browser
        // sends when the debounce timeout ends.
        data.put("for", DebouncePhase.TRAILING.getIdentifier());

        Element element = field.getElement();
        element.getNode().getFeature(ElementListenerMap.class).fireEvent(
                new DomEvent(element, MissingAPI.SELECTION_CHANGE, data));
    }

    /**
     * Clicks the button and returns the last selection that the click asked the
     * browser to make in the field, if any.
     * <p>
     * The button tester also sends the response to the mocked browser, which
     * drops the JavaScript calls before a test can read them. So the click
     * event is fired directly, after the checks the tester makes, and the calls
     * are read before anything is sent.
     */
    public static Optional<Requested> clickAndReadSelection(Button button,
            Component field) {
        if (!button.isEnabled() || !button.isVisible()
                || !button.isAttached()) {
            throw new IllegalStateException(
                    "Button '" + button.getText() + "' is not usable");
        }
        UIInternals internals = UI.getCurrent().getInternals();
        // Drop the calls that are already queued, such as the navigation's.
        internals.getStateTree().runExecutionsBeforeClientResponse();
        internals.dumpPendingJavaScriptInvocations();

        ComponentUtil.fireEvent(button, new ClickEvent<>(button));

        // Element.executeJs queues its call until the response is written.
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(call -> call.getOwner() == field.getElement().getNode())
                .map(call -> call.getInvocation().getJsCall())
                .filter(TextSelectionTestSupport::isSetSelectionRange)
                .map(call -> new Requested((Integer) call.arguments().get(0),
                        (Integer) call.arguments().get(1)))
                .reduce((first, second) -> second);
    }

    private static boolean isSetSelectionRange(JsCall call) {
        return call != null
                && HasTextSelection.class
                        .equals(call.definitionType().getEnclosingClass())
                && call.methodName().equals("setSelectionRange");
    }
}
