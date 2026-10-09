package com.example.muc02;

import com.example.common.UseCaseDescription;
import com.example.security.CurrentUserSignal;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserSessionRegistry;
import com.example.views.ActiveUsersDisplay;
import com.example.views.ColoredAvatar;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * Multi-User Case 2: Collaborative Cursor Positions
 *
 * Demonstrates showing all users' cursor positions in real-time: - Each user
 * updates their own cursor position signal - All users read all cursor signals
 * - Map<UserId, Signal<CursorPosition>> - Real-time updates
 *
 * Key Patterns: - Per-user writable signals shared via static Map - All users
 * observe all signals - Efficient multi-signal updates - Collaborative
 * awareness indicators
 */
@Route(value = "muc-02", layout = MainLayout.class)
@PageTitle("Multi-User Case 2: Collaborative Cursors")
@UseCaseDescription("Showing every user's cursor position to all others in real time")
@Menu(order = 51, title = "MUC 2: Collaborative Cursors")
@StyleSheet("muc02.css")
@AnonymousAllowed
public class MUC02View extends VerticalLayout {

    private final String currentUser;
    private @Nullable SharedValueSignal<MUC02Signals.CursorPosition> myCursorSignal;
    private final MUC02Signals muc02Signals;
    private final UserSessionRegistry userSessionRegistry;
    private @Nullable String sessionId;
    private final java.util.IdentityHashMap<SharedValueSignal<MUC02Signals.CursorPosition>, String> cursorKeyMap = new java.util.IdentityHashMap<>();

    public MUC02View(CurrentUserSignal currentUserSignal,
            MUC02Signals muc02Signals,
            UserSessionRegistry userSessionRegistry) {
        this.currentUser = currentUserSignal.getUserSignal().peek()
                .getUsername();
        this.muc02Signals = muc02Signals;
        this.userSessionRegistry = userSessionRegistry;

        addClassName("muc02-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Multi-User Case 2: Collaborative Cursor Positions");

        Paragraph description = new Paragraph(
                "This use case demonstrates showing all users' cursor positions in a shared canvas area. "
                        + "Each user's cursor position is stored in their own signal, and all signals are read by all users. "
                        + "Try opening in multiple windows to see collaborative cursor awareness.");

        // Canvas area for cursor tracking
        Div canvas = new Div();
        canvas.setId("cursor-canvas");
        canvas.setWidthFull();
        canvas.addClassName("cursor-canvas");

        // Add cursor indicators for all users
        Div cursorsContainer = new Div();
        cursorsContainer.addClassName("cursors-container");
        canvas.add(cursorsContainer);

        // Render cursor indicators for all users
        renderAllCursors(cursorsContainer);

        // Track mouse movement
        canvas.getElement().addEventListener("mousemove", event -> {
            // Only update if attached (sessionId and myCursorSignal are set)
            if (myCursorSignal != null) {
                // Get mouse position relative to canvas
                double clientX = event.getEventData().get("event.offsetX")
                        .asDouble();
                double clientY = event.getEventData().get("event.offsetY")
                        .asDouble();
                myCursorSignal.set(new MUC02Signals.CursorPosition(
                        (int) clientX, (int) clientY));
            }
        }).addEventData("event.offsetX").addEventData("event.offsetY");

        // Active sessions display
        ActiveUsersDisplay activeSessionsBox = new ActiveUsersDisplay(
                userSessionRegistry, "muc-02");

        // Current users list (cursor tracking)
        H3 usersTitle = new H3("Cursor Tracking");
        Div usersList = new Div();
        usersList.addClassName("users-list");

        // Display cursor positions per session - reactive
        usersList.bindChildren(
                muc02Signals.getSessionCursorsSignal().map(cursors -> {
                    cursorKeyMap.clear();
                    cursors.forEach(
                            (key, signal) -> cursorKeyMap.put(signal, key));
                    return new java.util.ArrayList<>(cursors.values());
                }), this::createCursorListItem);

        // Info box
        Div infoBox = new Div();
        infoBox.addClassName("info-box");
        infoBox.add(new Paragraph(
                "💡 Each user's cursor position is stored in a SharedValueSignal in a shared Map. "
                        + "All users read all signals to display cursor indicators. This pattern enables "
                        + "real-time collaborative awareness without complex synchronization code. "
                        + "With Vaadin Push, updates propagate automatically to all connected clients."));

        add(title, description, activeSessionsBox, canvas, usersTitle,
                usersList, infoBox);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.sessionId = SessionIdHelper.getCurrentSessionId();
        this.myCursorSignal = muc02Signals.getCursorSignalForUser(currentUser,
                sessionId);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (sessionId != null) {
            muc02Signals.unregisterCursor(currentUser, sessionId);
        }
    }

