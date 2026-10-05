package com.example.muc01;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;

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
        assertTrue(message.username().matches("guest-\\d{4}"),
                () -> "Unexpected guest username " + message.username());
    }

    @Test
    void otherUserMessageAppearsInView() {
        navigate(MUC01View.class);
        runPendingSignalsTasks();

        // Simulate User B sending a message via the shared signal
        muc01Signals.appendMessage(new MUC01Signals.Message("userB", "User B",
                "Hello from User B"));
        runPendingSignalsTasks();

        // User A's view should show the message
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains("Total messages: 1")));
        // The message text should be rendered
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> "Hello from User B".equals(d.getText())));
        // The author name should be rendered
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> "User B".equals(d.getText())));
    }

    @Test
    void multipleUsersMessagesAccumulate() {
        navigate(MUC01View.class);
        runPendingSignalsTasks();

        // User A sends a message
        TextField messageInput = findInView(TextField.class).single();
        test(messageInput).setValue("Hello from A");
        Button sendButton = findInView(Button.class).all().stream()
                .filter(b -> "Send Message".equals(b.getText())).findFirst()
                .orElseThrow();
        test(sendButton).click();
        runPendingSignalsTasks();

        // User B sends a message via shared signal
        muc01Signals.appendMessage(
                new MUC01Signals.Message("userB", "User B", "Hello from B"));
        runPendingSignalsTasks();

        // Both messages should be counted
        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains("Total messages: 2")));
    }

    @Test
    void clearAllRemovesMessagesFromAllUsers() {
        navigate(MUC01View.class);
        runPendingSignalsTasks();

        // Add messages from two different users
        muc01Signals.appendMessage(
                new MUC01Signals.Message("userA", "User A", "Message 1"));
        muc01Signals.appendMessage(
                new MUC01Signals.Message("userB", "User B", "Message 2"));
        runPendingSignalsTasks();

        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains("Total messages: 2")));

        // Clear all
        Button clearButton = findInView(Button.class).all().stream()
                .filter(b -> "Clear All Messages".equals(b.getText()))
                .findFirst().orElseThrow();
        test(clearButton).click();
        runPendingSignalsTasks();

        assertTrue(findInView(Div.class).all().stream()
                .anyMatch(d -> d.getText() != null
                        && d.getText().contains("Total messages: 0")));
    }
}
