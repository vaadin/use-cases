package com.example.muc08;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

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
        navigate(MUC08View.class);
        runPendingSignalsTasks();

        // Simulate an admin in another session posting via the shared bean
        muc08Signals.post(
                new MUC08Signals.Announcement("admin", "Coffee in the lobby"));
        runPendingSignalsTasks();

        assertTrue(bannerShown());
        assertEquals("Coffee in the lobby", bannerText().getText());
        assertTrue(find(Notification.class).all().stream()
                .anyMatch(n -> "Announcement: Coffee in the lobby"
                        .equals(test(n).getText())));
    }

    @Test
    void lateJoinerSeesCurrentAnnouncementWithoutToast() {
        // Another session posted before this session opened the view
        muc08Signals
                .post(new MUC08Signals.Announcement("admin", "System update"));

        navigate(MUC08View.class);
        runPendingSignalsTasks();

        assertTrue(bannerShown());
        assertEquals("System update", bannerText().getText());
        assertTrue(find(Notification.class).all().isEmpty(),
                "Late joiner sees the banner, not a toast");
    }

    @Test
    void clearingRemovesBannerForEveryone() {
        muc08Signals.post(new MUC08Signals.Announcement("admin", "Fire drill"));

        navigate(MUC08View.class);
        runPendingSignalsTasks();
        assertTrue(bannerShown());

        test(button("Clear announcement")).click();
        runPendingSignalsTasks();

        assertNull(muc08Signals.getAnnouncementSignal().peek());
        assertFalse(bannerShown());
        assertFalse(button("Clear announcement").isEnabled());
    }
}
