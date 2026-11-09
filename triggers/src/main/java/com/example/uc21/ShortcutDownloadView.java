package com.example.uc21;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.example.ShortcutTrigger;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.DownloadAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC21 — Shortcut download.
 * <p>
 * Press Ctrl+Shift+D (Cmd+Shift+D on a Mac) anywhere on the page. A
 * {@link ShortcutTrigger} runs a {@link DownloadAction} whose
 * {@link DownloadHandler} produces a CSV file on the server. The download
 * starts inside the key press, without a round-trip first, and the trigger
 * stops the browser's own shortcut for the same keys.
 * <p>
 * {@code Anchor} with a download handler starts a download on a click only.
 * With triggers, the same handler can follow any gesture: the same
 * {@link DownloadAction} is also wired to a button here.
 */
@Route(value = "uc21", layout = MainLayout.class)
@PageTitle("UC21 — Shortcut download")
@UseCaseDescription("Starting a server-generated download from a keyboard shortcut")
@Menu(order = 21, title = "UC21 — Shortcut download")
@StyleSheet("uc21.css")
public class ShortcutDownloadView extends VerticalLayout {

    private static final String CSV = """
            id,name,email
            1,Ada Lovelace,ada@example.com
            2,Alan Turing,alan@example.com
            3,Grace Hopper,grace@example.com
            """;

    public ShortcutDownloadView() {
        addClassName("uc21-view");
        add(new H1("UC21 — Shortcut download"));
        add(new Paragraph("Press Ctrl+Shift+D (Cmd+Shift+D on a Mac) "
                + "anywhere on this page, or click Download CSV. Either one "
                + "downloads people.csv, which the server generates with the "
                + "content shown below."));

        add(new H2("Preview"));
        Pre preview = new Pre(CSV);
        preview.addClassName("uc21-preview");
        add(preview);

        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            byte[] bytes = CSV.getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(new ByteArrayInputStream(bytes),
                    "people.csv", "text/csv", bytes.length);
        }, "people.csv");

        // The modifiers must match exactly, so Ctrl and Cmd each need their
        // own trigger.
        new ShortcutTrigger(this, Key.KEY_D, KeyModifier.CONTROL,
                KeyModifier.SHIFT).triggers(new DownloadAction(handler));
        new ShortcutTrigger(this, Key.KEY_D, KeyModifier.META,
                KeyModifier.SHIFT).triggers(new DownloadAction(handler));

        Button download = new Button("Download CSV");
        download.setId("download");
        new ClickTrigger(download).triggers(new DownloadAction(handler));
        add(download);
    }
}
