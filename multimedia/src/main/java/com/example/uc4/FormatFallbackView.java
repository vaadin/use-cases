package com.example.uc4;

import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
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
 * A marketing site offers its trailer encoded with AV1, which is about 30%
 * smaller at the same quality, and with H.264, which plays everywhere. AV1 is
 * not universal: Safari only plays it on devices with a hardware AV1 decoder
 * (Apple M3 / A17 Pro and newer). The player lists both as {@code <source>}
 * children in order of preference, and the browser takes the first one it can
 * play. The files are public assets served as static resources, so plain URLs
 * are enough. The codec is burnt into the picture, and the badge reports the
 * file the browser chose.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Format fallback")
@UseCaseDescription("Offering a video in a modern codec with a fallback that plays everywhere")
@Menu(order = 4, title = "UC4 — Format fallback")
public class FormatFallbackView extends VerticalLayout {

    static final String AV1 = "media/trailer-av1.mp4";
    static final String H264 = "media/trailer.mp4";

    private final ValueSignal<String> chosen = new ValueSignal<>(
            "not loaded yet");

    public FormatFallbackView() {
        add(new H1("UC4 — Format fallback"));
        add(new Paragraph("AV1 is a newer, royalty-free codec that "
                + "compresses better than H.264: the AV1 trailer is about "
                + "550 KB, the H.264 one about 790 KB, at the same 720p "
                + "resolution. A smaller file starts sooner, uses less of "
                + "a mobile data plan, and costs less to serve."));
        add(new Paragraph("The AV1 source is listed first, the H.264 second. "
                + "Browsers that cannot decode AV1 skip it and play the "
                + "H.264 file instead, without the application having to "
                + "know which browser it is talking to."));

        Span badge = new Span();
        badge.addClassName("status-badge");
        badge.bindText(chosen.map(format -> "Browser chose: " + format));
        add(new HorizontalLayout(badge));

        Video video = new Video();
        video.setControlsVisible(true);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Product trailer");
        video.addSource(AV1, "video/mp4; codecs=\"av01.0.05M.08, mp4a.40.2\"");
        video.addSource(H264, "video/mp4; codecs=\"avc1.64001f, mp4a.40.2\"");
        add(video);

        add(new H2("Which browsers play AV1"));
        add(new UnorderedList(
                new ListItem("Chrome and Edge on desktop, Chrome on Android, "
                        + "and Firefox including ESR: play AV1."),
                new ListItem("Safari on macOS, iOS and iPadOS: plays AV1 "
                        + "only with a hardware AV1 decoder — Macs with an "
                        + "M3 chip or newer, iPhone 15 Pro and newer, and "
                        + "iPads with an M4 or A17 Pro chip. Older devices "
                        + "fall back to H.264. Every browser on iPhone and "
                        + "iPad uses Safari's engine, so the same applies "
                        + "to them.")));
        add(new Anchor("https://vaadin.com/docs/latest/compatibility",
                "Browsers supported by Vaadin"));

        MissingAPI.addSourceChosenListener(video,
                url -> chosen.set(url.endsWith(AV1) ? "AV1"
                        : url.endsWith(H264) ? "H.264" : url));
    }
}
