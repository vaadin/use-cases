package com.example.uc2;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC2 — Slideshow / presentation mode.
 * <p>
 * A small carousel of slides projected with
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(present).enter(stage)} on the slide stage itself.
 * Component-level fullscreen is the right tool for a slideshow: only the slide
 * is shown, no app chrome, no view controls — that matches how native
 * presentation tools behave. Left/Right arrows still navigate via
 * {@code addClickShortcut} (those listen at the UI level so keydown events keep
 * reaching the server). Escape exits, so there is no need for an in-view exit
 * button.
 * <p>
 * Page-level fullscreen — where the whole document is fullscreened, app chrome
 * included — is demonstrated separately in
 * {@link com.example.uc7.AppFullscreenView UC7}.
 */
@Route(value = "uc2", layout = MainLayout.class)
@Menu(order = 2, title = "UC2 — Slideshow")
@StyleSheet("uc2.css")
public class SlideshowView extends VerticalLayout {

    private static final List<String> SLIDES = List.of(
            "Welcome to Fullscreen 101 — use ← / → to navigate, Escape to exit",
            "The slide enters fullscreen via Fullscreen.onClick(present).enter(stage)",
            "Keyboard shortcuts still reach the server",
            "Fullscreen.stateSignal() reactively reports the current state",
            "Press Escape to return to the editor");

    private final Span slideContent = new Span(SLIDES.get(0));
    private final Span counter = new Span();
    private final Span stateBadge = new Span();
    private final Div stage = new Div();

    private int index;

    public SlideshowView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Slideshow / presentation mode"));
        add(new Paragraph(
                "Click “Present” to fullscreen the slide. Only the slide is "
                        + "visible — Fullscreen.onClick(present).enter(stage) "
                        + "wraps the stage and the browser hides everything "
                        + "outside it. "
                        + "Left/Right arrows navigate, Escape exits; no on-"
                        + "screen exit button is needed because the audience "
                        + "would not see one anyway."));

        stateBadge.addClassName("status-badge");
        add(stateBadge);

        stage.addClassName("slideshow");
        stage.add(slideContent);
        add(stage);

        Div footer = new Div();
        footer.addClassName("slideshow-footer");
        footer.add(counter);
        add(footer);

        Button prev = new Button("Previous", e -> show(index - 1));
        prev.addClickShortcut(Key.ARROW_LEFT);
        Button next = new Button("Next", e -> show(index + 1));
        next.addClickShortcut(Key.ARROW_RIGHT);
        Button present = new Button("Present");
        present.addThemeVariants(ButtonVariant.PRIMARY);
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Present button's click trigger.
        Fullscreen.onClick(present).enter(stage);
        add(new HorizontalLayout(prev, next, present));

        show(0);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();

        stateBadge.bindText(fs.map(SlideshowView::badgeText));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));
        stage.bindClassName("live",
                fs.map(s -> s == FullscreenState.FULLSCREEN));
    }

    private void show(int target) {
        index = Math.floorMod(target, SLIDES.size());
        slideContent.setText(SLIDES.get(index));
        counter.setText("Slide " + (index + 1) + " of " + SLIDES.size());
    }

    private static String badgeText(FullscreenState state) {
        // FULLSCREEN: only the slide is visible (Component wrapper), the
        // badge is hidden — keep the idle text either way.
        return switch (state) {
        case FULLSCREEN, NOT_FULLSCREEN ->
            "Press Present to start the slideshow";
        case UNSUPPORTED -> "Fullscreen is not supported in this browser";
        case UNKNOWN -> "Detecting fullscreen support…";
        };
    }
}
