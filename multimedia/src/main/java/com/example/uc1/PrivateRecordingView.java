package com.example.uc1;

import java.util.List;

import com.example.MediaLibrary;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Media;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Video;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC1 — Private recording with a poster.
 * <p>
 * A team tool lists the meeting recordings that belong to the signed-in user.
 * The files must not be reachable through a public URL, so both the video and
 * its poster image are served by the application through
 * {@link DownloadHandler}s: the URLs are tied to this session and this
 * component, and stop working when either goes away. The poster shows before
 * playback starts, and {@link Media.Preload#METADATA} fetches only enough to
 * show the duration. The selected recording is part of the URL, such as
 * {@code uc1/sprint-42}, so it can be bookmarked and shared.
 */
@Route(value = "uc1/:recording?", layout = MainLayout.class)
@PageTitle("UC1 — Private recording")
@UseCaseDescription("Serving a video only its owner can watch")
@Menu(order = 1, title = "UC1 — Private recording")
public class PrivateRecordingView extends VerticalLayout
        implements BeforeEnterObserver {

    /**
     * A recording owned by the signed-in user.
     */
    record Recording(String id, String title, String description) {
    }

    static final List<Recording> RECORDINGS = List.of(
            new Recording("sprint-41", "Sprint 41 review",
                    "Faster search and the export button is back."),
            new Recording("sprint-42", "Sprint 42 review",
                    "Dark mode and the reworked mobile layout."));

    private final ValueSignal<Recording> selected = new ValueSignal<>(
            RECORDINGS.get(0));
    private final Div playerSlot = new Div();

    public PrivateRecordingView() {
        add(new H1("UC1 — Private recording"));
        add(new Paragraph("Your recordings are served by the application, "
                + "not from a public folder. Copy the video address into "
                + "another browser and it will not load: the URL only "
                + "works for this session while the player is on screen."));

        add(new H2("Your recordings"));
        HorizontalLayout list = new HorizontalLayout();
        for (Recording recording : RECORDINGS) {
            Button button = new Button(recording.title(),
                    e -> UI.getCurrent().navigate(PrivateRecordingView.class,
                            new RouteParameters("recording", recording.id())));
            button.bindThemeName(ButtonVariant.PRIMARY.getVariantName(),
                    selected.map(recording::equals));
            list.add(button);
        }
        add(list);

        playerSlot.addClassName("player-slot");
        add(playerSlot);
        Signal.effect(playerSlot, () -> {
            playerSlot.removeAll();
            playerSlot.add(createPlayer(selected.get()));
        });
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get("recording").orElse("");
        selected.set(RECORDINGS.stream().filter(r -> r.id().equals(id))
                .findFirst().orElse(RECORDINGS.get(0)));
    }

    private static Div createPlayer(Recording recording) {
        // A fresh player per recording: swapping the <source> of a player
        // that has already loaded does nothing without HTMLMediaElement.load().
        Video video = new Video();
        video.setControls(true);
        video.setPreload(Media.Preload.METADATA);
        video.setWidth("640px");
        video.setMaxWidth("100%");
        video.setAriaLabel(recording.title());
        video.addSource(
                DownloadHandler.fromInputStream(event -> new DownloadResponse(
                        MediaLibrary.open(recording.id() + ".mp4"),
                        recording.id() + ".mp4", "video/mp4",
                        MediaLibrary.bytes(recording.id() + ".mp4").length)),
                "video/mp4");
        video.setPoster(
                DownloadHandler.fromInputStream(event -> new DownloadResponse(
                        MediaLibrary.open(recording.id() + "-poster.jpg"),
                        recording.id() + "-poster.jpg", "image/jpeg",
                        MediaLibrary.bytes(
                                recording.id() + "-poster.jpg").length)));

        return new Div(video, new Paragraph(recording.description()));
    }
}
