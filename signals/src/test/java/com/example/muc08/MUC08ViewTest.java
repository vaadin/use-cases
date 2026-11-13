package com.example.muc08;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SecuredBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextArea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MUC08View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC08ViewTest extends SpringBrowserlessTest {

    @Autowired
    private MUC08Signals muc08Signals;

    @Autowired
    private ApplicationContext applicationContext;

    // Browserless queries only return visible components, so a hidden banner
    // is simply not found.
    private boolean bannerShown() {
        return findInView(Div.class).all().stream()
                .anyMatch(d -> d.hasClassName("announcement-banner"));
    }

    private Span bannerText() {
        return findInView(Span.class).all().stream()
                .filter(s -> s.hasClassName("announcement-text")).findFirst()
                .orElseThrow();
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }

    @Test
    void viewRendersWithBannerHiddenAndConsole() {
        navigate(MUC08View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H2.class).single().getText()
                .contains("Broadcast an Announcement"));
        assertNotNull(findInView(TextArea.class).single());
        assertNotNull(button("Post announcement"));
        assertFalse(button("Clear announcement").isEnabled());
        assertFalse(bannerShown());
        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> "No active announcement.".equals(p.getText())));
    }

    @Test
    void postingShowsBannerAndNotification() {
        navigate(MUC08View.class);
        runPendingSignalsTasks();

        test(findInView(TextArea.class).single())
                .setValue("Maintenance in 10 minutes");
        test(button("Post announcement")).click();
        runPendingSignalsTasks();

        assertTrue(bannerShown());
        assertEquals("Maintenance in 10 minutes", bannerText().getText());
        assertTrue(button("Clear announcement").isEnabled());
        assertEquals("", findInView(TextArea.class).single().getValue());
        assertEquals("Announcement: Maintenance in 10 minutes",
                test(find(Notification.class).single()).getText());

        MUC08Signals.Announcement stored = muc08Signals.getAnnouncementSignal()
                .peek();
        assertNotNull(stored);
        assertEquals("user", stored.author());
    }

    @Test
    @WithAnonymousUser
    void anonymousVisitorCanPostAsGuest() {
        navigate(MUC08View.class);
        runPendingSignalsTasks();

        test(findInView(TextArea.class).single())
                .setValue("Hello from a guest");
        test(button("Post announcement")).click();
        runPendingSignalsTasks();

        MUC08Signals.Announcement stored = muc08Signals.getAnnouncementSignal()
                .peek();
        assertNotNull(stored);
        assertTrue(stored.author().matches("guest-\\d+"),
                () -> "Unexpected guest author " + stored.author());
        assertTrue(bannerShown());
    }

    @Test
    void blankMessageIsNotPosted() {
        navigate(MUC08View.class);
        runPendingSignalsTasks();

        test(findInView(TextArea.class).single()).setValue("   ");
        test(button("Post announcement")).click();
        runPendingSignalsTasks();

        assertNull(muc08Signals.getAnnouncementSignal().peek());
        assertFalse(bannerShown());
    }

    @Test
    void announcementFromAnotherSessionAppearsImmediately() {
        try (var app = createApp()) {
            var alice = app.newUser("alice", "USER").newWindow();
            var admin = app.newUser("admin", "ADMIN").newWindow();
            alice.navigate(MUC08View.class);
            admin.navigate(MUC08View.class);

            post(admin, "Coffee in the lobby");

            assertEquals("Coffee in the lobby", alice.findInView(Span.class)
                    .withClassName("announcement-text").single().getText());
            assertTrue(alice.find(Notification.class).all().stream()
                    .anyMatch(n -> "Announcement: Coffee in the lobby"
                            .equals(alice.test(n).getText())));
        }
    }

    @Test
    void lateJoinerSeesCurrentAnnouncementWithoutToast() {
        try (var app = createApp()) {
            var admin = app.newUser("admin", "ADMIN").newWindow();
            admin.navigate(MUC08View.class);
            post(admin, "System update");

            // This session opens the view after the announcement was posted
            var alice = app.newUser("alice", "USER").newWindow();
            alice.navigate(MUC08View.class);
            alice.runPendingSignalsTasks();

            assertEquals("System update", alice.findInView(Span.class)
                    .withClassName("announcement-text").single().getText());
            assertTrue(alice.find(Notification.class).all().isEmpty(),
                    "Late joiner sees the banner, not a toast");
        }
    }

    @Test
    void clearingRemovesBannerForEveryone() {
        try (var app = createApp()) {
            var alice = app.newUser("alice", "USER").newWindow();
            var admin = app.newUser("admin", "ADMIN").newWindow();
            alice.navigate(MUC08View.class);
            admin.navigate(MUC08View.class);
            post(admin, "Fire drill");
            assertFalse(alice.findInView(Div.class)
                    .withClassName("announcement-banner").all().isEmpty());

            alice.test(alice.findInView(Button.class)
                    .withText("Clear announcement").single()).click();
            alice.runPendingSignalsTasks();

            assertNull(muc08Signals.getAnnouncementSignal().peek());
            assertTrue(admin.findInView(Div.class)
                    .withClassName("announcement-banner").all().isEmpty());
            assertFalse(admin.findInView(Button.class)
                    .withText("Clear announcement").single().isEnabled());
        }
    }

    private SecuredBrowserlessApplicationContext<Authentication> createApp() {
        return SpringBrowserlessApplicationContext.createSecured(
                applicationContext, b -> b.withViewPackages(MUC08View.class)
                        .withSessionLockIsolation(true));
    }

    private static void post(BrowserlessUIContext window, String text) {
        window.test(window.findInView(TextArea.class).single()).setValue(text);
        window.test(window.findInView(Button.class)
                .withText("Post announcement").single()).click();
        window.runPendingSignalsTasks();
    }
}
