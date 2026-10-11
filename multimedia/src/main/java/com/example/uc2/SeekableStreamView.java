package com.example.uc2;

import com.example.Chapters;
import com.example.Chapters.Chapter;
import com.example.MediaLibrary;
import com.example.common.UseCaseDescription;
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
import com.vaadin.flow.server.streams.InputStreamDownloadHandler;

/**
 * UC2 — Seekable streaming of a long recording.
 * <p>
 * Watching a one-hour meeting means jumping to the part you care about. For
 * that the browser asks the server for byte ranges ({@code Range: bytes=...})
 * and expects a {@code 206 Partial Content} answer. The two players below serve
 * the same recording with {@code DownloadHandler.fromInputStream}. By default
 * that handler always sends the whole content, because it cannot know whether a
 * stream can skip ahead. The right one opts in with
 * {@code enableRangeRequests()} and answers byte-range requests. Only the right
 * one can be scrubbed. ({@code DownloadHandler.forFile} answers ranges without
 * opting in.)
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Seekable streaming")
@UseCaseDescription("Making a long recording seekable by serving byte ranges")
@Menu(order = 2, title = "UC2 — Seekable streaming")
public class SeekableStreamView extends VerticalLayout {

    static final String RECORDING = "quarterly-review.mp4";

    public SeekableStreamView() {
        add(new H1("UC2 — Seekable streaming"));
        Chapter questions = Chapters.QUARTERLY_REVIEW.getLast();
        add(new Paragraph("Start either player, then drag the scrubber to "
                + Chapters.format(questions.start()) + " (the \""
                + questions.title() + "\" chapter). The default handler "
                + "sends the recording in one piece without advertising "
                + "range support, so the browser cannot jump ahead of what it "
                + "has downloaded. With range requests enabled, the handler "
                + "answers each seek with just the bytes needed."));

        Div players = new Div();
        players.addClassName("side-by-side");

        Video whole = createPlayer("Default stream handler");
        whole.addSource(wholeHandler(), "video/mp4");
        players.add(new Div(new H2("fromInputStream"), whole,
                new Paragraph("Always 200 OK with the whole recording.")));

        Video ranged = createPlayer("Stream handler with range requests");
        ranged.addSource(rangedHandler(), "video/mp4");
        players.add(
                new Div(new H2("enableRangeRequests()"), ranged, new Paragraph(
                        "206 Partial Content for each requested range.")));

        add(players);
    }

    static InputStreamDownloadHandler wholeHandler() {
        return streamHandler();
    }

    static InputStreamDownloadHandler rangedHandler() {
        // The content length is required for range requests.
        return streamHandler().enableRangeRequests();
    }

    private static InputStreamDownloadHandler streamHandler() {
        return DownloadHandler.fromInputStream(event -> new DownloadResponse(
                MediaLibrary.open(RECORDING), RECORDING, "video/mp4",
                MediaLibrary.bytes(RECORDING).length)).inline();
    }

    private static Video createPlayer(String label) {
        Video video = new Video();
        video.setControlsVisible(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidthFull();
        video.setAriaLabel(label);
        return video;
    }
}
