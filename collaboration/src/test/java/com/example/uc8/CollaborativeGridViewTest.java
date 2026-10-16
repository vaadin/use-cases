package com.example.uc8;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import com.example.uc8.RosterTopic.Employee;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { CollaborativeGridView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CollaborativeGridViewTest extends SpringBrowserlessTest {

    @Autowired
    RosterTopic topic;

    @Test
    void rowsAddedAndRemovedByOnePeerReachTheOthers() {
        navigate(CollaborativeGridView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        int before = rows(rig, 1);

        test(findInView(Button.class).withText("Add row").all().get(0)).click();
        runPendingSignalsTasks();
        assertEquals(before + 1, rows(rig, 1));

        // Removing from the shared list, from outside any session: a row
        // added in one place is a row everywhere.
        topic.rows().remove(topic.rows().peek().getLast());
        runPendingSignalsTasks();
        assertEquals(before, rows(rig, 1));
    }

    @Test
    void selectingARowTellsTheOtherPeersWhereYouAre() {
        navigate(CollaborativeGridView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        test(grid(rig, 0)).select(1);
        runPendingSignalsTasks();

        assertEquals("e2", topic.watching().peek()
                .get(rig.getPanel(0).getPeer().key()).peek());
        assertTrue(rig.getPanel(1).getElement().getTextRecursively()
                .contains("1 other user(s) have a row selected"));
        assertTrue(
                rig.getPanel(0).getElement().getTextRecursively()
                        .contains("0 other user(s) have a row selected"),
                "your own selection is not somebody else's");
    }

    @Test
    void aDisconnectedPeerStopsWatching() {
        navigate(CollaborativeGridView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        test(grid(rig, 0)).select(0);
        runPendingSignalsTasks();

        rig.getPanel(0).setConnected(false);
        runPendingSignalsTasks();

        assertEquals(0, topic.watching().peek().size(),
                "a peer that left cannot still be looking at a row");
    }

    @Test
    void anEmployeeAddedFromAnotherSessionShowsUp() {
        topic.rows().insertLast(
                new Employee("x1", "Someone", "Else", "someone@example.com"));

        navigate(CollaborativeGridView.class);
        runPendingSignalsTasks();

        assertTrue(test(grid(findInView(PeerRig.class).single(), 0))
                .getCellText(rows(findInView(PeerRig.class).single(), 0) - 1, 0)
                .equals("Someone"));
    }

    private Grid<SharedValueSignal<Employee>> grid(PeerRig rig, int panel) {
        @SuppressWarnings("unchecked")
        Grid<SharedValueSignal<Employee>> grid = find(Grid.class,
                rig.getPanel(panel)).single();
        return grid;
    }

    private int rows(PeerRig rig, int panel) {
        return test(grid(rig, panel)).size();
    }
}
