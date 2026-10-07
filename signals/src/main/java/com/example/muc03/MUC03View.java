package com.example.muc03;

import java.util.Random;

import com.example.security.CurrentUserSignal;
import com.example.signals.SessionIdHelper;
import com.example.signals.UserSessionRegistry;
import com.example.views.ActiveUsersDisplay;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;

/**
 * Multi-User Case 3: Competitive Button Click Game
 *
 * Demonstrates race condition handling and conflict resolution: - Button
 * appears at random position - Fastest clicker gets the point - All users see
 * shared leaderboard - Server-authoritative scoring (only one can win)
 *
 * Key Patterns: - Optimistic UI updates - Server-side signal coordination -
 * Atomic operations on shared signals - Conflict resolution strategy
 */
@Route(value = "muc-03", layout = MainLayout.class)
@PageTitle("Multi-User Case 3: Click Game")
@Menu(order = 52, title = "MUC 3: Click Race Game")
@StyleSheet("muc03.css")
@AnonymousAllowed
public class MUC03View extends VerticalLayout {

    private final String currentUser;
    private final MUC03Signals muc03Signals;
    private final UserSessionRegistry userSessionRegistry;
    private final Random random = new Random();
    private @Nullable String sessionId;
    private final java.util.IdentityHashMap<com.vaadin.flow.signals.shared.SharedValueSignal<Integer>, String> scoreKeyMap = new java.util.IdentityHashMap<>();

    public MUC03View(CurrentUserSignal currentUserSignal,
            MUC03Signals muc03Signals,
            UserSessionRegistry userSessionRegistry) {
        this.currentUser = currentUserSignal.getUserSignal().peek()
                .getUsername();
        this.muc03Signals = muc03Signals;
        this.userSessionRegistry = userSessionRegistry;

        addClassName("muc03-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Multi-User Case 3: Competitive Button Click Game");

        Paragraph description = new Paragraph(
                "This demonstrates race condition handling in a multi-user game. "
                        + "Click START to begin a round. Each round has 5 targets that appear one after another at random locations with random delays. "
                        + "The fastest user to click each target gets a point. Race to get the most points!");

        // Round status
        Div roundStatus = new Div();
        roundStatus.addClassName("round-status");
        roundStatus.bindText(muc03Signals.getRoundNumberSignal()
                .map(round -> round == 0 ? "Click START to begin"
                        : "Round " + round));

        Div clicksStatus = new Div();
        clicksStatus.addClassName("clicks-status");
        clicksStatus.bindText(muc03Signals.getClicksRemainingSignal().map(
                clicks -> clicks > 0 ? "Targets remaining: " + clicks : ""));

        // Game area
        Div gameArea = new Div();
        gameArea.setWidthFull();
        gameArea.addClassName("game-area");

        // Target button
        Button targetButton = new Button("CLICK ME!", event -> {
            handleButtonClick();
        });
        targetButton.addThemeName("primary");
        targetButton.addThemeName("large");
        targetButton.addClassName("target-button");

        // Bind button text to show clicks remaining
        targetButton.bindText(muc03Signals.getClicksRemainingSignal()
                .map(clicks -> "CLICK ME! (" + clicks + ")"));

        targetButton.bindVisible(muc03Signals.getButtonVisibleSignal());
        targetButton.getStyle().bind("left",
                muc03Signals.getButtonLeftSignal().map(left -> left + "px"));
        targetButton.getStyle().bind("top",
                muc03Signals.getButtonTopSignal().map(top -> top + "px"));

        gameArea.add(targetButton);

        // Controls
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(true);

        Button startButton = new Button("START ROUND", event -> {
            startNewRound();
        });
        startButton.addThemeName("success");

        Button resetButton = new Button("Reset Scores", event -> {
            muc03Signals.resetLeaderboard();
        });
        resetButton.addThemeName("error");
        resetButton.addThemeName("small");

        controls.add(startButton, resetButton);

        // Active sessions display
        ActiveUsersDisplay activeSessionsBox = new ActiveUsersDisplay(
                userSessionRegistry, "muc-03");

        // Leaderboard
        H3 leaderboardTitle = new H3("Leaderboard");
        Div leaderboardDiv = new Div();
        leaderboardDiv.addClassName("leaderboard");

        // Bind leaderboard display - sorted by score descending
        leaderboardDiv
                .bindChildren(com.vaadin.flow.signals.Signal.computed(() -> {
                    var scores = muc03Signals.getLeaderboardSignal().get();
                    scoreKeyMap.clear();
                    scores.forEach(
                            (key, signal) -> scoreKeyMap.put(signal, key));
                    return scores.entrySet().stream()
                            .sorted((e1, e2) -> Integer.compare(
                                    e2.getValue().get(), e1.getValue().get()))
                            .map(java.util.Map.Entry::getValue).toList();
                }), this::createLeaderboardItem);

        // Info box
        Div infoBox = new Div();
        infoBox.addClassName("info-box");
        infoBox.add(new Paragraph(
                "💡 This demonstrates atomic operations and conflict resolution in multi-user scenarios. "
                        + "Each round has 5 targets that appear with random delays (500-2000ms) at random positions. "
                        + "When multiple users try to click simultaneously, only the first click is counted (atomic operation). "
                        + "The leaderboard is a shared signal that updates for all users in real-time. "
                        + "Race against other players to get the most points!"));

        add(title, description, activeSessionsBox, roundStatus, clicksStatus,
                gameArea, controls, leaderboardTitle, leaderboardDiv, infoBox);
    }

