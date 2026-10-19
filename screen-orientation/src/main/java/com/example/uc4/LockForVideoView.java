package com.example.uc4;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
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
 * UC4 — Lock for video.
 * <p>
 * Classic media app pattern: when the user hits "Play", the player goes
 * fullscreen and the screen is locked to landscape. "Stop" releases the lock
 * and leaves fullscreen. The lock request goes through
 * {@link ScreenOrientation#lock(ScreenOrientationType, com.vaadin.flow.function.SerializableRunnable, com.vaadin.flow.function.SerializableConsumer)
 * ScreenOrientation.lock(...)} so success and failure reach the server;
 * fullscreen is requested with {@link Fullscreen#onClick
 * Fullscreen.onClick(play).enter(stage)}, since browsers only honour the lock
 * inside a fullscreen document. When the user leaves fullscreen another way
 * (Esc, the back gesture), the browser releases the lock, and the view follows
 * {@link Fullscreen#stateSignal()} to show that.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Lock for video")
@UseCaseDescription("Locking the screen to landscape during fullscreen video")
@Menu(order = 4, title = "UC4 — Lock for video")
@StyleSheet("uc4.css")
public class LockForVideoView extends VerticalLayout {

    /** What the view knows about the lock. */
    record LockStatus(boolean locked, boolean failed, String message) {
        static final LockStatus IDLE = new LockStatus(false, false, "Idle");
    }

    private final ValueSignal<LockStatus> status = new ValueSignal<>(
            LockStatus.IDLE);

    public LockForVideoView() {
        addClassName("uc4-view");
        add(new H1("UC4 — Lock for video"));
        add(new Paragraph("Click \"Play\" to enter fullscreen and lock the "
                + "screen to landscape. The lock usually only succeeds on "
                + "phones and tablets, in fullscreen. Desktop browsers "
                + "reject it, typically with NOT_SUPPORTED, and the badge "
                + "shows that. Click \"Stop\" to release the lock."));

        Signal<ScreenOrientationData> orientation = ScreenOrientation
                .orientationSignal();

        Span bigIcon = new Span("🎬");
        bigIcon.addClassName("uc4-icon");
        Span playingLabel = new Span();
        playingLabel.bindText(Signal.computed(() -> status.get().locked()
                ? "Playing — orientation locked to "
                        + orientation.get().type().getClientValue()
                : "Press Play to start"));
        // Stop sits inside the stage, so it stays reachable while the stage
        // is fullscreen.
        Button stop = new Button("Stop (unlock)", e -> stopPlayback());
        Div stage = new Div(bigIcon, playingLabel, stop);
        stage.addClassName("uc4-stage");

        Button play = new Button("Play (lock landscape)", e -> startPlayback());
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Play button's click trigger; the lock then runs inside the fullscreen
        // document, which is where browsers honour it.
        Fullscreen.onClick(play).enter(stage);
        add(stage, play);

        Span lockBadge = new Span();
        lockBadge.addClassName("status-badge");
        lockBadge.bindText(status.map(LockStatus::message));
        lockBadge.bindClassName("error", status.map(LockStatus::failed));
        HorizontalLayout statusRow = new HorizontalLayout(
                new Span("Lock state:"), lockBadge);
        statusRow.setAlignItems(Alignment.BASELINE);
        add(statusRow);

        // Leaving fullscreen releases the lock in the browser, also when the
        // user presses Esc instead of Stop. The server is not told that the
        // lock ended, so follow the fullscreen state (API-GAPS.md #3).
        Signal.effect(this, () -> {
            if (Fullscreen.stateSignal().get() != FullscreenState.FULLSCREEN
                    && status.peek().locked()) {
                status.set(new LockStatus(false, false,
                        "Released — fullscreen was closed"));
            }
        });
    }

    private void startPlayback() {
        ScreenOrientation.lock(ScreenOrientationType.LANDSCAPE_PRIMARY,
                () -> status.set(
                        new LockStatus(true, false, "Locked to landscape")),
                error -> status.set(new LockStatus(false, true,
                        "Lock failed: " + error.errorCode().name())));
    }

    private void stopPlayback() {
        ScreenOrientation.unlock();
        Fullscreen.exit();
        status.set(LockStatus.IDLE);
    }
}
