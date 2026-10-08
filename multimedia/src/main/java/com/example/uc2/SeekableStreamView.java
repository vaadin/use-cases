package com.example.uc2;

import com.example.Chapters;
import com.example.Chapters.Chapter;
import com.example.MediaLibrary;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;

/**
 * UC2 — Seekable streaming of a long recording.
 * <p>
 * Watching a one-hour meeting means jumping to the part you care about. For
 * that the browser asks the server for byte ranges ({@code Range: bytes=...})
 * and expects a {@code 206 Partial Content} answer. The two players below serve
 * the same recording with Flow's built-in handlers: the left one with
 * {@code DownloadHandler.fromInputStream}, which cannot skip ahead in a stream
 * and always sends the whole content, and the right one with
 * {@code DownloadHandler.forFile}, which answers byte-range requests. Only the
 * right one can be scrubbed.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Seekable streaming")
@Menu(order = 2, title = "UC2 — Seekable streaming")
public class SeekableStreamView extends VerticalLayout {

    static final String RECORDING = "quarterly-review.mp4";

    public SeekableStreamView() {
        add(new H1("UC2 — Seekable streaming"));
        Chapter questions = Chapters.QUARTERLY_REVIEW.getLast();
        add(new Paragraph("Start either player, then drag the scrubber to "
                + Chapters.format(questions.start()) + " (the \""
                + questions.title() + "\" chapter). The stream handler "
                + "sends the recording in one piece without advertising "
                + "range support, so the browser cannot jump ahead of what it "
                + "has downloaded. The file handler answers each seek with "
                + "just the bytes needed."));

        Div players = new Div();
        players.addClassName("side-by-side");

        Video stream = createPlayer("Stream handler");
        stream.addSource(DownloadHandler.fromInputStream(
                event -> new DownloadResponse(MediaLibrary.open(RECORDING),
                        RECORDING, "video/mp4",
                        MediaLibrary.bytes(RECORDING).length)),
                "video/mp4");
        players.add(new Div(new H2("fromInputStream"), stream,
                new Paragraph("Always 200 OK with the whole recording.")));

        Video file = createPlayer("File handler");
        file.addSource(
                DownloadHandler.forFile(MediaLibrary.file(RECORDING)).inline(),
                "video/mp4");
        players.add(new Div(new H2("forFile"), file, new Paragraph(
                "206 Partial Content for each requested range.")));

        add(players);
    }

    private static Video createPlayer(String label) {
        Video video = new Video();
        video.setControls(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidthFull();
        video.setAriaLabel(label);
        return video;
    }
}
