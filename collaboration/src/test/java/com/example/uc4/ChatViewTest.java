package com.example.uc4;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { ChatView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ChatViewTest extends SpringBrowserlessTest {

    @Autowired
    ApplicationContext applicationContext;

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
    void usersWhoLeftHandTheirNamesBack() {
        // More users over time than there are names in the pool: as long as
        // the earlier ones have left, nobody needs a number after their name.
        for (int visit = 0; visit < 15; visit++) {
            navigate(HomeView.class);
            navigate(ChatView.class);
            runPendingSignalsTasks();
        }

        PeerRig rig = findInView(PeerRig.class).single();
        rig.getPanels()
                .forEach(panel -> assertFalse(
                        panel.getPeer().name().matches(".* \\d+"),
                        panel.getPeer().name()));
        assertNotEquals(rig.getPanel(0).getPeer().name(),
                rig.getPanel(1).getPeer().name());
    }

    @Test
    void aMessageFromAnotherSessionArrivesToo() {
        try (var app = SpringBrowserlessApplicationContext.create(
                applicationContext,
                b -> b.withViewPackages(ChatView.class, HomeView.class)
                        .withSessionLockIsolation(true))) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(ChatView.class);
            bob.navigate(ChatView.class);
            alice.runPendingSignalsTasks();

            PeerRig bobsRig = bob.findInView(PeerRig.class).single();
            bob.test(bob.find(MessageInput.class, bobsRig.getPanel(0)).single())
                    .send("Shipped this morning");
            bob.runPendingSignalsTasks();

            PeerRig alicesRig = alice.findInView(PeerRig.class).single();
            assertTrue(alice
                    .test(alice.find(MessageList.class, alicesRig.getPanel(0))
                            .single())
                    .getMessages().stream()
                    .anyMatch(item -> "Shipped this morning"
                            .equals(item.getText())),
                    "a message sent in another browser should arrive here");
        }
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
