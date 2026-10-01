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
 * moment the session flushes its signal commands. A browserless test is the
 * only place it can be observed after the fact, because the view records both
 * readings while it is open.
 */
@SpringBootTest
@ViewPackages(classes = { PendingStateView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PendingStateViewTest extends SpringBrowserlessTest {

    @Autowired
    PendingTopic topic;

    @Test
    void aSubmittedWriteIsVisibleToItsOwnClientBeforeItIsConfirmed() {
        navigate(PendingStateView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        draft(rig, 0).setValue("Aisle 9");
        write(rig, 0);

        String observed = rig.getPanel(0).getElement().getTextRecursively();
        assertTrue(
                observed.contains(
                        "submitted: get() Aisle 9 / peekConfirmed() Aisle 4"),
                "inside the handler the client has moved on and the shared "
                        + "state has not: " + observed);

        runPendingSignalsTasks();
        assertEquals("Aisle 9", topic.label().peekConfirmed());
        assertTrue(
                rig.getPanel(0).getElement().getTextRecursively()
                        .contains("outcome: accepted"),
                "and the outcome only arrives after the flush");
    }

    @Test
    void aWriteTheValidatorRefusesNeverReachesTheSharedState() {
        navigate(PendingStateView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        draft(rig, 0).setValue("a label far too long for this field");
        write(rig, 0);
        runPendingSignalsTasks();

        assertEquals("Aisle 4", topic.label().peek(),
                "a refused write changes nothing");
        assertTrue(
                rig.getPanel(0).getElement().getTextRecursively()
                        .contains("refused at submit time"),
                "and it is an exception rather than a rejected operation");
        assertTrue(
                rig.getPanel(1).getElement().getTextRecursively()
                        .contains("Shared label: Aisle 4"),
                "the other peer sees no trace of it");
    }

    private TextField draft(PeerRig rig, int panel) {
        return find(TextField.class, rig.getPanel(panel)).withLabel("New label")
                .single();
    }

    private void write(PeerRig rig, int panel) {
        test(find(Button.class, rig.getPanel(panel)).withText("Write").single())
                .click();
    }
}
