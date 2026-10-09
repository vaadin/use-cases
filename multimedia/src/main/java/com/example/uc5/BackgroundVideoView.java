package com.example.uc5;

import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
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
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC5 — Muted, looping background video.
 * <p>
 * A landing page plays a short silent clip behind its headline. Browsers only
 * allow autoplay without a user gesture when the video is muted, and iOS Safari
 * additionally needs {@code playsinline}. The clip is decorative, so it is
 * hidden from assistive technology and has no controls — but moving content
 * that lasts more than five seconds needs a way to pause it (WCAG 2.2.2), so
 * the view adds its own pause button.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Background video")
@UseCaseDescription("Autoplaying a muted looping background video with a pause button")
@Menu(order = 5, title = "UC5 — Background video")
@StyleSheet("uc5.css")
public class BackgroundVideoView extends VerticalLayout {

    static final String CLIP = "media/background.mp4";

    private final ValueSignal<Boolean> paused = new ValueSignal<>(false);

    public BackgroundVideoView() {
        addClassName("uc5-view");
        add(new H1("UC5 — Background video"));
        add(new Paragraph("The hero below autoplays a muted clip in a loop. "
                + "It starts without a click because it is muted."));

        Video video = new Video();
        video.addClassName("hero-video");
        video.setAutoplay(true);
        // setMuted only writes the attribute, which does not mute an element
        // that already exists in the page, so autoplay would be blocked.
        MissingAPI.setMutedNow(video, true);
        video.setLoop(true);
        video.setPreload(Media.Preload.AUTO);
        MissingAPI.setPlaysInline(video, true);
        video.getElement().setAttribute("aria-hidden", "true");
        video.addSource(CLIP, "video/mp4");

        Button pause = new Button();
        pause.addClassName("hero-pause");
        pause.addThemeVariants(ButtonVariant.PRIMARY);
        pause.bindText(
                paused.map(p -> p ? "Play background" : "Pause background"));
        pause.addClickListener(e -> {
            boolean pausing = !paused.peek();
            if (pausing) {
                MissingAPI.pause(video);
            } else {
                MissingAPI.play(video);
            }
            paused.set(pausing);
        });

        Div overlay = new Div(new H2("Ship features, not boilerplate"),
                new Paragraph("Build the whole app in Java."), pause);
        overlay.addClassName("hero-overlay");

        Div hero = new Div(video, overlay);
        hero.addClassName("hero");
        add(hero);
    }
}
