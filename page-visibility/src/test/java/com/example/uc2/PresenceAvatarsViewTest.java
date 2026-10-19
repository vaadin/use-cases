package com.example.uc2;

import com.example.PageVisibilityTestSupport;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.page.PageVisibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { PresenceAvatarsView.class, HomeView.class })
class PresenceAvatarsViewTest extends SpringBrowserlessTest {

    @Autowired
    private PresenceRegistry registry;

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
     * A second browser session navigates after the first. The shared
     * {@link PresenceRegistry} bean must accept the second navigation without
     * raising an {@code IllegalStateException}.
     * <p>
     * Reproduces a regression where the registry held a session-local
     * {@code ListSignal}: the first navigation bound the signal to session A,
     * and the second navigation tripped
     * {@code IllegalStateException: This ListSignal instance was created in
     * one VaadinSession but is being accessed from another}.
     */
    @Test
    void secondSessionCanNavigateWithoutCrossSessionException() {
        long before = registry.size();
        navigate(PresenceAvatarsView.class);
        runPendingSignalsTasks();
        assertEquals(before + 1, registry.size());

        // Discard the current mock VaadinSession and start a fresh one to
        // simulate a second browser tab in another session. The first
        // session's detach removes its presence, so only the new visitor
        // is left.
        cleanVaadinEnvironment();
        initVaadinEnvironment();

        navigate(PresenceAvatarsView.class);
        runPendingSignalsTasks();

        assertEquals(before + 1, registry.size(),
                "second session should have replaced the first presence");
    }
}