    private void startNewRound() {
        // Position button randomly and start a new round
        int[] position = getRandomPosition();
        muc03Signals.startNewRound(position[0], position[1]);
    }

    private void handleButtonClick() {
        if (sessionId == null) {
            return;
        }
        // Atomic operation: Only first click counts (handled by
        // CollaborativeSignals)
        boolean moreClicksRemain = muc03Signals.awardPoint(currentUser,
                sessionId);

        // If there are more clicks remaining, reposition button after random
        // delay
        if (moreClicksRemain) {
            new Thread(() -> {
                try {
                    // Random delay between 500ms and 2000ms
                    int delay = 500 + random.nextInt(1500);
                    Thread.sleep(delay);

                    // Reposition button at random location
                    int[] position = getRandomPosition();
                    muc03Signals.repositionButton(position[0], position[1]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private int[] getRandomPosition() {
        int left = random.nextInt(400);
        int top = random.nextInt(200);
        return new int[] { left, top };
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.sessionId = SessionIdHelper.getCurrentSessionId();
        muc03Signals.initializePlayerScore(currentUser, sessionId);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (sessionId != null) {
            muc03Signals.unregisterScore(currentUser, sessionId);
        }
    }

    private HorizontalLayout createLeaderboardItem(
            com.vaadin.flow.signals.shared.SharedValueSignal<Integer> scoreSignal) {
        String sessionKey = scoreKeyMap.getOrDefault(scoreSignal, "");
        boolean isCurrentSession = sessionId != null
                && sessionKey.equals(currentUser + ":" + sessionId);
        String username = sessionKey.split(":")[0];

        var displayNameSignal = userSessionRegistry
                .getDisplayNameSignal(sessionKey);

        HorizontalLayout item = new HorizontalLayout();
        item.setSpacing(true);
        item.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        item.addClassName("leaderboard-item");
        if (isCurrentSession) {
            item.addClassName("current-session");
        }

        Image avatar = new Image(MainLayout.getProfilePicturePath(username),
                "");
        avatar.setWidth("32px");
        avatar.setHeight("32px");
        avatar.addClassName("leaderboard-avatar");

        Span nameLabel = new Span();
        nameLabel.bindText(Signal.computed(() -> String.format("%s: %d points",
                displayNameSignal.get(), scoreSignal.get())));

        item.add(avatar, nameLabel);
        return item;
    }
}
