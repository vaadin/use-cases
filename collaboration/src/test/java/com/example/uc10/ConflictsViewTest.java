package com.example.uc10;

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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The race is staged rather than raced: both peers read, both peers edit, both
 * peers save. That is the sequence a real conflict produces, and it is the
 * sequence a test can reproduce exactly.
 */
@SpringBootTest
@ViewPackages(classes = { ConflictsView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ConflictsViewTest extends SpringBrowserlessTest {

    @Autowired
    RaceTopic topic;

    @Test
    void theSecondCompareAndSetIsRejectedAndTheValueIsTheFirstOnes() {
        navigate(ConflictsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        click(rig, 0, "Read");
        click(rig, 1, "Read");
        draft(rig, 0).setValue("Picked by the first user");
        draft(rig, 1).setValue("Picked by the second user");

        click(rig, 0, "Save (replace)");
        runPendingSignalsTasks();
        click(rig, 1, "Save (replace)");
        runPendingSignalsTasks();

        assertEquals("Picked by the first user", topic.note().peek());
        assertTrue(
                rig.getPanel(1).getElement().getTextRecursively()
                        .contains("replace: REJECTED"),
                "the second save was based on a value that had moved on");
        assertTrue(rig.getPanel(0).getElement().getTextRecursively()
                .contains("replace: accepted"));
    }

    @Test
    void aBlindSaveWinsAndSaysNothing() {
        navigate(ConflictsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        click(rig, 0, "Read");
        click(rig, 1, "Read");
        draft(rig, 0).setValue("First");
        draft(rig, 1).setValue("Second");

        click(rig, 0, "Save (set)");
        runPendingSignalsTasks();
        click(rig, 1, "Save (set)");
        runPendingSignalsTasks();

        // The behaviour of a last-write-wins map, made explicit: the first
        // user's edit is gone and both operations were accepted.
        assertEquals("Second", topic.note().peek());
        assertTrue(rig.getPanel(0).getElement().getTextRecursively()
                .contains("set: accepted"));
    }

    @Test
    void concurrentIncrementsAreNeverLost() {
        navigate(ConflictsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        click(rig, 0, "+1");
        click(rig, 1, "+1");
        runPendingSignalsTasks();

        // peek(), not getAsInt(): the convenient int accessor goes through
        // get() and so needs a reactive context, which a test thread is not.
        assertEquals(2, topic.tally().peek().intValue(),
                "incrementBy sends a delta, so neither can overwrite the other");
    }

    @Test
    void movingAnItemBetweenListsIsAllOrNothing() {
        navigate(ConflictsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        int backlog = topic.backlog().peek().size();

        click(rig, 0, "Move first backlog item (transaction)");
        runPendingSignalsTasks();
        click(rig, 1, "Move first backlog item (transaction)");
        runPendingSignalsTasks();

        assertEquals(backlog - 2, topic.backlog().peek().size());
        assertEquals(2, topic.done().peek().size(),
                "two moves, two items — no duplicates and none dropped");
    }

    private void click(PeerRig rig, int panel, String caption) {
        test(find(Button.class, rig.getPanel(panel)).withText(caption).single())
                .click();
    }

    private TextField draft(PeerRig rig, int panel) {
        return find(TextField.class, rig.getPanel(panel)).withLabel("Your edit")
                .single();
    }
}
