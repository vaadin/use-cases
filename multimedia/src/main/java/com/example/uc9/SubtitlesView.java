package com.example.uc9;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import com.example.MediaLibrary;
import com.example.MissingAPI;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
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
 * — the same choice the browser's own subtitle menu offers. The choice is kept
 * in the URL, such as {@code uc9?subtitles=de} or {@code uc9?subtitles=off}, so
 * a shared link opens with the same subtitles.
 * <p>
 * {@code Media} only accepts {@code Source} children, so there is no way to add
 * a {@code <track>} through the component API, and nothing to select the
 * showing track from the server. {@link MissingAPI#addTextTrack} and
 * {@link MissingAPI#showTextTrack} fill in.
 */
@Route(value = "uc9", layout = MainLayout.class)
@PageTitle("UC9 — Subtitles")
@Menu(order = 9, title = "UC9 — Subtitles")
public class SubtitlesView extends VerticalLayout
        implements BeforeEnterObserver {

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

    private static final String OFF_PARAMETER = "off";

    private final Video video = new Video();
    private final Map<String, Element> tracks = new LinkedHashMap<>();
    private final RadioButtonGroup<String> picker = new RadioButtonGroup<>(
            "Subtitles");
    private final String browserLanguage;
    private @Nullable String urlLanguage;

    public SubtitlesView() {
        add(new H1("UC9 — Subtitles"));
        add(new Paragraph("Pick a subtitle language below, or use the "
                + "subtitles menu in the player. The subtitles start in "
                + "your browser's language when one of the tracks matches."));

        video.setControls(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Quarterly product review");
        video.addSource(DownloadHandler
                .forFile(MediaLibrary.file("quarterly-review.mp4")).inline(),
                "video/mp4");

        VaadinRequest request = VaadinRequest.getCurrent();
        browserLanguage = initialLanguage(
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
            tracks.put(subtitles.language(), track);
        }
        add(video);

        picker.setItems(Stream.concat(Stream.of(OFF),
                SUBTITLES.stream().map(Subtitles::label)).toList());
        picker.addValueChangeListener(e -> {
            String language = languageOf(e.getValue());
            MissingAPI.showTextTrack(video, language);
            if (!Objects.equals(language, urlLanguage)) {
                UI.getCurrent().navigate(SubtitlesView.class,
                        QueryParameters.of("subtitles",
                                language == null ? OFF_PARAMETER : language));
            }
        });
        add(picker);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String parameter = event.getLocation().getQueryParameters()
                .getSingleParameter("subtitles").orElse("");
        if (parameter.equals(OFF_PARAMETER)) {
            urlLanguage = null;
        } else {
            urlLanguage = tracks.containsKey(parameter) ? parameter
                    : browserLanguage;
        }
        tracks.forEach((language, track) -> track.setAttribute("default",
                language.equals(urlLanguage)));
        picker.setValue(labelOf(urlLanguage));
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

    private static String labelOf(@Nullable String language) {
        return SUBTITLES.stream().filter(s -> s.language().equals(language))
                .map(Subtitles::label).findFirst().orElse(OFF);
    }

    private static @Nullable String languageOf(@Nullable String label) {
        return SUBTITLES.stream().filter(s -> Objects.equals(s.label(), label))
                .map(Subtitles::language).findFirst().orElse(null);
    }
}
