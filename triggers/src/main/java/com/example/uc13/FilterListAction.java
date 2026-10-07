package com.example.uc13;

import java.util.Objects;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.trigger.internal.Action;
import com.vaadin.flow.component.trigger.internal.Trigger;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;

/**
 * Custom {@link Action} that hides children of a list container whose
 * {@code textContent} doesn't contain the search value read from
 * {@code event.target.value}. Filtering is entirely client-side — no server
 * round-trip per keystroke.
 * <p>
 * The action is intentionally specialised: it relies on the surrounding trigger
 * being a DOM event trigger on the search field, so {@code event.target} is the
 * field. A more general version would accept an {@code Action.Input<String>}
 * for the query, but {@code Action.Input#toJs} is currently package-private —
 * application code can't call it. See API-GAPS.md.
 */
public class FilterListAction extends Action {

    private final Element listContainer;

    public FilterListAction(Component listContainer) {
        this(Objects.requireNonNull(listContainer).getElement());
    }

    public FilterListAction(Element listContainer) {
        this.listContainer = Objects.requireNonNull(listContainer);
    }

    @Override
    protected JsFunction toJs(Trigger trigger) {
        return JsFunction
                .of("""
                        const q = ((event.target && event.target.value) || '').toLowerCase();
                        for (const row of $0.children) {
                            row.hidden = q && !row.textContent.toLowerCase().includes(q);
                        }""",
                        listContainer)
                .withArguments("event");
    }
}
