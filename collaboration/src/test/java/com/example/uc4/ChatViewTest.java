package com.example.uc4;

import java.time.Instant;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import com.example.uc4.ChatTopic.Message;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { ChatView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChatViewTest extends SpringBrowserlessTest {

    @Autowired
    ChatTopic chat;

    @Test
    void aMessageSentByOnePeerArrivesAtTheOthers() {
        navigate(ChatView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        int before = messages(rig, 1);

        test(find(MessageInput.class, rig.getPanel(0)).single())
                .send("Where is order 1041?");
        runPendingSignalsTasks();

        assertEquals(before + 1, messages(rig, 1));
        assertTrue(
                test(find(MessageList.class, rig.getPanel(1)).single())
                        .getMessages().stream()
                        .anyMatch(item -> "Where is order 1041?"
                                .equals(item.getText())),
                "the other peer should see the message text");
    }

    @Test
    void aMessageFromAnotherSessionArrivesToo() {
        // The shared list is application-scoped, so an append from outside any
        // session stands in for a second browser.
        chat.send(new Message(42, "Someone Else", 3, "Shipped this morning",
                Instant.now()));

        navigate(ChatView.class);
        runPendingSignalsTasks();

        assertTrue(test(find(MessageList.class,
                findInView(PeerRig.class).single().getPanel(0)).single())
                .getMessages().stream().anyMatch(
                        item -> "Shipped this morning".equals(item.getText())));
    }

    @Test
    void restartingTheServerRebuildsTheListFromTheStore() {
        navigate(ChatView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        test(find(MessageInput.class, rig.getPanel(0)).single())
                .send("Please confirm");
        runPendingSignalsTasks();
        int live = messages(rig, 0);

        // The signal is not durable; the store next to it is. This is the
        // hand-written half of what a persister would have done.
        test(findInView(Button.class).withText("Restart the server").single())
                .click();
        runPendingSignalsTasks();

        assertEquals(live, messages(rig, 0),
                "every message should come back from the store");
        assertTrue(test(find(MessageList.class, rig.getPanel(0)).single())
                .getMessages().stream()
                .anyMatch(item -> "Please confirm".equals(item.getText())));
    }

    private int messages(PeerRig rig, int panel) {
        return test(find(MessageList.class, rig.getPanel(panel)).single())
                .size();
    }
}
