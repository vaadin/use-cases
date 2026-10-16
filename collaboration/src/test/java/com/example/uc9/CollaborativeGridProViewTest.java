package com.example.uc9;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import com.example.uc9.CatalogTopic.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.gridpro.GridPro;
import com.vaadin.flow.component.gridpro.GridProTester;
import com.vaadin.flow.signals.shared.SharedValueSignal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { CollaborativeGridProView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CollaborativeGridProViewTest extends SpringBrowserlessTest {

    @Autowired
    CatalogTopic topic;

    @Test
    void aCellEditedByOnePeerAppearsInTheOthers() {
        navigate(CollaborativeGridProView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        tester(rig, 0).setValue(0, 2, "0.15");
        runPendingSignalsTasks();

        assertEquals("0.15", tester(rig, 1).getCellText(0, 2));
        assertEquals("0.15", topic.rows().peek().getFirst().peek().price());
    }

    @Test
    void twoPeersEditingDifferentColumnsOfOneRowBothKeepTheirChange() {
        navigate(CollaborativeGridProView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        SharedValueSignal<Product> row = topic.rows().peek().getFirst();

        // The other peer's edit, applied as a read-modify-write like the view
        // does. Writing a whole row built from a stale read — set() with a
        // withX() of an old value — is what would lose one of the two.
        row.update(product -> product.withCategory("Bolts"));
        tester(rig, 0).setValue(0, 0, "Hex bolt M10");
        runPendingSignalsTasks();

        Product result = row.peek();
        assertEquals("Hex bolt M10", result.name());
        assertEquals("Bolts", result.category(),
                "the concurrent edit to another column must survive");
    }

    @Test
    void startingToEditMarksTheRowForTheOtherPeers() {
        navigate(CollaborativeGridProView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        var peer = rig.getPanel(0).getPeer();

        // GridPro reports the item but not the column, so the mark is a row
        // and this is as precise as the view can be.
        topic.editing().put(peer.key(), "p1");
        runPendingSignalsTasks();
        assertTrue(rig.getPanel(1).getElement().getTextRecursively()
                .contains("1 other user(s) editing a row"));

        rig.getPanel(0).setConnected(false);
        runPendingSignalsTasks();
        assertEquals(0, topic.editing().peek().size(),
                "a peer that left is not editing anything");
    }

    private GridProTester<GridPro<SharedValueSignal<Product>>, SharedValueSignal<Product>> tester(
            PeerRig rig, int panel) {
        @SuppressWarnings("unchecked")
        GridPro<SharedValueSignal<Product>> grid = find(GridPro.class,
                rig.getPanel(panel)).single();
        return test(GridProTester.class, grid);
    }
}
