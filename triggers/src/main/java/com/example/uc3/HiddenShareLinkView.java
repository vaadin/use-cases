package com.example.uc3;

import java.time.LocalTime;

import com.example.common.UseCaseDescription;
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
 * UC3 — Hidden share link.
 * <p>
 * The server keeps the full share URL in a {@link ValueSignal} that no
 * component shows: no text, no hidden field. Typing in the slug field updates
 * the signal. The Copy button's {@link WriteToClipboardAction} reads the URL
 * through a {@link SignalInput}, which keeps a copy of the signal's value in a
 * JavaScript property of the button and updates it whenever the signal changes.
 * The click reads that property and writes it to the clipboard inside the
 * click, then reports the copied text back so the view can confirm it.
 * <p>
 * Without {@link SignalInput}, the URL would have to be rendered into a hidden
 * field just to be read back, or the click would go to the server first, and a
 * clipboard write after that round-trip is no longer part of the click, which
 * Safari and Firefox reject.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Hidden share link")
@UseCaseDescription("Copying a server-side value that is never rendered in the UI")
@Menu(order = 3, title = "UC3 — Hidden share link")
@StyleSheet("uc3.css")
public class HiddenShareLinkView extends VerticalLayout {

    public HiddenShareLinkView() {
        addClassName("uc3-view");
        add(new H1("UC3 — Hidden share link"));
        add(new Paragraph("Type a slug, then click Copy and paste the link "
                + "anywhere. The server builds the full URL in a "
                + "ValueSignal that no component shows, so you will not find "
                + "it on the page. SignalInput keeps a copy of it in a "
                + "JavaScript property of the Copy button, and the click "
                + "writes that to the clipboard. The line next to the button "
                + "shows what the browser reported as copied."));

        ValueSignal<String> shareLink = new ValueSignal<>(buildUrl(""));

        TextField slug = new TextField("Slug");
        slug.setId("slug");
        slug.setValueChangeMode(ValueChangeMode.EAGER);
        slug.addValueChangeListener(e -> shareLink.set(buildUrl(e.getValue())));

        Span confirmation = new Span("(no copy yet)");
        confirmation.setId("confirmation");
        confirmation.addClassName("uc3-confirmation");

        Button copy = new Button("Copy share link");
        copy.setId("copy");

        new ClickTrigger(copy).triggers(new WriteToClipboardAction(
                new SignalInput<>(copy, shareLink), null,
                copied -> confirmation.setText("Copied at "
                        + LocalTime.now().withNano(0) + ": " + copied),
                err -> confirmation.setText("Copy failed: " + err.message())));

        HorizontalLayout copyRow = new HorizontalLayout(copy, confirmation);
        copyRow.addClassName("uc3-copy-row");
        add(slug, copyRow);
    }

    private static String buildUrl(String slug) {
        return "https://example.com/r/" + slug + "?from=demo";
    }
}
