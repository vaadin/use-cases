package com.example.uc4;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

/**
 * UC4 — Copy image data.
 * <p>
 * {@link Clipboard#onClick(com.vaadin.flow.component.Component)
 * Clipboard.onClick(button).writeImage(image)} draws the source component's
 * root {@code <img>} onto a canvas in the browser and writes the resulting PNG
 * to the clipboard, all inside the click handler so the user gesture is
 * preserved. The {@link Image} here is backed by a {@link StreamResource} that
 * produces a small SVG, so the example doesn't depend on any external assets.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Copy image")
@UseCaseDescription("Copying an image so it can be pasted elsewhere")
@Menu(order = 4, title = "UC4 — Copy image")
public class CopyImageView extends VerticalLayout {

    private static final String CHART_SVG = """
            <svg xmlns="http://www.w3.org/2000/svg" width="300" height="200">
              <rect width="300" height="200" fill="#4A90D9"/>
              <text x="150" y="40" text-anchor="middle" fill="white"
                    font-size="20" font-family="sans-serif">Sample Chart</text>
              <rect x="40"  y="110" width="40" height="60"  fill="#81B5E8"/>
              <rect x="90"  y="90"  width="40" height="80"  fill="#81B5E8"/>
              <rect x="140" y="70"  width="40" height="100" fill="#81B5E8"/>
              <rect x="190" y="100" width="40" height="70"  fill="#81B5E8"/>
              <rect x="240" y="120" width="40" height="50"  fill="#81B5E8"/>
            </svg>
            """;

    public CopyImageView() {
        add(new H1("UC4 — Copy image"));
        add(new Paragraph(
                "The image is rasterised to PNG on the client and written to "
                        + "the clipboard inside the click handler, so the user "
                        + "gesture is preserved. Paste into an image-aware "
                        + "destination (chat, doc editor, image viewer)."));

        StreamResource resource = new StreamResource("chart.svg",
                () -> new ByteArrayInputStream(
                        CHART_SVG.getBytes(StandardCharsets.UTF_8)));
        resource.setContentType("image/svg+xml");

        Image preview = new Image(resource, "Sample chart");
        preview.setWidth("300px");

        Button copyButton = new Button("Copy chart");
        Clipboard.onClick(copyButton).writeImage(preview,
                () -> Notification.show("Chart copied"),
                error -> Notification.show("Copy failed: " + error.message()));

        HorizontalLayout row = new HorizontalLayout(preview, copyButton);
        row.setAlignItems(Alignment.START);
        add(row);
    }
}
