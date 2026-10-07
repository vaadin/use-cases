package com.example.muc03;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

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
        navigate(MUC03View.class);
        runPendingSignalsTasks();

        // Initialize User B in the leaderboard
        muc03Signals.initializePlayerScore("userB", "sessionB");

        // Start a round
        muc03Signals.startNewRound(100, 100);
        runPendingSignalsTasks();

        // User B clicks first and gets the point
        boolean moreClicks = muc03Signals.awardPoint("userB", "sessionB");
        assertTrue(moreClicks); // 4 clicks remaining
        runPendingSignalsTasks();

        // Clicks remaining should be 4
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains("Targets remaining: 4")));

        // User B's score should appear in leaderboard
        assertTrue(findInView(Span.class).all().stream().anyMatch(
                s -> s.getText() != null && s.getText().contains("1 points")));
    }

    @Test
    void resetScoresClearsLeaderboard() {
        navigate(MUC03View.class);
        runPendingSignalsTasks();

        // Add scores for two players
        muc03Signals.initializePlayerScore("userB", "sessionB");
        muc03Signals.startNewRound(100, 100);
        muc03Signals.awardPoint("userB", "sessionB");
        runPendingSignalsTasks();

        // Reset scores
        Button resetButton = findInView(Button.class).all().stream()
                .filter(b -> "Reset Scores".equals(b.getText())).findFirst()
                .orElseThrow();
        test(resetButton).click();
        runPendingSignalsTasks();

        // No score spans should remain (leaderboard cleared)
        assertTrue(findInView(Span.class).all().stream().noneMatch(
                s -> s.getText() != null && s.getText().contains("points")));
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
}
