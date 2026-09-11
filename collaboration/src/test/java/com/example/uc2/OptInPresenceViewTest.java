package com.example.uc2;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.checkbox.Checkbox;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The distinction under test is the one the use case is about: being connected
 * is not being present. Two peers are attached from the start and neither is
 * listed until it says so.
 */
@SpringBootTest
@ViewPackages(classes = { OptInPresenceView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class OptInPresenceViewTest extends SpringBrowserlessTest {

    @Test
    void attachedPeersAreNotPresentUntilTheyOptIn() {
        navigate(OptInPresenceView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        assertEquals(0, avatars(rig, 0),
                "an attached peer should not be listed yet");

        find(Checkbox.class, rig.getPanel(0)).single().setValue(true);
        runPendingSignalsTasks();

        assertEquals(1, avatars(rig, 1),
                "the other peer should see the one that joined");

        find(Checkbox.class, rig.getPanel(0)).single().setValue(false);
        runPendingSignalsTasks();

        assertEquals(0, avatars(rig, 1), "leaving should be visible too");
    }

    @Test
    void disconnectingRemovesAPeerThatHadOptedIn() {
        navigate(OptInPresenceView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        find(Checkbox.class, rig.getPanel(0)).single().setValue(true);
        find(Checkbox.class, rig.getPanel(1)).single().setValue(true);
        runPendingSignalsTasks();
        assertEquals(2, avatars(rig, 1));

        // Opting in is a decision; disappearing is not. Without the detach
        // listener this peer would stay in the map for good.
        rig.getPanel(0).setConnected(false);
        runPendingSignalsTasks();
        assertEquals(1, avatars(rig, 1));
    }

    private int avatars(PeerRig rig, int panel) {
        return find(AvatarGroup.class, rig.getPanel(panel)).single().getItems()
                .size();
    }
}
