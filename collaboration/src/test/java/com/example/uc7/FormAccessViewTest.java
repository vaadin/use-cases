package com.example.uc7;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { FormAccessView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FormAccessViewTest extends SpringBrowserlessTest {

    @Autowired
    AccessTopic topic;

    @Test
    void theSecondClaimIsRejectedRatherThanQuietlyWinning() {
        navigate(FormAccessView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        claim(rig, 0);
        runPendingSignalsTasks();
        assertEquals(rig.getPanel(0).getPeer(), topic.holder().peek());

        claim(rig, 1);
        runPendingSignalsTasks();

        assertEquals(rig.getPanel(0).getPeer(), topic.holder().peek(),
                "a compare-and-set claim must not take a held lock");
        assertTrue(rig.getPanel(1).getElement().getTextRecursively()
                .contains("REJECTED"), "and the peer that lost has to be told");
    }

    @Test
    void onlyTheHolderCanWrite() {
        navigate(FormAccessView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        Peer holder = rig.getPanel(0).getPeer();
        Peer spectator = rig.getPanel(1).getPeer();
        claim(rig, 0);
        runPendingSignalsTasks();

        // Read-only in the UI, and refused in the shared state. The second
        // half is what a disabled field cannot give you.
        assertTrue(name(rig, 1).isReadOnly());
        assertTrue(!name(rig, 0).isReadOnly());

        // A validator does not reject the write, it refuses to submit it:
        // the call throws instead of returning a failed operation.
        assertThrows(UnsupportedOperationException.class,
                () -> topic.formFor(spectator).write(AccessTopic.NAME,
                        "from a spectator", spectator));
        assertEquals("", topic.form().value(AccessTopic.NAME).peek(),
                "and nothing reaches the shared state");

        topic.formFor(holder).write(AccessTopic.NAME, "from the holder",
                holder);
        assertEquals("from the holder",
                topic.form().value(AccessTopic.NAME).peek());
    }

    @Test
    void releasingHandsTheLockOn() {
        navigate(FormAccessView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        claim(rig, 0);
        runPendingSignalsTasks();

        test(find(Button.class, rig.getPanel(0)).withText("Release").single())
                .click();
        runPendingSignalsTasks();
        assertEquals(Peer.NOBODY, topic.holder().peek());

        claim(rig, 1);
        runPendingSignalsTasks();
        assertEquals(rig.getPanel(1).getPeer(), topic.holder().peek());
    }

    private void claim(PeerRig rig, int panel) {
        test(find(Button.class, rig.getPanel(panel)).withText("Claim editing")
                .single()).click();
    }

    private TextField name(PeerRig rig, int panel) {
        return find(TextField.class, rig.getPanel(panel)).withLabel("Name")
                .single();
    }
}
