package com.example.uc21;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.example.ShortcutTrigger;
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
 * UC21 — Keyboard shortcut triggers a server-handler download.
 * <p>
 * Press Ctrl+Shift+D anywhere in the view. A {@link ShortcutTrigger} fires
 * a {@link DownloadAction} whose {@link DownloadHandler} produces a CSV on the
 * fly. The gesture context is preserved through the trigger fire, so the
 * browser permits the download — calling
 * {@code element.executeJs("location.href=…")} from a server push would be
 * blocked in many browsers.
 * <p>
 * Vaadin's high-level {@code Anchor} (and the {@code Download} component in the
 * {@code download} module) wire downloads to click only. The trigger API lets
 * the same handler bind to any user-gesture trigger — keyboard shortcut here,
 * long-press or double-click elsewhere.
 */
@Route(value = "uc21", layout = MainLayout.class)
@PageTitle("UC21 — Shortcut download")
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
        add(new Paragraph(
                "Press Ctrl+Shift+D anywhere in this view, or click the "
                        + "Download CSV button. Either gesture fires a "
                        + "DownloadAction whose handler generates the CSV "
                        + "preview below on the fly; the browser permits the "
                        + "download because the keystroke or click is the user "
                        + "gesture."));

        add(new H2("Preview"));
        Pre preview = new Pre(CSV);
        preview.addClassName("preview");
        add(preview);

        DownloadHandler handler = DownloadHandler.fromInputStream(event -> {
            byte[] bytes = CSV.getBytes(StandardCharsets.UTF_8);
            return new DownloadResponse(new ByteArrayInputStream(bytes),
                    "people.csv", "text/csv", bytes.length);
        }, "people.csv");

        new ShortcutTrigger(this, Key.KEY_D, KeyModifier.CONTROL,
                KeyModifier.SHIFT).triggers(new DownloadAction(handler));

        // The same handler bound to a click gesture — the trigger API lets one
        // handler back any user-gesture trigger, keyboard shortcut or button.
        Button download = new Button("Download CSV");
        new ClickTrigger(download).triggers(new DownloadAction(handler));
        add(download);
    }
}
