package com.example.uc8;

import com.example.Chapters;
import com.example.MissingAPI;
import com.example.RangeDownloadHandler;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
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
 * UC8 — Resume where you left off.
 * <p>
 * A learning platform remembers how far the user got in a recording. When they
 * come back — after navigating away or reloading — the player continues from
 * there, and a "Start over" button rewinds. The position is saved every few
 * seconds while playing and whenever playback pauses; finishing the recording
 * forgets it.
 * <p>
 * The server needs to read the playback position and set the start position,
 * neither of which the component exposes; {@link MissingAPI} fills in.
 */
@Route(value = "uc8", layout = MainLayout.class)
@PageTitle("UC8 — Resume playback")
@Menu(order = 8, title = "UC8 — Resume playback")
public class ResumePlaybackView extends VerticalLayout {

    static final String RECORDING = "quarterly-review.mp4";

    private final PlaybackPositions positions = PlaybackPositions.current();
    private final ValueSignal<Double> resumeFrom;
    private final Video video = new Video();
    private boolean finished;

    public ResumePlaybackView() {
        add(new H1("UC8 — Resume playback"));
        add(new Paragraph("Play part of the recording, then open another use "
                + "case or reload the page. When you return, the player "
                + "continues where you stopped."));

        resumeFrom = new ValueSignal<>(positions.get(RECORDING).orElse(0.0));

        Span status = new Span();
        status.addClassName("status-badge");
        status.bindText(resumeFrom.map(seconds -> seconds > 0
                ? "Resuming from " + Chapters.format(seconds)
                : "Starting from the beginning"));
        Button startOver = new Button("Start over", e -> startOver());
        startOver.bindEnabled(resumeFrom.map(seconds -> seconds > 0));
        HorizontalLayout bar = new HorizontalLayout(status, startOver);
        bar.setAlignItems(Alignment.CENTER);
        add(bar);

        video.setControls(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Quarterly product review");
        video.addSource(RangeDownloadHandler.forMedia(RECORDING, "video/mp4"),
                "video/mp4");
        add(video);

        if (resumeFrom.peek() > 0) {
            MissingAPI.startAt(video, resumeFrom.peek());
        }
        MissingAPI.addTimeUpdateListener(video, 2000, this::savePosition);
        MissingAPI.addPauseListener(video, this::savePosition);
        MissingAPI.addPlayingListener(video, isPlaying -> {
            if (isPlaying) {
                finished = false;
            }
        });
        MissingAPI.addEndedListener(video, this::finish);
    }

    /**
     * Stores the position reported by the player.
     *
     * @param seconds
     *            the playback position
     */
    void savePosition(double seconds) {
        // A throttled timeupdate can arrive after "ended"; keep it forgotten.
        if (!finished) {
            positions.save(RECORDING, seconds);
        }
    }

    /**
     * Forgets the position once the recording has played to the end.
     */
    void finish() {
        finished = true;
        positions.clear(RECORDING);
    }

    private void startOver() {
        positions.clear(RECORDING);
        resumeFrom.set(0.0);
        MissingAPI.seek(video, 0);
    }
}
