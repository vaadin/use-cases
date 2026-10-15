package com.example.uc13;

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
 * The pending window is real but short: it lasts from the {@code set()} to the
 * moment the session flushes its signal commands. A browserless test can look
 * inside it, between the click and {@code runPendingSignalsTasks()}.
 */
@SpringBootTest
@ViewPackages(classes = { PendingStateView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PendingStateViewTest extends SpringBrowserlessTest {

    @Autowired
    PendingTopic topic;

    @Test
    void aWriteIsOnlyReportedAsSavedOnceTheSharedStateAcceptedIt() {
        navigate(PendingStateView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        draft(rig, 0).setValue("Aisle 9");
        save(rig, 0);

        assertTrue(text(rig, 0).contains("Saving…"),
                "submitted but not applied yet: " + text(rig, 0));
        assertEquals("Aisle 4", topic.label().peekConfirmed());

        runPendingSignalsTasks();
        assertEquals("Aisle 9", topic.label().peekConfirmed());
        assertTrue(text(rig, 0).contains("Saved: Aisle 9"),
                "the outcome only arrives after the flush");
        assertTrue(text(rig, 1).contains("Shelf label: Aisle 9"));
    }

    @Test
    void aWriteTheValidatorRefusesNeverReachesTheSharedState() {
        navigate(PendingStateView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        draft(rig, 0).setValue("a label far too long for this field");
        save(rig, 0);
        runPendingSignalsTasks();

        assertEquals("Aisle 4", topic.label().peek(),
                "a refused write changes nothing");
        assertTrue(text(rig, 0).contains("Not saved: a label can be at most"),
                text(rig, 0));
        assertTrue(text(rig, 1).contains("Shelf label: Aisle 4"),
                "the other peer sees no trace of it");
    }

    private String text(PeerRig rig, int panel) {
        return rig.getPanel(panel).getElement().getTextRecursively();
    }

    private TextField draft(PeerRig rig, int panel) {
        return find(TextField.class, rig.getPanel(panel)).withLabel("New label")
                .single();
    }

    private void save(PeerRig rig, int panel) {
        test(find(Button.class, rig.getPanel(panel)).withText("Save").single())
                .click();
    }
}
