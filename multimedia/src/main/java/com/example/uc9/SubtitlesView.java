package com.example.uc9;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import com.example.MediaLibrary;
import com.example.MissingAPI;
import com.example.RangeDownloadHandler;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC9 — Subtitles in several languages.
 * <p>
 * An international company publishes its product review with subtitles in
 * English, German and Finnish. The WebVTT files are served by the application
 * next to the video. The subtitles start in the language the browser asks for
 * when there is a track for it, and a language picker on the page switches them
 * — the same choice the browser's own subtitle menu offers.
 * <p>
 * {@code Media} only accepts {@code Source} children, so there is no way to add
 * a {@code <track>} through the component API, and nothing to select the
 * showing track from the server. {@link MissingAPI#addTextTrack} and
 * {@link MissingAPI#showTextTrack} fill in.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Subtitles")
@Menu(order = 9, title = "UC9 — Subtitles")
public class SubtitlesView extends VerticalLayout {

    /**
     * A subtitle track.
     *
     * @param language
     *            the BCP 47 language code, also the file name
     * @param label
     *            the language name shown to the user
     */
    record Subtitles(String language, String label) {
    }

    static final List<Subtitles> SUBTITLES = List.of(
            new Subtitles("en", "English"), new Subtitles("de", "Deutsch"),
            new Subtitles("fi", "Suomi"));

    static final String OFF = "Off";

    public SubtitlesView() {
        add(new H1("UC9 — Subtitles"));
        add(new Paragraph("Pick a subtitle language below, or use the "
                + "subtitles menu in the player. The subtitles start in "
                + "your browser's language when one of the tracks matches."));

        Video video = new Video();
        video.setControls(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Quarterly product review");
        video.addSource(RangeDownloadHandler.forMedia("quarterly-review.mp4",
                "video/mp4"), "video/mp4");

        VaadinRequest request = VaadinRequest.getCurrent();
        String initial = initialLanguage(
                request == null ? null : request.getLocale());
        for (Subtitles subtitles : SUBTITLES) {
            String file = "subtitles/" + subtitles.language() + ".vtt";
            Element track = MissingAPI.addTextTrack(video, "subtitles",
                    subtitles.language(), subtitles.label(),
                    DownloadHandler
                            .fromInputStream(event -> new DownloadResponse(
                                    MediaLibrary.open(file),
                                    subtitles.language() + ".vtt", "text/vtt",
                                    MediaLibrary.bytes(file).length)));
            track.setAttribute("default", subtitles.language().equals(initial));
        }
        add(video);

        RadioButtonGroup<String> picker = new RadioButtonGroup<>("Subtitles");
        picker.setItems(Stream.concat(Stream.of(OFF),
                SUBTITLES.stream().map(Subtitles::label)).toList());
        picker.setValue(labelOf(initial));
        picker.addValueChangeListener(
                e -> MissingAPI.showTextTrack(video, languageOf(e.getValue())));
        add(picker);
    }

    /**
     * Picks the track matching the user's locale, English otherwise.
     *
     * @param locale
     *            the user's locale
     * @return the language code of the track to show first
     */
    static String initialLanguage(@Nullable Locale locale) {
        String language = locale == null ? "" : locale.getLanguage();
        return SUBTITLES.stream().map(Subtitles::language)
                .filter(language::equals).findFirst().orElse("en");
    }

    private static String labelOf(String language) {
        return SUBTITLES.stream().filter(s -> s.language().equals(language))
                .map(Subtitles::label).findFirst().orElse(OFF);
    }

    private static @Nullable String languageOf(@Nullable String label) {
        return SUBTITLES.stream().filter(s -> Objects.equals(s.label(), label))
                .map(Subtitles::language).findFirst().orElse(null);
    }
}
