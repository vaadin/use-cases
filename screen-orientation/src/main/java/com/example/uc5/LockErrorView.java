package com.example.uc5;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationLockError;
import com.vaadin.flow.component.screenorientation.ScreenOrientationLockErrorCode;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Lock error UX.
 * <p>
 * Shows how a view can react to each way
 * {@link ScreenOrientation#lock(ScreenOrientationType, com.vaadin.flow.function.SerializableRunnable, com.vaadin.flow.function.SerializableConsumer)
 * ScreenOrientation.lock(...)} can fail. The error callback receives a
 * {@link ScreenOrientationLockError}; the view switches over its
 * {@link ScreenOrientationLockErrorCode} to pick a message for the user, and
 * logs the browser's own {@link ScreenOrientationLockError#debugInfo()} only as
 * a detail, because its wording differs between browsers.
 * <ul>
 * <li>{@code NOT_SUPPORTED} — a lock outside fullscreen, or on a device that
 * cannot rotate (any desktop);
 * <li>{@code ABORT} — a second lock replaced the first before the browser
 * answered it, on desktops too;
 * <li>{@code SECURITY} — the page is hidden or not allowed to lock (a sandboxed
 * iframe); no button here can cause it.
 * </ul>
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Lock error UX")
@UseCaseDescription("Handling the ways locking the screen orientation can fail")
@Menu(order = 5, title = "UC5 — Lock error UX")
@StyleSheet("uc5.css")
public class LockErrorView extends VerticalLayout {

    private static final DateTimeFormatter TIME = DateTimeFormatter
            .ofPattern("HH:mm:ss");

    private final Div log = new Div();

    public LockErrorView() {
        addClassName("uc5-view");
        add(new H1("UC5 — Lock error UX"));
        add(new Paragraph("Each button asks the browser to lock the screen "
                + "orientation. The log shows the error code the server "
                + "received and the message the view picks for it. On a "
                + "desktop a lock fails with NOT_SUPPORTED. \"Two locks in "
                + "a row\" fails the first one with ABORT, because the "
                + "second replaces it before the browser answers. On a "
                + "phone, a lock in fullscreen succeeds."));

        Button lockWithoutFullscreen = new Button("Lock without fullscreen",
                e -> attempt(ScreenOrientationType.LANDSCAPE_PRIMARY));
        Button fullscreenLock = new Button("Enter fullscreen and lock",
                e -> attempt(ScreenOrientationType.LANDSCAPE_PRIMARY));
        // Fullscreen needs the click's user gesture, so it is requested on the
        // client before the click reaches the server and locks.
        Fullscreen.onClick(fullscreenLock).enter();
        Button rapidLocks = new Button("Two locks in a row", e -> {
            attempt(ScreenOrientationType.LANDSCAPE_PRIMARY);
            attempt(ScreenOrientationType.PORTRAIT_PRIMARY);
        });
        Button unlock = new Button("Unlock and leave fullscreen", e -> {
            ScreenOrientation.unlock(() -> addLog("uc5-info", "Unlocked"));
            Fullscreen.exit();
        });

        HorizontalLayout actions = new HorizontalLayout(lockWithoutFullscreen,
                fullscreenLock, rapidLocks, unlock);
        actions.addClassName("uc5-actions");
        add(actions);

        log.addClassName("uc5-log");
        addLog("uc5-info", "Click a button above to attempt a lock.");
        add(log);
    }

    private void attempt(ScreenOrientationType target) {
        ScreenOrientation.lock(target,
                () -> addLog("uc5-ok",
                        "✓ Locked to " + target.getClientValue()),
                error -> addLog("uc5-err",
                        "✗ " + target.getClientValue() + " — "
                                + error.errorCode().name() + ": "
                                + userMessage(error.errorCode()) + " (browser: "
                                + error.debugInfo() + ")"));
    }

    /**
     * The message a user would see for each failure. The switch is exhaustive,
     * so a new error code fails the build until it gets a message.
     */
    static String userMessage(ScreenOrientationLockErrorCode code) {
        return switch (code) {
        // Covers both "not in fullscreen" and "can never lock", which the
        // code does not tell apart (API-GAPS.md #4).
        case NOT_SUPPORTED ->
            "this device can not lock the screen here. Try fullscreen on a phone.";
        case ABORT -> "a newer lock replaced this one.";
        case SECURITY -> "this page is not allowed to lock the screen.";
        case UNKNOWN -> "the screen could not be locked.";
        };
    }

    private void addLog(String cls, String text) {
        Div line = new Div();
        line.addClassName(cls);
        line.setText("[" + LocalTime.now().format(TIME) + "] " + text);
        // Newest at the top to keep recent activity in view without
        // scrolling.
        log.getElement().insertChild(0, line.getElement());
    }
}
