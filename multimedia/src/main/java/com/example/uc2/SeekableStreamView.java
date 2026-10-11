package com.example.uc2;

import com.example.Chapters;
import com.example.Chapters.Chapter;
import com.example.MediaLibrary;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC2 — Seekable streaming of a long recording.
 * <p>
 * Watching a one-hour meeting means jumping to the part you care about. For
 * that the browser asks the server for byte ranges ({@code Range: bytes=...})
 * and expects a {@code 206 Partial Content} answer; Safari does not play video
 * at all without it. {@code DownloadHandler.forFile} answers those requests by
 * seeking in the file, so the player can jump anywhere without downloading what
 * comes before. Other handlers send the whole content unless they opt in with
 * {@code enableRangeRequests()}.
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
        add(new Paragraph("Start the player, then drag the scrubber to "
                + Chapters.format(questions.start()) + " (the \""
                + questions.title() + "\" chapter). The recording is served "
                + "from a file that answers each seek with just the bytes "
                + "needed, so playback continues right there instead of "
                + "waiting for everything before it to download."));

        Video video = new Video();
        video.setControlsVisible(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel("Quarterly product review");
        video.addSource(MediaLibrary.serve(RECORDING), "video/mp4");
        add(video);
    }
}
