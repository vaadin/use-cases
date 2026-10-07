package com.example.uc1;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC1 — Image lightbox.
 * <p>
 * A grid of image thumbnails. Clicking one swaps it into the preview pane below
 * and immediately fullscreens that pane via
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(...).enter(stage)}. The browser renders only the wrapped
 * stage; pressing Escape returns to the gallery.
 */
@Route(value = "uc1", layout = MainLayout.class)
@Menu(order = 1, title = "UC1 — Image lightbox")
@StyleSheet("uc1.css")
public class ImageLightboxView extends VerticalLayout {

    private record Photo(String name, String gradient) {
    }

    private static final List<Photo> PHOTOS = List.of(
            new Photo("Sunset",
                    "linear-gradient(135deg, #ff6e7f 0%, #bfe9ff 100%)"),
            new Photo("Forest",
                    "linear-gradient(135deg, #134e5e 0%, #71b280 100%)"),
            new Photo("Desert",
                    "linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%)"),
            new Photo("Ocean",
                    "linear-gradient(135deg, #2980b9 0%, #6dd5fa 100%)"),
            new Photo("Mountain",
                    "linear-gradient(135deg, #355c7d 0%, #c06c84 100%)"),
            new Photo("City night",
                    "linear-gradient(135deg, #0f2027 0%, #2c5364 100%)"));

    private final Div stage = new Div();
    private final Div stageImage = new Div();
    private final Span stateBadge = new Span();
    private final Span selectedName = new Span(PHOTOS.get(0).name());

    public ImageLightboxView() {
        addClassName("uc1-view");
        add(new H1("UC1 — Image lightbox"));
        add(new Paragraph(
                "Click any thumbnail to enlarge it to fullscreen. Press "
                        + "Escape (or the browser's close gesture) to return. "
                        + "The lightbox is a single Div fullscreened with "
                        + "Fullscreen.onClick(thumb).enter(stage); the rest of "
                        + "the view is hidden by the wrapper."));

        stateBadge.addClassName("status-badge");
        add(stateBadge);

        Div grid = new Div();
        grid.addClassName("lightbox-grid");
        for (Photo photo : PHOTOS) {
            grid.add(thumbnail(photo));
        }
        add(grid);

        Paragraph note = new Paragraph("Current selection: ");
        note.add(selectedName);
        add(note);

        stage.addClassName("lightbox-stage");
        stage.add(stageImage);
        stageImage.addClassName("lightbox-image");
        showPhoto(PHOTOS.get(0));
        add(stage);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();

        stateBadge.bindText(fs.map(ImageLightboxView::badgeText));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));
        stage.bindClassName("active",
                fs.map(s -> s == FullscreenState.FULLSCREEN));
    }

    private Div thumbnail(Photo photo) {
        Div thumb = new Div();
        thumb.addClassName("lightbox-thumb");
        thumb.setText(photo.name());
        // Per-photo gradient is data, not a discrete state — pass it through
        // as a CSS custom property so the rule lives in uc1.css.
        thumb.getStyle().set("--lightbox-thumb-bg", photo.gradient());
        thumb.addClickListener(e -> showPhoto(photo));
        // Fullscreen needs the click's user gesture, so bind the request to the
        // thumbnail's click trigger.
        Fullscreen.onClick(thumb).enter(stage);
        return thumb;
    }

    private void showPhoto(Photo photo) {
        selectedName.setText(photo.name());
        // Same pattern as the thumbnail: the gradient is dynamic per photo,
        // exposed to uc1.css via a custom property.
        stageImage.getStyle().set("--lightbox-image-bg", photo.gradient());
    }

    private static String badgeText(FullscreenState state) {
        // FULLSCREEN: the badge sits outside the fullscreened stage, so the
        // user can't see it while fullscreen — no point switching the text.
        return switch (state) {
        case FULLSCREEN, NOT_FULLSCREEN -> "Click a thumbnail to enlarge";
        case UNSUPPORTED -> "Fullscreen is not supported in this browser";
        case UNKNOWN -> "Detecting fullscreen support…";
        };
    }
}
