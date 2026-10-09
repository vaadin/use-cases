package com.example.uc3;

import com.example.HlsDownloadHandler;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Media;
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
 * UC3 — Adaptive streaming with HLS.
 * <p>
 * A video platform offers the same recording in several qualities and lets the
 * player switch between them as the connection changes. The stream is HLS: a
 * master playlist lists three renditions (240p, 360p, 720p), each cut into
 * six-second segments. The rendition is burnt into the corner of the picture,
 * so a switch is visible.
 * <p>
 * The whole stream is served by one {@link HlsDownloadHandler}, which works
 * around the handler's single URL by rewriting the playlists. Browsers that do
 * not play HLS natively get {@code hls.js} as a fallback; the badge shows which
 * path this browser took.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Adaptive streaming")
@UseCaseDescription("Switching video quality to match the connection")
@Menu(order = 3, title = "UC3 — Adaptive streaming")
@NpmPackage(value = "hls.js", version = "1.7.3")
@JsModule("./hls-fallback.ts")
public class AdaptiveStreamingView extends VerticalLayout {

    static final String HLS_TYPE = "application/vnd.apple.mpegurl";

    private final ValueSignal<String> playback = new ValueSignal<>("checking…");

    public AdaptiveStreamingView() {
        add(new H1("UC3 — Adaptive streaming"));
        add(new Paragraph("The player picks a rendition from the master "
                + "playlist and switches as bandwidth changes. Throttle the "
                + "network in the browser's developer tools and watch the "
                + "label in the top-left corner of the picture."));

        Span badge = new Span();
        badge.addClassName("status-badge");
        badge.bindText(playback.map(how -> "Playback: " + how));
        add(new HorizontalLayout(badge));

        Video video = new Video();
        video.setControlsVisible(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Quarterly product review, adaptive stream");
        video.addSource(new HlsDownloadHandler("hls"), HLS_TYPE);
        add(video);

        video.getElement().executeJs("return window.multimediaAttachHls(this)")
                .then(String.class, how -> playback.set(switch (how) {
                case "native" -> "native HLS";
                case "hls.js" -> "hls.js (Media Source Extensions)";
                default -> "not supported in this browser";
                }));
    }
}
