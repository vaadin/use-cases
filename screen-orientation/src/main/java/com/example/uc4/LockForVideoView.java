package com.example.uc4;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationData;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC4 — Lock landscape for fullscreen video playback.
 * <p>
 * Classic media app pattern: when the user hits "Play", the player goes
 * fullscreen and the screen is locked to landscape. Closing the player releases
 * the lock. The lock request goes through
 * {@link ScreenOrientation#lock(ScreenOrientationType, com.vaadin.flow.function.SerializableRunnable, com.vaadin.flow.function.SerializableConsumer)
 * ScreenOrientation.lock(...)} so success and failure are surfaced reactively;
 * fullscreen is requested by binding
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(play).enter(stage)} to the Play button, since the lock is
 * only honoured inside a fullscreen document.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Lock landscape for video")
@Menu(order = 4, title = "UC4 — Lock for video")
@StyleSheet("uc4.css")
public class LockForVideoView extends VerticalLayout {

    private final Div stage = new Div();
    private final Span playingLabel = new Span();
    private final Span lockBadge = new Span();
    private final ValueSignal<Boolean> locked = new ValueSignal<>(false);
    private final ValueSignal<String> lockMessage = new ValueSignal<>("Idle");
    private final ValueSignal<String> lockBadgeMod = new ValueSignal<>("");

    public LockForVideoView() {
        addClassName("uc4-view");
        add(new H1("UC4 — Lock landscape for video"));
        add(new Paragraph("Click \"Play\" to enter fullscreen and lock the "
                + "screen to landscape. The lock typically only succeeds on "
                + "mobile/tablet devices and in fullscreen mode — desktop "
                + "browsers will report a SecurityError or NotSupportedError, "
                + "and the badge will reflect that. Click \"Stop\" to "
                + "release the lock."));

        stage.addClassName("uc4-stage");
        stage.setId("video-stage");
        Span bigIcon = new Span("🎬");
        bigIcon.addClassName("big-icon");
        playingLabel.setText("Press Play to start");
        stage.add(bigIcon, playingLabel);
        add(stage);

        Button play = new Button("Play (lock landscape)", e -> startPlayback());
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Play button's click trigger; the lock then runs inside the fullscreen
        // document, which is where browsers honour it.
        Fullscreen.onClick(play).enter(stage);
        Button stop = new Button("Stop (unlock)", e -> stopPlayback());
        HorizontalLayout actions = new HorizontalLayout(play, stop);
        add(actions);

        HorizontalLayout status = new HorizontalLayout();
        lockBadge.addClassName("status-badge");
        status.add(new Span("Lock state:"), lockBadge);
        status.setAlignItems(Alignment.BASELINE);
        status.setSpacing(true);
        add(status);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<ScreenOrientationData> orientation = ScreenOrientation
                .orientationSignal(attachEvent.getUI());

        playingLabel.bindText(Signal.computed(() -> locked.get()
                ? "Playing — orientation locked to "
                        + orientation.get().type().getClientValue()
                : "Press Play to start"));
        lockBadge.bindText(lockMessage);
        lockBadge.bindClassName("error",
                lockBadgeMod.map(s -> s.equals("error")));
    }

    private void startPlayback() {
        ScreenOrientation.lock(ScreenOrientationType.LANDSCAPE_PRIMARY, () -> {
            locked.set(true);
            lockMessage.set("Locked to landscape");
            lockBadgeMod.set("");
        }, error -> {
            locked.set(false);
            lockMessage.set("Lock failed: " + error.errorCode().name() + " — "
                    + error.debugInfo());
            lockBadgeMod.set("error");
        });
    }

    private void stopPlayback() {
        ScreenOrientation.unlock();
        Fullscreen.exit();
        locked.set(false);
        lockMessage.set("Idle");
        lockBadgeMod.set("");
    }
}
