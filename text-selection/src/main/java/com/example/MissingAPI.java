package com.example;

import java.io.Serializable;

import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.shared.HasTextSelection;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Workarounds for text selection features that have no Java API yet.
 * {@link HasTextSelection} can set the selection but not read it back; each
 * method names its {@code API-GAPS.md} entry and how to migrate once the real
 * API exists.
 */
public final class MissingAPI {

    /** The DOM event that reports a new selection. */
    static final String SELECTION_CHANGE = "selectionchange";

    /** Event data expressions, evaluated in the browser for each event. */
    static final String START = "element.inputElement.selectionStart";
    static final String END = "element.inputElement.selectionEnd";
    static final String CONTENT = "element.inputElement.value.substring("
            + START + ", " + END + ")";

    private static final String DATA_KEY = MissingAPI.class.getName()
            + ".selection";

    private MissingAPI() {
    }

    /**
     * The selection in a text field, as the browser last reported it. The
     * indexes use the same rules as {@code HTMLInputElement.selectionStart} and
     * {@code selectionEnd}; when nothing is selected, {@code start} and
     * {@code end} are both the cursor position.
     *
     * @param start
     *            index of the first selected character
     * @param end
     *            index after the last selected character
     * @param content
     *            the selected text
     */
    public record TextSelection(int start, int end,
            String content) implements Serializable {

        /** The selection before the browser has reported one. */
        public static final TextSelection NONE = new TextSelection(0, 0, "");

        public int length() {
            return end - start;
        }

        public boolean isEmpty() {
            return start == end;
        }
    }

    /**
     * A read-only signal with the current selection of the field. The same
     * signal is returned for each call on the same field. It starts as
     * {@link TextSelection#NONE} and follows the selection while the field is
     * attached.
     * <p>
     * Missing ({@code API-GAPS.md} #1): a way to read the selection or the
     * cursor position on the server. {@code HasTextSelection} only has
     * {@code setSelectionRange}, {@code selectAll} and
     * {@code setCursorPosition}. The browser fires {@code selectionchange} on
     * the {@code <input>} or {@code <textarea>} each time the selection or the
     * cursor moves; a debounced DOM listener sends the new range to the server.
     * Migrate by replacing the call with the field's own selection signal or
     * getter once there is one, and delete this method.
     */
    public static Signal<TextSelection> selectionSignal(
            HasTextSelection field) {
        Component component = (Component) field;
        @SuppressWarnings("unchecked")
        ValueSignal<TextSelection> signal = (ValueSignal<TextSelection>) ComponentUtil
                .getData(component, DATA_KEY);
        if (signal == null) {
            ValueSignal<TextSelection> created = new ValueSignal<>(
                    TextSelection.NONE);
            Element element = component.getElement();
            // A DOM listener stays registered across detach and re-attach, so
            // it is added once, here, and needs no cleanup.
            element.addEventListener(SELECTION_CHANGE,
                    event -> created.set(read(event.getEventData())))
                    .addEventData(START).addEventData(END).addEventData(CONTENT)
                    .debounce(100);
            ComponentUtil.setData(component, DATA_KEY, created);
            signal = created;
        }
        return signal.asReadonly();
    }

    private static TextSelection read(JsonNode data) {
        int start = data.path(START).asInt(0);
        int end = Math.max(start, data.path(END).asInt(start));
        return new TextSelection(start, end, data.path(CONTENT).asString(""));
    }
}
