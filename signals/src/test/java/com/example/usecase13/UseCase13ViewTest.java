package com.example.usecase13;

import com.example.signals.UserSessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.H3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase13View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class UseCase13ViewTest extends SpringBrowserlessTest {

    @Autowired
    private UserSessionRegistry userSessionRegistry;

    @Test
    void viewRendersWithUserCountDisplay() {
        navigate(UseCase13View.class);
        runPendingSignalsTasks();

        // The counter box should show "Currently Online" text
        assertTrue(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("Currently Online")));
    }

    @Test
    void registeringUsersCreatesCards() {
        navigate(UseCase13View.class);
        runPendingSignalsTasks();

        // The mock user's own session is already registered by the framework
        int initialCount = findInView(Card.class).all().size();

        // Register 2 additional fake users
        userSessionRegistry.registerUser("alice", "session-a");
        userSessionRegistry.registerUser("bob", "session-b");
        runPendingSignalsTasks();

        // Should have 2 more cards than initial
        assertEquals(initialCount + 2, findInView(Card.class).all().size());
    }

    @Test
    void userCountReflectsRegisteredUsers() {
        navigate(UseCase13View.class);
        runPendingSignalsTasks();

        int initialCount = userSessionRegistry.getActiveUserCount();

        userSessionRegistry.registerUser("alice", "session-a");
        userSessionRegistry.registerUser("bob", "session-b");
        runPendingSignalsTasks();

        int expectedCount = initialCount + 2;
        // Counter should show the expected count
        assertTrue(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null && h.getText()
                        .contains(String.valueOf(expectedCount))));
    }
}
