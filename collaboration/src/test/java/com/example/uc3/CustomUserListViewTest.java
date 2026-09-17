package com.example.uc3;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two things are worth pinning down here: that a rename is an update of the
 * peer's own entry rather than a new one, and that the hand-written join/leave
 * log actually reports what an effect cannot.
 */
@SpringBootTest
@ViewPackages(classes = { CustomUserListView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CustomUserListViewTest extends SpringBrowserlessTest {

    @Test
    void renamingUpdatesTheEntryEverywhereWithoutAddingOne() {
        navigate(CustomUserListView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        int before = rows(rig, 1);

        find(TextField.class, rig.getPanel(0)).single()
                .setValue("Renamed Peer");
        runPendingSignalsTasks();

        assertEquals(before, rows(rig, 1),
                "a rename is a put on the same key, not a new peer");
        assertTrue(text(rig, 1).contains("Renamed Peer"),
                "the other peer's list should show the new name");
    }

    @Test
    void theLogReportsJoiningLeavingAndRenaming() {
        navigate(CustomUserListView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        // Panel 0 was rendered before panel 1 existed, so it saw it arrive.
        assertTrue(text(rig, 0).contains("joined"),
                "the log should report the second peer joining");

        find(TextField.class, rig.getPanel(0)).single().setValue("Renamed");
        runPendingSignalsTasks();
        assertTrue(text(rig, 1).contains("is now Renamed"),
                "a rename should be reported as a rename, not a join");

        rig.getPanel(0).setConnected(false);
        runPendingSignalsTasks();
        assertTrue(text(rig, 1).contains("left"),
                "leaving is a diff against the previous snapshot");
    }

    private int rows(PeerRig rig, int panel) {
        return find(Div.class, rig.getPanel(panel)).withClassName("user-list")
                .single().getChildren().toList().size();
    }

    private String text(PeerRig rig, int panel) {
        return rig.getPanel(panel).getElement().getTextRecursively();
    }
}
