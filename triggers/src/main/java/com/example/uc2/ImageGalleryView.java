package com.example.uc2;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.trigger.internal.ClickTrigger;
import com.vaadin.flow.component.trigger.internal.SetPropertyAction;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC2 — Image gallery select.
 * <p>
 * Three {@link Image} tiles. One {@link ClickTrigger} per tile runs three
 * {@link SetPropertyAction}s in the same click: the clicked tile gets the
 * {@code uc2-selected} class and the other two get {@code uc2-dimmed}. A
 * "Reset" button clears the classes of all three the same way. Nothing goes to
 * the server.
 * <p>
 * With server-side click listeners, the same change would arrive after a
 * round-trip. {@link SetPropertyAction} writes any property of any element, so
 * the targets do not need to be components with an API for it.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Image gallery select")
@UseCaseDescription("Highlighting a clicked image and dimming the others in one client-side step")
@Menu(order = 2, title = "UC2 — Image gallery select")
@StyleSheet("uc2.css")
public class ImageGalleryView extends VerticalLayout {

    public ImageGalleryView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Image gallery select"));
        add(new Paragraph("Click one tile: it gets a border and the other two "
                + "fade to gray, in the same click and without a server "
                + "round-trip. One ClickTrigger per tile runs three "
                + "SetPropertyActions that set the class names of the three "
                + "images. Reset clears them the same way."));

        // The tile colors are part of the pictures, not of the page style.
        Image a = tile("a", "ALPHA", "1976d2");
        Image b = tile("b", "BETA", "d81b60");
        Image c = tile("c", "GAMMA", "388e3c");

        HorizontalLayout gallery = new HorizontalLayout(a, b, c);
        gallery.addClassName("uc2-gallery");

        wire(a, b, c);
        wire(b, a, c);
        wire(c, a, b);

        Button reset = new Button("Reset");
        reset.setId("reset");
        reset.addClassName("uc2-reset");
        new ClickTrigger(reset).triggers(
                new SetPropertyAction<>(a, "className", ""),
                new SetPropertyAction<>(b, "className", ""),
                new SetPropertyAction<>(c, "className", ""));

        add(gallery, reset);
    }

    private static void wire(Image self, Image other1, Image other2) {
        new ClickTrigger(self).triggers(
                new SetPropertyAction<>(self, "className", "uc2-selected"),
                new SetPropertyAction<>(other1, "className", "uc2-dimmed"),
                new SetPropertyAction<>(other2, "className", "uc2-dimmed"));
    }

    private static Image tile(String id, String label, String hexColor) {
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' "
                + "viewBox='0 0 200 200'>"
                + "<rect width='200' height='200' fill='#" + hexColor + "'/>"
                + "<text x='100' y='115' text-anchor='middle' fill='white' "
                + "font-size='28' font-family='sans-serif' "
                + "font-weight='600'>" + label + "</text>" + "</svg>";
        String dataUri = "data:image/svg+xml;base64," + Base64.getEncoder()
                .encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        Image img = new Image(dataUri, label);
        img.setId(id);
        return img;
    }
}
