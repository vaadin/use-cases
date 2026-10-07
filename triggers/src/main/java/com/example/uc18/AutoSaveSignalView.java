package com.example.uc18;

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
 * UC18 — Auto-save signal on every keystroke.
 * <p>
 * A {@code DomEventTrigger} on the textarea's {@code input} event fires a
 * {@link SetSignalAction} that pushes the field's current value into a
 * server-side {@link ValueSignal}. The reactive char/word counts are bound to
 * the signal — they update on every keystroke without any Java handler running
 * per event.
 * <p>
 * Contrast with
 * {@link com.vaadin.flow.component.trigger.internal.CallbackAction} (UC8,
 * UC11): that routes through a {@code Consumer<T>} the application supplies;
 * {@code SetSignalAction} skips the consumer, hooking the typing stream
 * directly into the signal graph. Downstream UI updates flow from signal
 * effects.
 */
@Route(value = "uc18", layout = MainLayout.class)
@PageTitle("UC18 — Auto-save signal")
@Menu(order = 18, title = "UC18 — Auto-save signal")
@StyleSheet("uc18.css")
public class AutoSaveSignalView extends VerticalLayout {

    public AutoSaveSignalView() {
        addClassName("uc18-view");
        add(new H1("UC18 — Auto-save signal"));
        add(new Paragraph(
                "Type in the box. The character and word counts below come "
                        + "from a server-side ValueSignal that's updated on "
                        + "every keystroke via SetSignalAction. The Spans bind "
                        + "to mapped views of the signal — no per-keystroke "
                        + "Java handler runs."));

        TextArea draft = new TextArea();
        draft.setId("draft");
        draft.setPlaceholder("Start typing…");
        draft.addClassName("draft");

        ValueSignal<String> text = new ValueSignal<>("");

        Span charCount = new Span();
        charCount.setId("chars");
        charCount.addClassName("count");
        charCount.bindText(text.map(s -> s.length() + " characters"));

        Span wordCount = new Span();
        wordCount.setId("words");
        wordCount.addClassName("count");
        wordCount.bindText(text.map(s -> {
            String trimmed = s.trim();
            int count = trimmed.isEmpty() ? 0 : trimmed.split("\\s+").length;
            return count + " word" + (count == 1 ? "" : "s");
        }));

        new DomEventTrigger(draft, "input")
                .triggers(new SetSignalAction<>(text, String.class,
                        new PropertyInput<>(draft, "value", String.class)));

        HorizontalLayout counts = new HorizontalLayout(charCount, wordCount);
        counts.addClassName("counts");
        add(draft, counts);
    }
}
