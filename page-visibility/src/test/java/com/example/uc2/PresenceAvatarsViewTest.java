package com.example.uc2;

import java.util.Optional;

import com.example.PageVisibilityTestSupport;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.page.PageVisibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { PresenceAvatarsView.class, HomeView.class })
class PresenceAvatarsViewTest extends SpringBrowserlessTest {

    @Autowired
    private PresenceRegistry registry;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void openingTheViewJoinsAndLeavingItLeaves() {
        long before = registry.size();

        navigate(PresenceAvatarsView.class);
        runPendingSignalsTasks();
        assertEquals(before + 1, registry.size(),
                "the visiting user should join the room");

        navigate(HomeView.class);
        assertEquals(before, registry.size(),
                "the visiting user should leave the room with the view");
    }

    @Test
    void avatarReflectsVisibilityStateClasses() {
        navigate(PresenceAvatarsView.class);
        runPendingSignalsTasks();

        PageVisibilityTestSupport.setPageVisibility(PageVisibility.VISIBLE);
        runPendingSignalsTasks();

        ColoredAvatar avatar = findInView(ColoredAvatar.class).all().stream()
                .findFirst()
                .orElseThrow(() -> new AssertionError("no avatar rendered"));
        assertTrue(
                avatar.getElement().getClassList()
                        .contains("uc2-avatar-visible"),
                "avatar should carry the uc2-avatar-visible class while VISIBLE");

        PageVisibilityTestSupport.setPageVisibility(PageVisibility.HIDDEN);
        runPendingSignalsTasks();

        assertTrue(
                avatar.getElement().getClassList()
                        .contains("uc2-avatar-hidden"),
                "avatar should carry the uc2-avatar-hidden class while HIDDEN");
        assertFalse(
                avatar.getElement().getClassList()
                        .contains("uc2-avatar-visible"),
                "uc2-avatar-visible should be cleared when state flips to HIDDEN");
    }

    /**
     * Two users in the room see each other, and a tab that another user hides
     * greys out their avatar here.
     * <p>
     * Also guards against a regression where the registry held a session-local
     * {@code ListSignal}: the second session's navigation tripped
     * {@code IllegalStateException: This ListSignal instance was
     * created in one VaadinSession but is being accessed from another}.
     */
    @Test
    void anotherUsersPresenceAndVisibilityShowInTheRoom() {
        try (var app = SpringBrowserlessApplicationContext
                .create(applicationContext,
                        b -> b.withViewPackages(PresenceAvatarsView.class,
                                HomeView.class)
                                .withSessionLockIsolation(true))) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(PresenceAvatarsView.class);
            bob.navigate(PresenceAvatarsView.class);
            String bobName = bob.find(Span.class).withClassName("uc2-you-name")
                    .single().getText();

            bob.activate();
            PageVisibilityTestSupport.setPageVisibility(PageVisibility.VISIBLE);
            runAllPendingSignalsTasks(bob);
            assertTrue(avatarOf(alice, bobName).isPresent(),
                    "the other user should be in the room");
            assertTrue(avatarOf(alice, bobName).orElseThrow()
                    .hasClassName("uc2-avatar-visible"));

            bob.activate();
            PageVisibilityTestSupport.setPageVisibility(PageVisibility.HIDDEN);
            runAllPendingSignalsTasks(bob);
            assertTrue(
                    avatarOf(alice, bobName).orElseThrow()
                            .hasClassName("uc2-avatar-hidden"),
                    "the other user's hidden tab should grey out the avatar");

            bob.navigate(HomeView.class);
            runAllPendingSignalsTasks(bob);
            assertTrue(avatarOf(alice, bobName).isEmpty(),
                    "the other user should leave the room with the view");
        }
    }

    private static Optional<ColoredAvatar> avatarOf(BrowserlessUIContext window,
            String name) {
        return window.find(ColoredAvatar.class).all().stream()
                .filter(avatar -> avatar.getElement().getAttribute("title")
                        .startsWith(name + " — "))
                .findFirst();
    }

    private static void runAllPendingSignalsTasks(BrowserlessUIContext window) {
        while (window.runPendingSignalsTasks()) {
            // until nothing is pending
        }
    }
}
