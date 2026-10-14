package com.example.uc2;

import com.example.Chapters;
import com.example.Chapters.Chapter;
import com.example.MediaLibrary;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Code;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.UnorderedList;
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
 * {@code enableRangeRequests()}; the view lists which handler does what.
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

        add(new H2("Which handlers can seek"));
        add(new UnorderedList(item(new Code("DownloadHandler.forFile(file)"),
                " answers byte ranges out of the box by seeking in "
                        + "the file. This player uses it, and it is the "
                        + "way to serve large media."),
                item(new Code("DownloadHandler.fromInputStream(...)"
                        + ".enableRangeRequests()"),
                        " opts in for a stream. "
                                + "The DownloadResponse must give the content "
                                + "length. Each range skips to its start, which "
                                + "is cheap for a FileInputStream but reads and "
                                + "discards the bytes otherwise."),
                item(new Code("forClassResource(...).enableRangeRequests()"),
                        " and the same on forServletResource opt in for "
                                + "resources. Inside a jar every seek reads the "
                                + "entry from its start, which is fine for short "
                                + "clips but not for long recordings."),
                item(new Code("META-INF/resources"), " — static files served "
                        + "by URL, like the clips in UC4 to UC6, get byte ranges "
                        + "from the servlet container."),
                new ListItem("Without range support a handler answers 200 "
                        + "with the whole file: the scrubber snaps back to "
                        + "what has been downloaded, and Safari does not play "
                        + "the video at all.")));
    }

    private static ListItem item(Code api, String text) {
        return new ListItem(api, new Text(text));
    }
}
