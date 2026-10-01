package com.example.uc6;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The log is the use case, so the assertions are about what a log line can
 * contain: old and new value, which the binding provides, and the author, which
 * only the stored value knows.
 */
@SpringBootTest
@ViewPackages(classes = { FormEventsView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class FormEventsViewTest extends SpringBrowserlessTest {

    @Autowired
    FormEventsTopic topic;

    @Test
    void aChangeIsLoggedWithBothValuesAndItsAuthor() {
        navigate(FormEventsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        var author = rig.getPanel(0).getPeer();

        find(TextField.class, rig.getPanel(0)).withLabel("Name").single()
                .setValue("Ada");
        runPendingSignalsTasks();

        String observed = rig.getPanel(1).getElement().getTextRecursively();
        assertTrue(observed.contains("\"\" → \"Ada\""),
                "the binding reports the previous value as well as the new one: "
                        + observed);
        assertTrue(observed.contains("(by " + author.name() + ")"),
                "the author comes from the stored value, since the binding "
                        + "does not know who wrote it");
    }

    @Test
    void editorArrivalsAndDeparturesAreLogged() {
        navigate(FormEventsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        var peer = rig.getPanel(0).getPeer();

        topic.form().enter(FormEventsTopic.NAME, peer);
        runPendingSignalsTasks();
        assertTrue(rig.getPanel(1).getElement().getTextRecursively()
                .contains(peer.name() + " started editing name"));

        topic.form().leave(FormEventsTopic.NAME, peer);
        runPendingSignalsTasks();
        assertTrue(
                rig.getPanel(1).getElement().getTextRecursively()
                        .contains(peer.name() + " stopped editing name"),
                "there is no leave event, so this line is a diff");
    }
}
