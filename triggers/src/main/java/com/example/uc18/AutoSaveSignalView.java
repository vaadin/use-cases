package com.example.uc18;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.trigger.internal.DomEventTrigger;
import com.vaadin.flow.component.trigger.internal.PropertyInput;
import com.vaadin.flow.component.trigger.internal.SetSignalAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC18 — Auto-save signal.
 * <p>
 * A {@code DomEventTrigger} on the text area's {@code input} event runs a
 * {@link SetSignalAction} that sends the current text to a {@link ValueSignal}
 * on the server on every keystroke. The character and word counts are bound to
 * the signal, so the view has no listener of its own: the counts follow the
 * signal.
 * <p>
 * Compare with
 * {@link com.vaadin.flow.component.trigger.internal.CallbackAction} (UC8,
 * UC11), which passes the value to a callback that the application writes.
 * {@code SetSignalAction} writes it straight into the signal.
 */
@Route(value = "uc18", layout = MainLayout.class)
@PageTitle("UC18 — Auto-save signal")
@UseCaseDescription("Pushing every keystroke into a server signal without a Java handler")
@Menu(order = 18, title = "UC18 — Auto-save signal")
@StyleSheet("uc18.css")
public class AutoSaveSignalView extends VerticalLayout {

    public AutoSaveSignalView() {
        addClassName("uc18-view");
        add(new H1("UC18 — Auto-save signal"));
        add(new Paragraph("Type in the box. Every keystroke sends the text "
                + "to a signal on the server with SetSignalAction, and the "
                + "character and word counts below are bound to that signal. "
                + "The view has no listener of its own."));

        TextArea draft = new TextArea();
        draft.setId("draft");
        draft.setPlaceholder("Start typing…");
        draft.addClassName("uc18-draft");

        ValueSignal<String> text = new ValueSignal<>("");

        Span charCount = new Span();
        charCount.setId("chars");
        charCount.addClassName("uc18-count");
        charCount.bindText(text.map(s -> s.length() + " characters"));

        Span wordCount = new Span();
        wordCount.setId("words");
        wordCount.addClassName("uc18-count");
        wordCount.bindText(text.map(s -> {
            String trimmed = s.trim();
            int count = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
            return count + " word" + (count == 1 ? "" : "s");
        }));

        new DomEventTrigger(draft, "input")
                .triggers(new SetSignalAction<>(text, String.class,
                        new PropertyInput<>(draft, "value", String.class)));

        HorizontalLayout counts = new HorizontalLayout(charCount, wordCount);
        counts.addClassName("uc18-counts");
        add(draft, counts);
    }
}
