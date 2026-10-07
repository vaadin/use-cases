package com.example.uc7;

import java.util.Optional;

import com.example.Chapters;
import com.example.Chapters.Chapter;
import com.example.MissingAPI;
import com.example.RangeDownloadHandler;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC7 — Server-driven controls and chapters.
 * <p>
 * A training portal shows a recording with the application's own controls
 * instead of the browser's: play/pause, skip ten seconds back or forward, and a
 * chapter list that jumps to a timestamp. The chapter that is playing is
 * highlighted as the video runs, and the position is shown next to the
 * controls.
 * <p>
 * All of that is state the server has to drive or observe — {@code play()},
 * {@code pause()}, {@code currentTime}, and the {@code play} / {@code pause} /
 * {@code timeupdate} events — none of which the component exposes yet. The view
 * uses {@link MissingAPI} for them and keeps the observed state in signals.
 * <p>
 * Jumping to a chapter puts its start time in the URL, such as
 * {@code uc7?t=23}, so a link can point at a chapter; opening it starts the
 * player there.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Chapters & controls")
@Menu(order = 7, title = "UC7 — Chapters & controls")
@StyleSheet("uc7.css")
public class ChapterControlsView extends VerticalLayout
        implements BeforeEnterObserver {

    private final ValueSignal<Boolean> playing = new ValueSignal<>(false);
    private final ValueSignal<Double> position = new ValueSignal<>(0.0);
    private final ValueSignal<Double> duration = new ValueSignal<>(0.0);
    private final Video video = new Video();

    public ChapterControlsView() {
        addClassName("uc7-view");
        add(new H1("UC7 — Chapters & controls"));
        add(new Paragraph("The buttons below are Vaadin components, not the "
                + "browser's controls. Every click is handled on the server, "
                + "and the highlighted chapter follows the playback "
                + "position reported by the browser."));

        video.setPreload(Media.Preload.METADATA);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Quarterly product review");
        video.addSource(RangeDownloadHandler.forMedia("quarterly-review.mp4",
                "video/mp4"), "video/mp4");
        MissingAPI.addPlayingListener(video, playing::set);
        MissingAPI.addTimeUpdateListener(video, 500, position::set);
        MissingAPI.addDurationListener(video, duration::set);
        add(video);

        Button playPause = new Button();
        playPause.addThemeVariants(ButtonVariant.PRIMARY);
        playPause.bindText(playing.map(p -> p ? "Pause" : "Play"));
        playPause.addClickListener(e -> {
            if (playing.peek()) {
                MissingAPI.pause(video);
            } else {
                MissingAPI.play(video);
            }
        });
        Button back = new Button("−10 s", e -> MissingAPI.seekBy(video, -10));
        Button forward = new Button("+10 s", e -> MissingAPI.seekBy(video, 10));
        Span time = new Span();
        time.addClassName("time");
        time.bindText(() -> Chapters.format(position.get()) + " / "
                + Chapters.format(duration.get()));
        HorizontalLayout controls = new HorizontalLayout(back, playPause,
                forward, time);
        controls.setAlignItems(Alignment.CENTER);
        add(controls);

        add(new H2("Chapters"));
        Div chapters = new Div();
        chapters.addClassName("chapters");
        for (int i = 0; i < Chapters.QUARTERLY_REVIEW.size(); i++) {
            int index = i;
            Chapter chapter = Chapters.QUARTERLY_REVIEW.get(i);
            Button button = new Button(
                    Chapters.format(chapter.start()) + " · " + chapter.title(),
                    e -> jumpTo(chapter));
            button.addClassName("chapter");
            button.bindClassName("current", position
                    .map(seconds -> Chapters.indexAt(seconds) == index));
            chapters.add(button);
        }
        add(chapters);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.getLocation().getQueryParameters().getSingleParameter("t")
                .flatMap(ChapterControlsView::parseSeconds)
                .filter(seconds -> seconds != position.peek())
                .ifPresent(seconds -> {
                    position.set(seconds);
                    MissingAPI.startAt(video, seconds);
                });
    }

    private void jumpTo(Chapter chapter) {
        // Update right away rather than waiting for the next timeupdate.
        position.set((double) chapter.start());
        MissingAPI.seek(video, chapter.start());
        MissingAPI.play(video);
        UI.getCurrent().navigate(ChapterControlsView.class,
                QueryParameters.of("t", String.valueOf(chapter.start())));
    }

    private static Optional<Double> parseSeconds(String value) {
        try {
            double seconds = Double.parseDouble(value);
            return seconds >= 0 && Double.isFinite(seconds)
                    ? Optional.of(seconds)
                    : Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
