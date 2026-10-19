package com.example.uc13;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Custom {@link Action} that hides children of a list container whose
 * {@code textContent} doesn't contain the query produced by an
 * {@link Action.Input}. Filtering is entirely client-side — no server
 * round-trip per keystroke.
 * <p>
 * The query source is any {@code Action.Input<String>} — typically a
 * {@code PropertyInput} reading the search field's {@code value} — so the
 * action composes with any trigger, mirroring
 * {@code SetPropertyAction(target, name, source)}.
 */
public class FilterListAction extends Action {

    private final Element listContainer;
    private final Action.Input<String> query;

    public FilterListAction(Component listContainer,
            Action.Input<String> query) {
        this(Objects.requireNonNull(listContainer).getElement(), query);
    }

    public FilterListAction(Element listContainer, Action.Input<String> query) {
        this.listContainer = Objects.requireNonNull(listContainer);
        this.query = Objects.requireNonNull(query);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction
                .of("""
                        const q = ($1(event) || '').toLowerCase();
                        for (const row of $0.children) {
                            row.hidden = q && !row.textContent.toLowerCase().includes(q);
                        }""",
                        listContainer, query.toJs(trigger))
                .withArguments("event");
    }
}
