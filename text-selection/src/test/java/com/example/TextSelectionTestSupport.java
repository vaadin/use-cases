package com.example;

import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.shared.HasTextSelection;
import com.vaadin.flow.dom.DomEvent;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;

/**
 * Shared helper for browserless tests that need to drive the
 * {@code selectionSignal()} of a TextField/TextArea without a real browser. The
 * signal is filled by a {@code vaadin-text-selection-change} DOM event dispatched
 * from the client; tests fire the same event directly through
 * {@link ElementListenerMap}, mirroring flow-components' own
 * {@code SelectionSignalTest}.
 */
public final class TextSelectionTestSupport {

    private TextSelectionTestSupport() {
    }

    public static void setSelection(HasTextSelection field, int start, int end) {
        // Ensure the signal — and therefore its DOM listener — is created
        // before we fire the event. selectionSignal() is lazy.
        field.selectionSignal();

        String value = readValue(field);
        int safeStart = Math.max(0, Math.min(start, value.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, value.length()));
        String content = value.substring(safeStart, safeEnd);

        ObjectNode data = JacksonUtils.createObjectNode();
        data.put("event.detail.start", safeStart);
        data.put("event.detail.end", safeEnd);
        data.put("event.detail.content", content);

        Element element = ((Component) field).getElement();
        DomEvent event = new DomEvent(element, "vaadin-text-selection-change", data);
        element.getNode().getFeature(ElementListenerMap.class).fireEvent(event);
    }

    private static String readValue(HasTextSelection field) {
        try {
            Object v = field.getClass().getMethod("getValue").invoke(field);
            return v == null ? "" : v.toString();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(
                    "HasTextSelection field does not expose getValue()", e);
        }
    }
}
