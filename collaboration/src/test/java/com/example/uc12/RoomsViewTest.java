package com.example.uc12;

import com.example.collab.PeerRig;
import com.example.home.HomeView;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { RoomsView.class, HomeView.class })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class RoomsViewTest extends SpringBrowserlessTest {

    @Autowired
    RoomsTopic topic;

    @Test
    void aRoomCreatedByOnePeerAppearsForTheOthers() {
        navigate(RoomsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        find(TextField.class, rig.getPanel(0)).single().setValue("Shipping");
        test(find(Button.class, rig.getPanel(0)).withText("Create").single())
                .click();
        runPendingSignalsTasks();

        // The room list is shared state, not a parameter: the other peer can
        // render a room it never asked for.
        assertTrue(
                rig.getPanel(1).getElement().getTextRecursively()
                        .contains("shipping"),
                rig.getPanel(1).getElement().getTextRecursively());
    }

    @Test
    void messagesAndPresenceBelongToOneRoomOnly() {
        navigate(RoomsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        test(find(MessageInput.class, rig.getPanel(0)).single())
                .send("Anyone in general?");
        runPendingSignalsTasks();

        assertEquals(1, topic.messages(RoomsTopic.DEFAULT_ROOM).peek().size());
        assertEquals(0, topic.messages("returns").peek().size());
        assertEquals(1,
                test(find(MessageList.class, rig.getPanel(1)).single()).size(),
                "both peers start in the default room");

        // Switching rooms leaves the first one and shows the second one's
        // messages, which are none.
        test(find(Button.class, rig.getPanel(1)).withTextContaining("returns")
                .single()).click();
        runPendingSignalsTasks();

        assertEquals(0,
                test(find(MessageList.class, rig.getPanel(1)).single()).size());
        assertFalse(
                topic.presence(RoomsTopic.DEFAULT_ROOM).peek()
                        .containsKey(rig.getPanel(1).getPeer().key()),
                "a peer that moved rooms is no longer present in the old one");
        assertTrue(topic.presence("returns").peek()
                .containsKey(rig.getPanel(1).getPeer().key()));
    }

    @Test
    void emptyRoomsAreOnlyClosedWhenAsked() {
        navigate(RoomsView.class);
        runPendingSignalsTasks();

        PeerRig rig = findInView(PeerRig.class).single();
        test(find(MessageInput.class, rig.getPanel(0)).single())
                .send("Keeping this room busy");
        topic.room("empty-one");
        runPendingSignalsTasks();

        // Nothing expires by itself, which is the gap: the room stays until
        // somebody removes it.
        assertTrue(topic.roomNames().peek().contains("empty-one"));

        test(findInView(Button.class).withText("Close empty rooms").single())
                .click();
        runPendingSignalsTasks();

        assertFalse(topic.roomNames().peek().contains("empty-one"));
        assertTrue(topic.roomNames().peek().contains(RoomsTopic.DEFAULT_ROOM),
                "a room with messages or people in it stays");
    }
}
