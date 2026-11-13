package com.example.muc03;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MUC03View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC03ViewTest extends SpringBrowserlessTest {

    @Autowired
    private MUC03Signals muc03Signals;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void viewRendersWithGameControls() {
        navigate(MUC03View.class);

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "START ROUND".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Reset Scores".equals(b.getText())));
    }

    @Test
    void initialStatusShowsClickStart() {
        navigate(MUC03View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> "Click START to begin".equals(d.getText())));
    }

    @Test
    void startRoundUpdatesSharedState() {
        navigate(MUC03View.class);
        runPendingSignalsTasks();

        // Click START ROUND
        Button startButton = findInView(Button.class).all().stream()
                .filter(b -> "START ROUND".equals(b.getText())).findFirst()
                .orElseThrow();
        test(startButton).click();
        runPendingSignalsTasks();

        // Round number should update to 1
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> "Round 1".equals(d.getText())));

        // Target button should become visible with clicks remaining
        assertTrue(findInView(Button.class).all().stream().anyMatch(
                b -> b.getText() != null && b.getText().contains("CLICK ME!")));
    }

    @Test
    void multiplePlayersCompeteForPoints() {
        try (var app = createApp()) {
            var alice = app.newUser("alice", "USER").newWindow();
            var bob = app.newUser("bob", "USER").newWindow();
            alice.navigate(MUC03View.class);
            bob.navigate(MUC03View.class);

            bobScoresAPoint(alice, bob);

            assertTrue(alice.findInView(Div.class).all().stream()
                    .anyMatch(d -> "Targets remaining: 4".equals(d.getText())));
            assertTrue(alice.findInView(Span.class).all().stream()
                    .anyMatch(s -> "bob: 1 points".equals(s.getText())));
            assertTrue(alice.findInView(Span.class).all().stream()
                    .anyMatch(s -> "alice: 0 points".equals(s.getText())));
        }
    }

    @Test
    void resetScoresClearsLeaderboard() {
        try (var app = createApp()) {
            var alice = app.newUser("alice", "USER").newWindow();
            var bob = app.newUser("bob", "USER").newWindow();
            alice.navigate(MUC03View.class);
            bob.navigate(MUC03View.class);
            bobScoresAPoint(alice, bob);

            alice.test(alice.findInView(Button.class).withText("Reset Scores")
                    .single()).click();
            alice.runPendingSignalsTasks();

            // No score spans remain for anyone (leaderboard cleared)
            for (var window : List.of(alice, bob)) {
                assertTrue(window.findInView(Span.class).all().stream()
                        .noneMatch(s -> s.getText() != null
                                && s.getText().contains("points")));
            }
        }
    }

    @Test
    void clickingTargetButtonAwardsPointAndDecrementsClicks() {
        navigate(MUC03View.class);
        runPendingSignalsTasks();

        // Initialize current user's score and start a round via signals
        muc03Signals.initializePlayerScore("user", "session0");
        muc03Signals.startNewRound(100, 100);
        runPendingSignalsTasks();

        // Target button should be visible — click it via the UI
        Button targetButton = findInView(Button.class).all().stream().filter(
                b -> b.getText() != null && b.getText().contains("CLICK ME!"))
                .findFirst().orElseThrow();
        test(targetButton).click();
        runPendingSignalsTasks();

        // After click, clicks remaining should be 4
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains("Targets remaining: 4")));
    }

    private SecuredBrowserlessApplicationContext<Authentication> createApp() {
        return SpringBrowserlessApplicationContext.createSecured(
                applicationContext, b -> b.withViewPackages(MUC03View.class)
                        .withSessionLockIsolation(true));
    }

    private static void bobScoresAPoint(BrowserlessUIContext alice,
            BrowserlessUIContext bob) {
        alice.test(
                alice.findInView(Button.class).withText("START ROUND").single())
                .click();
        alice.runPendingSignalsTasks();
        bob.test(
                bob.findInView(Button.class).withText("CLICK ME! (5)").single())
                .click();
        bob.runPendingSignalsTasks();
    }
}
