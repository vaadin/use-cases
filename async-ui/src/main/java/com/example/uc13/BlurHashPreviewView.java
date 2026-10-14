package com.example.uc13;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.example.BlurHash;
import com.example.backend.PhotoLibrary;
import com.example.backend.PhotoLibrary.Photo;
import com.example.backend.SimulatedLatency;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC13 — Slow images with a BlurHash preview.
 * <p>
 * Each photo is stored with its BlurHash, a string of under 30 characters
 * computed when the photo was uploaded. The page decodes it into a tiny blurry
 * image and sends that inline, so the colours and the rough composition are on
 * screen in the same response as the rest of the page. The real photo loads
 * afterwards over a deliberately slow connection and fades in over the preview
 * once the browser reports it complete. Each tile reserves the photo's aspect
 * ratio up front, so nothing below it moves.
 * <p>
 * Flow's {@code Image} has no load event and no placeholder of its own, so the
 * fade is wired up by hand (see API-GAPS.md).
 */
@Route(value = "uc13", layout = MainLayout.class)
@PageTitle("UC13 — Slow images with a preview")
@UseCaseDescription("Showing a blurry preview with the right colours while a slow image loads")
@Menu(order = 13, title = "UC13 — Slow images with a preview")
@StyleSheet("uc13.css")
public class BlurHashPreviewView extends VerticalLayout {

    /** What stands in for a photo until it has loaded. */
    enum Placeholder {
        NOTHING("Nothing"),
        AVERAGE_COLOR("Average colour"),
        BLURHASH("BlurHash preview");

        private final String label;

        Placeholder(String label) {
            this.label = label;
        }
    }

    /** The width of the decoded preview; the browser scales it up. */
    static final int PREVIEW_WIDTH = 32;
    // Different per photo, so they do not all finish at once.
    private static final List<Duration> CHUNK_DELAYS = List.of(
            Duration.ofMillis(150), Duration.ofMillis(250),
            Duration.ofMillis(200), Duration.ofMillis(300));

    private final SimulatedLatency latency;
    private final RadioButtonGroup<Placeholder> placeholder = new RadioButtonGroup<>(
            "While a photo loads, show");
    private final List<Tile> tiles = new ArrayList<>();

    public BlurHashPreviewView(PhotoLibrary library, SimulatedLatency latency) {
        this.latency = latency;
        addClassName("uc13-view");

        add(new H1("UC13 — Slow images with a preview"));
        add(new Paragraph("The photos below come over a slow connection and "
                + "take a few seconds each. With the BlurHash preview, the "
                + "colours and the rough shapes are there at once, and each "
                + "photo fades in when it is complete. Switch to the other "
                + "placeholders to compare: every switch loads the photos "
                + "again."));

        placeholder.setItems(Placeholder.values());
        placeholder.setItemLabelGenerator(p -> p.label);
        placeholder.setValue(Placeholder.BLURHASH);
        placeholder.addValueChangeListener(event -> reloadAll());
        Button again = new Button("Load again", event -> reloadAll());
        HorizontalLayout controls = new HorizontalLayout(placeholder, again);
        controls.setAlignItems(FlexComponent.Alignment.BASELINE);

        Div gallery = new Div();
        gallery.addClassName("gallery");
        List<Photo> photos = library.photos();
        for (int i = 0; i < photos.size(); i++) {
            Tile tile = new Tile(photos.get(i),
                    CHUNK_DELAYS.get(i % CHUNK_DELAYS.size()));
            tiles.add(tile);
            gallery.add(tile);
        }

        add(controls, gallery);
        reloadAll();
    }

    private void reloadAll() {
        tiles.forEach(Tile::reload);
    }

    /** A {@code data:} URL, so a small image arrives with the page itself. */
    static String pngDataUrl(BufferedImage image) {
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", png);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(png.toByteArray());
    }

    // Package-private test seam.
    List<Tile> tiles() {
        return tiles;
    }

    final class Tile extends Div {

        private final Photo photo;
        private final ThrottledDownloadHandler handler;
        private final Div frame = new Div();
        private final Image preview;
        private final Image full = new Image();
        private final ValueSignal<Boolean> loaded = new ValueSignal<>(false);

        Tile(Photo photo, Duration chunkDelay) {
            this.photo = photo;
            handler = new ThrottledDownloadHandler(photo.bytes(), "image/jpeg",
                    chunkDelay, latency);
            addClassName("photo");

            frame.addClassName("photo-frame");
            frame.getStyle().set("aspect-ratio",
                    photo.width() + " / " + photo.height());

            int previewHeight = Math.max(1,
                    PREVIEW_WIDTH * photo.height() / photo.width());
            preview = new Image(pngDataUrl(BlurHash.decode(photo.blurHash(),
                    PREVIEW_WIDTH, previewHeight)), "");
            preview.addClassName("photo-preview");

            full.setAlt(photo.title());
            full.addClassName("photo-full");
            full.getElement().addEventListener("load",
                    event -> loaded.set(true));
            bindClassName("loaded", loaded);
            frame.add(preview, full);

            Span title = new Span(photo.title());
            title.addClassName("photo-title");
            Span status = new Span();
            status.addClassName("photo-status");
            status.bindText(loaded.map(done -> done ? "Loaded"
                    : "Loading (" + seconds(handler.duration()) + ")…"));
            Span hash = new Span("BlurHash " + photo.blurHash());
            hash.addClassName("photo-hash");
            Div caption = new Div(title, status);
            caption.addClassName("photo-caption");
            add(frame, caption, hash);
        }

        void reload() {
            Placeholder mode = placeholder.getValue();
            preview.setVisible(mode == Placeholder.BLURHASH);
            frame.getStyle().set("background-color",
                    mode == Placeholder.NOTHING ? null
                            : BlurHash.averageColor(photo.blurHash()));
            // Without a placeholder the photo paints top to bottom as its
            // bytes arrive; with one it stays hidden and fades in once the
            // browser reports it complete.
            setClassName("fade-in", mode != Placeholder.NOTHING);
            loaded.set(false);
            // Setting the handler again registers it under a new URL, so the
            // browser cannot answer from its cache.
            full.setSrc(handler);
        }

        Photo photo() {
            return photo;
        }

        Image preview() {
            return preview;
        }

        Image full() {
            return full;
        }

        Div frame() {
            return frame;
        }

        boolean isLoaded() {
            return loaded.peek();
        }
    }

    private static String seconds(Duration duration) {
        return BigDecimal.valueOf(duration.toMillis(), 3).stripTrailingZeros()
                .toPlainString() + " s";
    }
}
