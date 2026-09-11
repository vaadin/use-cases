package com.example.uc5;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { CollaborativeFormView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CollaborativeFormViewTest extends SpringBrowserlessTest {

    @Autowired
    FormTopic topic;

    @Test
    void aValueTypedByOnePeerAppearsInTheOthersFields() {
        navigate(CollaborativeFormView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        name(rig, 0).setValue("Acme Supply Co.");
        runPendingSignalsTasks();

        assertEquals("Acme Supply Co.", name(rig, 1).getValue(),
                "the other peer's field should follow the shared value");
        assertEquals("Acme Supply Co.",
                topic.form().value(FormTopic.NAME).peek());
    }

    @Test
    void validityIsRecomputedFromTheSharedValueInEveryPanel() {
        navigate(CollaborativeFormView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        email(rig, 0).setValue("not-an-address");
        runPendingSignalsTasks();

        // Written by one peer, judged invalid by the other: the check is an
        // effect over the shared value, not a validator on one binding.
        assertTrue(email(rig, 1).isInvalid());

        email(rig, 0).setValue("clerk@example.com");
        runPendingSignalsTasks();
        assertTrue(!email(rig, 1).isInvalid());
    }

    @Test
    void aPeerThatDisappearsStopsEditing() {
        navigate(CollaborativeFormView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        var peer = rig.getPanel(0).getPeer();
        // Focus is a DOM event a browserless test cannot fire, so the field's
        // own entry point is used. What is under test is the exit: without the
        // detach listener this peer would highlight the field forever.
        topic.form().enter(FormTopic.NAME, peer);
        assertEquals(1, topic.form().editors(FormTopic.NAME).peek().size());

        rig.getPanel(0).setConnected(false);
        runPendingSignalsTasks();

        assertEquals(0, topic.form().editors(FormTopic.NAME).peek().size(),
                "a detached field must not leave its peer in the editor list");
    }

    private TextField name(PeerRig rig, int panel) {
        return find(TextField.class, rig.getPanel(panel)).withLabel("Name")
                .single();
    }

    private EmailField email(PeerRig rig, int panel) {
        return find(EmailField.class, rig.getPanel(panel)).single();
    }
}
