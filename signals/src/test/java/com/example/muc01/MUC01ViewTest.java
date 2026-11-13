package com.example.muc01;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = MUC01View.class)
@WithMockUser
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MUC01ViewTest extends SpringBrowserlessTest {

    @Autowired
    private MUC01Signals muc01Signals;

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void viewRendersWithMessageInputAndButtons() {
        navigate(MUC01View.class);

        assertEquals(1, findInView(TextField.class).all().size());
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Send Message".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Clear All Messages".equals(b.getText())));
    }

    @Test
    void sendMessageAppearsInChat() {
        navigate(MUC01View.class);
        runPendingSignalsTasks();

        TextField messageInput = findInView(TextField.class).single();
        test(messageInput).setValue("Hello from User A");

        Button sendButton = findInView(Button.class).all().stream()
                .filter(b -> "Send Message".equals(b.getText())).findFirst()
                .orElseThrow();
        test(sendButton).click();
        runPendingSignalsTasks();

        // Message count should show 1
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains("Total messages: 1")));
    }

    @Test
    @WithAnonymousUser
    void anonymousVisitorCanChatAsGuest() {
        navigate(MUC01View.class);
        runPendingSignalsTasks();

        test(findInView(TextField.class).single()).setValue("Hello as guest");
        test(findInView(Button.class).all().stream()
                .filter(b -> "Send Message".equals(b.getText())).findFirst()
                .orElseThrow()).click();
        runPendingSignalsTasks();

        MUC01Signals.Message message = muc01Signals.getMessagesSignal().peek()
                .getFirst().peek();
        assertTrue(message.username().matches("guest-\\d+"),
                () -> "Unexpected guest username " + message.username());
    }

    @Test
    void otherUserMessageAppearsInView() {
        try (var app = SpringBrowserlessApplicationContext.createSecured(
                applicationContext, b -> b.withViewPackages(MUC01View.class)
                        .withSessionLockIsolation(true))) {
            var alice = app.newUser("alice", "USER").newWindow();
            var bob = app.newUser("bob", "USER").newWindow();
            alice.navigate(MUC01View.class);
            bob.navigate(MUC01View.class);

            sendMessage(bob, "Hello from Bob");
            bob.runPendingSignalsTasks();

            assertTrue(alice.findInView(Div.class).all().stream()
                    .anyMatch(d -> "Total messages: 1".equals(d.getText())));
            assertTrue(alice.findInView(Div.class).withClassName("message-text")
                    .all().stream()
                    .anyMatch(d -> "Hello from Bob".equals(d.getText())));
            assertTrue(alice.findInView(Div.class)
                    .withClassName("message-author").all().stream()
                    .anyMatch(d -> "bob".equals(d.getText())));
        }
    }

    @Test
    void clearAllRemovesMessagesFromAllUsers() {
        try (var app = SpringBrowserlessApplicationContext.createSecured(
                applicationContext, b -> b.withViewPackages(MUC01View.class)
                        .withSessionLockIsolation(true))) {
            var alice = app.newUser("alice", "USER").newWindow();
            var bob = app.newUser("bob", "USER").newWindow();
            alice.navigate(MUC01View.class);
            bob.navigate(MUC01View.class);

            sendMessage(alice, "Message 1");
            sendMessage(bob, "Message 2");
            bob.runPendingSignalsTasks();
            for (var window : List.of(alice, bob)) {
                assertTrue(window.findInView(Div.class).all().stream().anyMatch(
                        d -> "Total messages: 2".equals(d.getText())));
            }

            alice.test(alice.findInView(Button.class)
                    .withText("Clear All Messages").single()).click();
            alice.runPendingSignalsTasks();

            assertTrue(bob.findInView(Div.class).all().stream()
                    .anyMatch(d -> "Total messages: 0".equals(d.getText())));
            assertTrue(bob.findInView(Div.class).withClassName("message-row")
                    .all().isEmpty());
        }
    }

    private static void sendMessage(BrowserlessUIContext window, String text) {
        window.test(window.findInView(TextField.class).single()).setValue(text);
        window.test(window.findInView(Button.class).withText("Send Message")
                .single()).click();
    }
}
