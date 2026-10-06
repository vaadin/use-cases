package com.example.uc1;

import java.time.Duration;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

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

/**
 * UC1 — Copy static text on click, with in-button "Copied" feedback applied
 * from the server.
 * <p>
 * Wires the click via the public {@link Clipboard#onClick} API. The server-side
 * {@code onCopied} callback sets the button label to "Copied" and adds a check
 * icon; a one-second {@code setTimeout} on the client dispatches a custom DOM
 * event that a server-side listener uses to revert the button. Keeps the demo
 * working without {@code @Push} or a server-side scheduled executor.
 * <p>
 * Trade-off vs the trigger-action approach: there is a round-trip delay between
 * the click and the visual feedback appearing, because the "Copied" mutation
 * only happens after the clipboard promise resolves and the outcome reaches the
 * server. The trigger-action approach instead ran the visual flash
 * synchronously inside the click handler.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Copy static text on click")
@UseCaseDescription("Copying a fixed text with one click and confirming it")
@Menu(order = 1, title = "UC1 — Copy static text")
public class CopyStaticTextView extends VerticalLayout {

    private static final String LINK = "https://example.com/share/abc123";
    private static final String DEFAULT_LABEL = "Copy link";
    private static final String FLASH_LABEL = "Copied";

    public CopyStaticTextView() {
        add(new H1("UC1 — Copy static text on click"));
        add(new Paragraph(
                "The text is known when the view is rendered. The copy "
                        + "happens client-side in the click handler. The "
                        + "server-side success callback then flashes "
                        + "'Copied' on the button for a second instead of "
                        + "opening a notification."));

        add(new Paragraph("Text to copy:"));
        add(new Pre(LINK));

        Button copyButton = new Button(DEFAULT_LABEL);

        Clipboard.onClick(copyButton).writeText(LINK, copied -> {
            copyButton.setText(FLASH_LABEL);
            copyButton.setIcon(VaadinIcon.CHECK.create());
            UI.getCurrentOrThrow().triggerAfter(Duration.ofSeconds(1), () -> {
                copyButton.setText(DEFAULT_LABEL);
                copyButton.setIcon(null);
            });
        }, error -> Notification.show("Copy failed: " + error.message()));

        add(copyButton);
    }
}
