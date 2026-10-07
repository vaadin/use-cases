package com.example.uc3;

import java.time.LocalTime;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.SignalInput;
import com.vaadin.flow.component.trigger.internal.WriteToClipboardAction;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC3 — Copy a share link that the UI never renders.
 * <p>
 * The server keeps the full URL in a {@link ValueSignal} that is bound to
 * <em>nothing</em>: no Span, no hidden TextField, no DOM element. Typing in the
 * slug field updates the signal server-side via a normal value-change listener.
 * On click, {@link WriteToClipboardAction} reads the signal at fire-time via
 * {@link SignalInput} and writes {@code text/plain} inside the original user
 * gesture — pure client-side, the value never crosses the DOM.
 * <p>
 * The framework analogue would have to either render the URL into a hidden
 * field just to read it back, or round-trip from the click to a server-side
 * method that calls {@code executeJs} for the clipboard — and that follow-up no
 * longer counts as a user gesture, so the browser rejects the clipboard write.
 * {@code SignalInput} skips both problems.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Copy a hidden share link")
@Menu(order = 3, title = "UC3 — Hidden share link")
@StyleSheet("uc3.css")
public class LiveSignalCounterView extends VerticalLayout {

    public LiveSignalCounterView() {
        addClassName("uc3-view");
        add(new H1("UC3 — Copy a share link that's never rendered"));
        add(new Paragraph(
                "Type a slug. A server-side ValueSignal<String> keeps the "
                        + "full URL but isn't bound to any UI element — open "
                        + "the page's DOM and you won't find it. Click Copy "
                        + "and SignalInput reads the signal's current value "
                        + "at the moment of the gesture. The confirmation "
                        + "line below is updated by the onCopied callback "
                        + "for evidence."));

        ValueSignal<String> shareLink = new ValueSignal<>(buildUrl(""));

        TextField slug = new TextField("Slug");
        slug.setId("slug");
        slug.setValueChangeMode(ValueChangeMode.EAGER);
        slug.addValueChangeListener(e -> shareLink.set(buildUrl(e.getValue())));

        Span confirmation = new Span("(no copy yet)");
        confirmation.setId("confirmation");
        confirmation.addClassName("confirmation");

        Button copy = new Button("Copy share link");
        copy.setId("copy");

        new ClickTrigger(copy).triggers(new WriteToClipboardAction(
                new SignalInput<>(copy, shareLink), null,
                copied -> confirmation.setText("Copied at "
                        + LocalTime.now().withNano(0) + ": " + copied),
                err -> confirmation.setText("Copy failed: " + err.message())));

        add(slug, new HorizontalLayout(copy, confirmation));
    }

    private static String buildUrl(String slug) {
        return "https://example.com/r/" + slug + "?from=demo";
    }
}
