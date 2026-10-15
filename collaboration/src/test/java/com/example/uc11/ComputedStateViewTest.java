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

        assertTrue(text(rig, 1).contains(claimer.name() + " 1"),
                "the other panel computes who claimed what from the same list: "
                        + text(rig, 1));
        assertTrue(text(rig, 1).contains("Your tasks: 0"),
                "and 'your tasks' is per peer, computed from the same list again");
        assertTrue(text(rig, 0).contains("Your tasks: 1"));
    }

    @Test
    void onlyTheClaimerCanMoveATaskOn() {
        navigate(ComputedStateView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        assertTrue(text(rig, 1).contains("To do 3 · in progress 1 · done 1"));
        assertTrue(text(rig, 1).contains("Complete: 20%"));

        test(find(Button.class, rig.getPanel(0)).withText("Claim").all()
                .getFirst()).click();
        runPendingSignalsTasks();
        assertTrue(visibleButtons(rig, 1, "Start").isEmpty(),
                "a task somebody else claimed cannot be started from here");

        test(visibleButtons(rig, 0, "Start").getFirst()).click();
        runPendingSignalsTasks();

        assertTrue(text(rig, 1).contains("To do 2 · in progress 2 · done 1"),
                "counts follow the shared list: " + text(rig, 1));
        assertTrue(text(rig, 1).contains("Complete: 20%"),
                "and the completion share does not move for to do → in progress");

        test(visibleButtons(rig, 0, "Finish").getFirst()).click();
        runPendingSignalsTasks();
        assertTrue(text(rig, 1).contains("Complete: 40%"));
    }

    private java.util.List<Button> visibleButtons(PeerRig rig, int panel,
            String caption) {
        return find(Button.class, rig.getPanel(panel)).withText(caption).all()
                .stream().filter(Button::isVisible).toList();
    }

    private String text(PeerRig rig, int panel) {
        return rig.getPanel(panel).getElement().getTextRecursively();
    }
}
