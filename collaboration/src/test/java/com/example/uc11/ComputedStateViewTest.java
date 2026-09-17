package com.example.uc11;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every readout in this view is derived, so the test is that a change made in
 * one panel reaches the <em>computed</em> values in the other one — with
 * nothing in between to keep them in step.
 */
@SpringBootTest
@ViewPackages(classes = { ComputedStateView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ComputedStateViewTest extends SpringBrowserlessTest {

    @Autowired
    BoardTopic topic;

    @Test
    void claimingATaskUpdatesTheDerivedWorkloadEverywhere() {
        navigate(ComputedStateView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        var claimer = rig.getPanel(0).getPeer();

        test(find(Button.class, rig.getPanel(0)).withText("Claim").all()
                .getFirst()).click();
        runPendingSignalsTasks();

        assertTrue(text(rig, 1).contains("Workload: " + claimer.name() + " 1"),
                "the other panel computes the workload from the same list");
        assertTrue(text(rig, 1).contains("Yours: 0"),
                "and 'yours' is per peer, computed from the same list again");
        assertTrue(text(rig, 0).contains("Yours: 1"));
    }

    @Test
    void advancingATaskMovesTheStatusCountsAndTheCompletionShare() {
        navigate(ComputedStateView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        assertTrue(text(rig, 1).contains("To do 3 · doing 1 · done 1"));
        assertTrue(text(rig, 1).contains("Complete: 20%"));

        // Advance a to-do task to doing in one panel.
        test(find(Button.class, rig.getPanel(0)).withText("Advance").all()
                .getFirst()).click();
        runPendingSignalsTasks();

        assertTrue(text(rig, 1).contains("To do 2 · doing 2 · done 1"),
                "counts follow the shared list: " + text(rig, 1));
        assertTrue(text(rig, 1).contains("Complete: 20%"),
                "and the completion share does not move for a to-do → doing");
    }

    private String text(PeerRig rig, int panel) {
        return rig.getPanel(panel).getElement().getTextRecursively();
    }
}
