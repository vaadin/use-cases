package com.example.uc4;

import com.example.MissingAPI;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Format fallback.
 * <p>
 * A marketing site offers its trailer as WebM (VP9 + Opus), which is smaller,
 * and as MP4 (H.264 + AAC), which plays everywhere. The player lists both as
 * {@code <source>} children in order of preference, and the browser takes the
 * first one it can play. The files are public assets served as static
 * resources, so plain URLs are enough. The format is burnt into the picture,
 * and the badge reports the file the browser chose.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Format fallback")
@Menu(order = 4, title = "UC4 — Format fallback")
public class FormatFallbackView extends VerticalLayout {

    static final String WEBM = "media/trailer.webm";
    static final String MP4 = "media/trailer.mp4";

    private final ValueSignal<String> chosen = new ValueSignal<>(
            "not loaded yet");

    public FormatFallbackView() {
        add(new H1("UC4 — Format fallback"));
        add(new Paragraph("The WebM source is listed first, the MP4 second. "
                + "Browsers without VP9 support skip the WebM file and play "
                + "the MP4 instead, without the application having to know "
                + "which browser it is talking to."));

        Span badge = new Span();
        badge.addClassName("status-badge");
        badge.bindText(chosen.map(format -> "Browser chose: " + format));
        add(new HorizontalLayout(badge));

        Video video = new Video();
        video.setControls(true);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Product trailer");
        video.addSource(WEBM, "video/webm; codecs=\"vp9, opus\"");
        video.addSource(MP4, "video/mp4; codecs=\"avc1.64001e, mp4a.40.2\"");
        add(video);

        MissingAPI.addSourceChosenListener(video,
                url -> chosen.set(url.endsWith(".webm") ? "WebM (VP9)"
                        : url.endsWith(".mp4") ? "MP4 (H.264)" : url));
    }
}
