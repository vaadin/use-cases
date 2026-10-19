package com.example.uc1;

import java.time.Duration;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;

/**
 * UC1 — Copy static text.
 * <p>
 * {@code Clipboard.onClick(button).writeText(LINK, onCopied, onError)} copies a
 * text that is known when the view is built. The copy runs in the browser's
 * click handler, where the user gesture is still valid. Once the browser
 * reports success, the {@code onCopied} callback shows "Copied" with a check
 * icon on the button, and {@link UI#triggerAfter} sets the label back one
 * second later. The timer runs in the browser, so no server-side scheduler is
 * needed. Because the feedback waits for the browser's answer, it appears one
 * round trip after the click.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Copy static text")
@UseCaseDescription("Copying a fixed text with one click and confirming it")
@Menu(order = 1, title = "UC1 — Copy static text")
public class CopyStaticTextView extends VerticalLayout {

    static final String LINK = "https://example.com/share/abc123";
    static final String DEFAULT_LABEL = "Copy link";
    static final String FLASH_LABEL = "Copied";

    private final Button copyButton = new Button(DEFAULT_LABEL);
    private @Nullable Registration pendingReset;

    public CopyStaticTextView() {
        add(new H1("UC1 — Copy static text"));
        add(new Paragraph("The text is known when the view is built. Click "
                + "the button: the browser copies the text, and once it "
                + "reports success the button shows 'Copied' for a second."));

        add(new Paragraph("Text to copy:"));
        add(new Pre(LINK));

        Clipboard.onClick(copyButton).writeText(LINK, copied -> flashCopied(),
                error -> Notification.show("Copy failed: " + error.message()));
        add(copyButton);

        // A reset still pending when the view is detached would run on a
        // button nobody sees; reset now and cancel the timer instead.
        whenAttached(ui -> this::reset);
    }

    private void flashCopied() {
        // A second click within the second restarts the timer, so the label
        // does not jump back early.
        cancelPendingReset();
        copyButton.setText(FLASH_LABEL);
        copyButton.setIcon(VaadinIcon.CHECK.create());
        pendingReset = UI.getCurrentOrThrow()
                .triggerAfter(Duration.ofSeconds(1), this::reset);
    }

    private void reset() {
        cancelPendingReset();
        copyButton.setText(DEFAULT_LABEL);
        copyButton.setIcon(null);
    }

    private void cancelPendingReset() {
        if (pendingReset != null) {
            pendingReset.remove();
            pendingReset = null;
        }
    }
}
