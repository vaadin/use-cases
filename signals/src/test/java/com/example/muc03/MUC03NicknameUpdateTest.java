package com.example.muc03;

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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = MUC03View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC03NicknameUpdateTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void leaderboardUpdatesWhenNicknameChanges() {
        try (var app = createApp()) {
            var alice = app.newUser("alice", "USER").newWindow();
            var bob = app.newUser("bob", "USER").newWindow();
            alice.navigate(MUC03View.class);
            bob.navigate(MUC03View.class);
            bobScoresAPoint(alice, bob);

            assertEquals(1, scoreLabels(alice, "bob: 1 points"),
                    () -> "Leaderboard should show bob with 1 point; "
                            + spans(alice));

            setNickname(bob, "CoolPlayer");

            assertEquals(1, scoreLabels(alice, "CoolPlayer: 1 points"),
                    () -> "Leaderboard should show the nickname; "
                            + spans(alice));
            assertEquals(0, scoreLabels(alice, "bob: 1 points"),
                    () -> "Leaderboard should no longer show the username; "
                            + spans(alice));
        }
    }

    @Test
    void leaderboardUpdatesWhenNicknameCleared() {
        try (var app = createApp()) {
            var alice = app.newUser("alice", "USER").newWindow();
            var bob = app.newUser("bob", "USER").newWindow();
            alice.navigate(MUC03View.class);
            bob.navigate(MUC03View.class);
            setNickname(bob, "CoolPlayer");
            bobScoresAPoint(alice, bob);

            assertEquals(1, scoreLabels(alice, "CoolPlayer: 1 points"),
                    () -> "Leaderboard should show the nickname; "
                            + spans(alice));

            setNickname(bob, "");

            assertEquals(1, scoreLabels(alice, "bob: 1 points"),
                    () -> "Leaderboard should revert to the username; "
                            + spans(alice));
        }
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

    private static void setNickname(BrowserlessUIContext window,
            String nickname) {
        window.test(window.find(TextField.class).all().stream()
                .filter(f -> "Set nickname...".equals(f.getPlaceholder()))
                .findFirst().orElseThrow()).setValue(nickname);
        window.runPendingSignalsTasks();
    }

    private static long scoreLabels(BrowserlessUIContext window, String text) {
        return window.findInView(Span.class).all().stream()
                .filter(s -> text.equals(s.getText())).count();
    }

    private static String spans(BrowserlessUIContext window) {
        return window.findInView(Span.class).all().stream().map(Span::getText)
                .toList().toString();
    }
}
