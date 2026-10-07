package com.example.uc3;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.wakelock.WakeLock;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC3 — Presentation / slideshow.
 * <p>
 * The slideshow has Start, Stop, and Next buttons. The wake lock is held only
 * while a presentation is running, so the speaker is not stuck with a black
 * screen mid-sentence — but the lock is dropped immediately when they stop
 * presenting, so it does not bleed into post-talk browsing.
 * <p>
 * The wiring uses {@link Signal#effect(Object, Runnable)} so that the lock
 * follows the {@code presenting} signal automatically: whatever flips that
 * signal (Start, Stop, last slide reached) also drives request / release. The
 * lock is also released in {@link #onDetach}, so closing the tab does not leak
 * a held lock.
 */
@Route(value = "uc3", layout = MainLayout.class)
@Menu(order = 3, title = "UC3 — Slideshow")
@StyleSheet("uc3.css")
public class SlideshowView extends VerticalLayout {

    private record Slide(String title, String body) {
    }

    private static final List<Slide> DECK = List.of(
            new Slide("Welcome", "A quick tour of the Screen Wake Lock API."),
            new Slide("Why hold the lock",
                    "Speaker notes, demos, and long pauses all let the screen "
                            + "dim. The Wake Lock API prevents that."),
            new Slide("Lifecycle",
                    "The browser drops the lock when the tab hides. Flow's "
                            + "WakeLock auto-reacquires when the tab "
                            + "returns."),
            new Slide("Be a good citizen",
                    "Release the lock when the user is no longer actively "
                            + "viewing content. Do not hold it for "
                            + "background tabs."),
            new Slide("End", "Questions?"));

    private final ValueSignal<Boolean> presenting = new ValueSignal<>(
            Boolean.FALSE);
    private final ValueSignal<Integer> slideIndex = new ValueSignal<>(0);

    private final Span lockBadge = new Span();
    private final Div slideTitle = new Div();
    private final Div slideBody = new Div();
    private final Button startButton = new Button("Start presentation");
    private final Button stopButton = new Button("Stop");
    private final Button nextButton = new Button("Next slide");

    public SlideshowView() {
        addClassName("uc3-view");
        add(new H1("UC3 — Presentation slideshow"));
        add(new Paragraph("Click Start to begin presenting — the wake lock "
                + "is requested while you advance through slides and "
                + "released as soon as you stop or reach the last slide."));

        lockBadge.addClassName("status-badge");
        add(new HorizontalLayout(new Span("Wake lock:"), lockBadge));

        Div slide = new Div();
        slide.addClassName("slide");
        slideTitle.addClassName("slide-title");
        slide.add(slideTitle, slideBody);
        add(slide);

        HorizontalLayout controls = new HorizontalLayout(startButton,
                nextButton, stopButton);
        add(controls);

        startButton.addClickListener(e -> {
            slideIndex.set(0);
            presenting.set(Boolean.TRUE);
        });
        stopButton.addClickListener(e -> presenting.set(Boolean.FALSE));
        nextButton.addClickListener(e -> {
            int next = slideIndex.peek() + 1;
            if (next >= DECK.size()) {
                presenting.set(Boolean.FALSE);
            } else {
                slideIndex.set(next);
            }
        });

        slideTitle.bindText(slideIndex.map(i -> DECK.get(clamp(i)).title()));
        slideBody.bindText(slideIndex.map(i -> DECK.get(clamp(i)).body()));

        startButton.bindEnabled(presenting.map(p -> !Boolean.TRUE.equals(p)));
        nextButton.bindEnabled(presenting);
        stopButton.bindEnabled(presenting);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<Boolean> active = WakeLock.activeSignal();

        lockBadge.bindText(active.map(held -> Boolean.TRUE.equals(held)
                ? "Holding — slide visible until you stop"
                : "Released"));
        lockBadge.bindClassName("active", active);

        Signal.effect(this, () -> {
            if (Boolean.TRUE.equals(presenting.get())) {
                WakeLock.request();
            } else {
                WakeLock.release();
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        // Defensive — the effect already released on stop, but a user who
        // navigates away mid-presentation would otherwise leave the want-lock
        // flag set on the client.
        WakeLock.release();
        super.onDetach(detachEvent);
    }

    private static int clamp(int i) {
        return Math.max(0, Math.min(DECK.size() - 1, i));
    }
}
