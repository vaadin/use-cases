package com.example.uc1;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.Presence;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Presence is application-scoped state with lifecycle hooks on both ends, so
 * the tests are about the ends: a peer that arrives, a peer that leaves, and a
 * peer that was never in this session at all.
 */
@SpringBootTest
@ViewPackages(classes = { PresenceView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PresenceViewTest extends SpringBrowserlessTest {

    @Autowired
    Presence presence;

    @Test
    void everyPeerSeesEveryOtherPeer() {
        navigate(PresenceView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        assertEquals(2, rig.getPanels().size());
        for (var panel : rig.getPanels()) {
            assertEquals(2,
                    find(AvatarGroup.class, panel).single().getItems().size(),
                    "each peer's avatar group should show both peers");
        }
    }

    @Test
    void addingAndDisconnectingPeersUpdatesEveryone() {
        navigate(PresenceView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        test(findInView(Button.class).withText("Add user").single()).click();
        runPendingSignalsTasks();
        assertEquals(3, find(AvatarGroup.class, rig.getPanel(0)).single()
                .getItems().size(), "the added peer should join the topic");

        // Disconnecting detaches that peer's content, which is what leaves.
        rig.getPanel(2).setConnected(false);
        runPendingSignalsTasks();
        assertEquals(2, find(AvatarGroup.class, rig.getPanel(0)).single()
                .getItems().size(),
                "a disconnected peer should leave the topic");
    }

    @Test
    void presenceFromAnotherSessionShowsUp() {
        // The bean outlives any session, so a peer written from outside this
        // one stands in for a second browser. If the view used a plain
        // ListSignal or ValueSignal instead of a shared one, this is the test
        // that would fail.
        Peer stranger = new Peer(999, "Someone Else", 5);
        presence.topic(PresenceView.TOPIC).put(stranger.key(), stranger);

        navigate(PresenceView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        assertEquals(3,
                find(AvatarGroup.class, rig.getPanel(0)).single().getItems()
                        .size(),
                "the two local peers and the one from the other session");
    }
}
