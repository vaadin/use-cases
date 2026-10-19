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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The race is staged rather than raced: both peers load, both peers edit, both
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
    void theSecondSaveIsRefusedUntilItsUserLoadsTheLatestNote() {
        navigate(ConflictsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        draft(rig, 0).setValue("Picked by the first user");
        draft(rig, 1).setValue("Picked by the second user");

        click(rig, 0, "Save");
        runPendingSignalsTasks();
        click(rig, 1, "Save");
        runPendingSignalsTasks();

        assertEquals("Picked by the first user", topic.note().peek());
        assertTrue(text(rig, 0).contains("Saved"));
        assertTrue(text(rig, 1).contains("Not saved"),
                "the second save was based on a value that had moved on");

        click(rig, 1, "Load latest");
        assertEquals("Picked by the first user", draft(rig, 1).getValue());
        draft(rig, 1)
                .setValue("Picked by the first user, packed by the second");
        click(rig, 1, "Save");
        runPendingSignalsTasks();

        assertEquals("Picked by the first user, packed by the second",
                topic.note().peek(), "an edit based on the latest note saves");
    }

    @Test
    void savingTwiceInARowIsNotAConflict() {
        navigate(ConflictsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        draft(rig, 0).setValue("First");
        click(rig, 0, "Save");
        runPendingSignalsTasks();
        draft(rig, 0).setValue("Second");
        click(rig, 0, "Save");
        runPendingSignalsTasks();

        // A successful save becomes the base of the next one, so a user is
        // never in conflict with themselves.
        assertEquals("Second", topic.note().peek());
        assertFalse(text(rig, 0).contains("Not saved"));
    }

    private String text(PeerRig rig, int panel) {
        return rig.getPanel(panel).getElement().getTextRecursively();
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