    private void renderAllCursors(Div container) {
        // Reactive rendering of cursor indicators
        container.bindChildren(Signal.computed(() -> {
            var cursors = muc02Signals.getSessionCursorsSignal().get();
            cursorKeyMap.clear();
            cursors.forEach((key, signal) -> cursorKeyMap.put(signal, key));
            return new java.util.ArrayList<>(cursors.values());
        }), this::createCursorIndicator);
    }

    private String lookupSessionKey(
            SharedValueSignal<MUC02Signals.CursorPosition> positionSignal) {
        return cursorKeyMap.getOrDefault(positionSignal, "");
    }

    private java.util.Map<String, String> buildColorMap() {
        var users = userSessionRegistry.getActiveUsersSignal().peek();
        java.util.Map<String, String> map = new java.util.HashMap<>();
        for (var userSignal : users) {
            var user = userSignal.peek();
            map.put(user.getCompositeKey(), user.cssColor());
        }
        return map;
    }

    private HorizontalLayout createCursorListItem(
            SharedValueSignal<MUC02Signals.CursorPosition> positionSignal) {
        String sessionKey = lookupSessionKey(positionSignal);
        String username = sessionKey.split(":")[0];
        String userColor = buildColorMap().getOrDefault(sessionKey, "#9E9E9E");

        HorizontalLayout userItem = new HorizontalLayout();
        userItem.setSpacing(true);
        userItem.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        userItem.setWidthFull();
        userItem.addClassName("cursor-list-item");

        Div colorDot = new Div();
        colorDot.addClassName("color-dot");
        // Per-user color is dynamic — keep inline
        colorDot.getStyle().set("background-color", userColor);

        ColoredAvatar avatar = new ColoredAvatar(username, userColor, 32);

        Div userLabel = new Div();
        userLabel
                .bindText(userSessionRegistry.getDisplayNameSignal(sessionKey));
        userLabel.addClassName("user-label");

        Div positionLabel = new Div(
                positionSignal.map(MUC02Signals.CursorPosition::toString));
        positionLabel.addClassName("position-label");

        userItem.add(colorDot, avatar, userLabel, positionLabel);
        return userItem;
    }

    private Div createCursorIndicator(
            SharedValueSignal<MUC02Signals.CursorPosition> positionSignal) {
        String sessionKey = lookupSessionKey(positionSignal);
        String userColor = buildColorMap().getOrDefault(sessionKey, "#9E9E9E");

        Div cursorIndicator = new Div();
        cursorIndicator.addClassName("cursor-indicator");
        // Per-user color is dynamic — keep inline
        cursorIndicator.getStyle().set("background-color", userColor);

        cursorIndicator.getStyle().bind("left", positionSignal
                .map(pos -> pos != null ? pos.x() + "px" : "0px"));
        cursorIndicator.getStyle().bind("top", positionSignal
                .map(pos -> pos != null ? pos.y() + "px" : "0px"));

        Div label = new Div();
        label.bindText(userSessionRegistry.getDisplayNameSignal(sessionKey));
        label.addClassName("cursor-indicator-label");
        // Per-user color is dynamic — keep inline
        label.getStyle().set("background-color", userColor);

        cursorIndicator.add(label);
        return cursorIndicator;
    }
}
