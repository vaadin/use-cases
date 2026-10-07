package com.example.muc03;

import java.util.stream.Collectors;

import com.example.signals.UserSessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MUC03View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC03NicknameUpdateTest extends SpringBrowserlessTest {

    @Autowired
    private MUC03Signals muc03Signals;

    @Autowired
    private UserSessionRegistry userSessionRegistry;

    @Test
    void leaderboardUpdatesWhenNicknameChanges() {
        navigate(MUC03View.class);
        runPendingSignalsTasks();

        // Register a second user and give them a score
        userSessionRegistry.registerUser("userB", "sessionB", "muc-03");
        muc03Signals.initializePlayerScore("userB", "sessionB");
        muc03Signals.startNewRound(100, 100);
        boolean awarded = muc03Signals.awardPoint("userB", "sessionB");
        runPendingSignalsTasks();

        // Verify the score actually got awarded (rules out leaderboard
        // signal short-circuit due to leaked state from a previous test).
        assertTrue(awarded, () -> "awardPoint returned false; "
                + diagnostics("after awardPoint"));
        Integer userBScore = muc03Signals.getLeaderboardSignal().peek()
                .get("userB:sessionB").peek();
        assertEquals(Integer.valueOf(1), userBScore,
                () -> "userB score should be 1 after awardPoint; "
                        + diagnostics("after awardPoint"));

        // Verify userB's name appears in the leaderboard
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains("userB")
                                && s.getText().contains("1 points")),
                () -> "Leaderboard should show userB with 1 point; "
                        + diagnostics("before setting nickname"));

        // Now change userB's nickname
        userSessionRegistry.setNickname("userB", "sessionB", "CoolPlayer");
        runPendingSignalsTasks();

        // The leaderboard should now show the nickname instead
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains("CoolPlayer")
                                && s.getText().contains("1 points")),
                () -> "Leaderboard should update to show nickname 'CoolPlayer'; "
                        + diagnostics("after setting nickname"));

        // The old username should no longer appear in the score label
        assertTrue(
                findInView(Span.class).all().stream()
                        .noneMatch(s -> s.getText() != null
                                && s.getText().contains("userB")
                                && s.getText().contains("1 points")),
                () -> "Leaderboard should no longer show 'userB' in score label; "
                        + diagnostics("after setting nickname"));
    }

    @Test
    void leaderboardUpdatesWhenNicknameCleared() {
        navigate(MUC03View.class);
        runPendingSignalsTasks();

        // Register userB, give them a nickname and a score
        userSessionRegistry.registerUser("userB", "sessionB", "muc-03");
        userSessionRegistry.setNickname("userB", "sessionB", "CoolPlayer");
        muc03Signals.initializePlayerScore("userB", "sessionB");
        muc03Signals.startNewRound(100, 100);
        boolean awarded = muc03Signals.awardPoint("userB", "sessionB");
        runPendingSignalsTasks();

        // Verify the score actually got awarded (rules out leaderboard
        // signal short-circuit due to leaked state from a previous test).
        assertTrue(awarded, () -> "awardPoint returned false; "
                + diagnostics("after awardPoint"));
        Integer userBScore = muc03Signals.getLeaderboardSignal().peek()
                .get("userB:sessionB").peek();
        assertEquals(Integer.valueOf(1), userBScore,
                () -> "userB score should be 1 after awardPoint; "
                        + diagnostics("after awardPoint"));

        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains("CoolPlayer")
                                && s.getText().contains("1 points")),
                () -> "Leaderboard should show nickname with 1 point; "
                        + diagnostics("before clearing nickname"));

        // Clear the nickname
        userSessionRegistry.setNickname("userB", "sessionB", "");
        runPendingSignalsTasks();

        // Should revert to username
        assertTrue(
                findInView(Span.class).all().stream()
                        .anyMatch(s -> s.getText() != null
                                && s.getText().contains("userB")
                                && s.getText().contains("1 points")),
                () -> "Leaderboard should revert to username when nickname cleared; "
                        + diagnostics("after clearing nickname"));
    }

    /**
     * Snapshot of state useful for diagnosing flakiness — leaderboard scores,
     * computed display names, and every Span on the view.
     */
    private String diagnostics(String label) {
        StringBuilder sb = new StringBuilder("\n--- DEBUG: ").append(label)
                .append(" ---\n");

        sb.append("leaderboard: ");
        muc03Signals.getLeaderboardSignal().peek().forEach((k, v) -> sb
                .append(k).append("=").append(v.peek()).append(" "));
        sb.append("\n");

        sb.append("active users: ")
                .append(userSessionRegistry.getActiveUsersSignal().peek()
                        .stream()
                        .map(s -> s.peek().getCompositeKey() + "(nickname="
                                + s.peek().nickname() + ")")
                        .collect(Collectors.joining(", ")))
                .append("\n");

        sb.append("display names: ")
                .append(userSessionRegistry.getDisplayNamesSignal().peek())
                .append("\n");

        sb.append("buttonVisible=")
                .append(muc03Signals.getButtonVisibleSignal().peek())
                .append(" clicksRemaining=")
                .append(muc03Signals.getClicksRemainingSignal().peek())
                .append("\n");

        sb.append("spans on view:\n");
        findInView(Span.class).all().forEach(
                s -> sb.append("  [").append(s.getText()).append("]\n"));

        return sb.toString();
    }
}
