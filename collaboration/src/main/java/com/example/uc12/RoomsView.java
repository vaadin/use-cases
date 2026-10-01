package com.example.uc12;

import java.util.List;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.uc12.RoomsTopic.Post;
import com.example.views.MainLayout;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.AvatarGroup;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.messages.MessageInput;
import com.vaadin.flow.component.messages.MessageList;
import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC12 — Room per topic, created on demand. Beyond the sampler.
 * <p>
 * Every sample in the Collaboration Engine Sampler takes a topic id as a
 * string, and the engine keeps a topic per id somewhere out of sight. Signals
 * have no topic concept, which turns out to be an advantage: a room is a node
 * in a shared tree, created with {@code putChildIfAbsent} by whoever joins
 * first, and the list of rooms is itself shared state that clients can render.
 * A CE application cannot show its user a list of topics; this one is a
 * computed signal over the tree.
 * <p>
 * Each room node carries its messages as list children and its presence as map
 * children, so joining a room is a {@code put} on that room's map and leaving
 * is a {@code remove} — the same presence code as UC1, one level down the tree.
 * <p>
 * <em>Close empty rooms</em> is the part CE does not need: topics there expire
 * on a timeout, whereas a node tree keeps every child until somebody removes it
 * (API-GAPS.md #3).
 */
@Route(value = "rooms", layout = MainLayout.class)
@RouteAlias(value = "uc12", layout = MainLayout.class)
@PageTitle("UC12 — Room per topic, created on demand")
@Menu(order = 12, title = "UC12 Rooms on demand")
public class RoomsView extends UseCaseView {

    public RoomsView(RoomsTopic topic) {
        super("UC12 — Room per topic, created on demand",
                "Create a room in one panel and it appears in the others, because the room list is shared state "
                        + "and not a parameter. Each room has its own presence and its own messages; switching "
                        + "rooms leaves the first one.");

        Span removed = new Span();
        Button close = new Button("Close empty rooms", event -> {
            List<String> gone = topic.closeEmptyRooms();
            removed.setText(gone.isEmpty() ? "No empty rooms"
                    : "Closed: " + String.join(", ", gone));
        });
        Div controls = new Div(close, removed);

        add(new PeerRig(peer -> peerView(topic, peer)), controls);

        addNote("Rooms are map children of one SharedNodeSignal, so the room list is a computed signal rather "
                + "than something the server knows and the client does not. Nothing expires: an empty room stays "
                + "in the tree until it is removed (API-GAPS.md #3).");
    }

    private static Component peerView(RoomsTopic topic, Peer peer) {
        ValueSignal<String> selected = new ValueSignal<>(
                RoomsTopic.DEFAULT_ROOM);

        Div roomBar = new Div();
        roomBar.addClassName("room-bar");

        Div roomPanel = new Div();

        // Switching rooms rebuilds the room panel rather than re-pointing the
        // bindings inside it: a binding belongs to a component, so a new room
        // gets new components. Leaving the old room happens here, because the
        // presence entry is keyed by peer and not by component.
        Runnable openSelected = () -> {
            roomPanel.removeAll();
            roomPanel.add(roomView(topic, peer, selected.peek()));
        };

        Signal.effect(roomBar, () -> {
            roomBar.removeAll();
            topic.roomNames().get().forEach(name -> {
                Button open = new Button(
                        name + " (" + topic.messages(name).get().size() + ")",
                        event -> {
                            if (!name.equals(selected.peek())) {
                                topic.presence(selected.peek())
                                        .remove(peer.key());
                                selected.set(name);
                                openSelected.run();
                            }
                        });
                open.addThemeVariants(ButtonVariant.LUMO_SMALL);
                if (name.equals(selected.peek())) {
                    open.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
                }
                roomBar.add(open);
            });
        });

        TextField newRoom = new TextField();
        newRoom.setPlaceholder("New room");
        Button create = new Button("Create", event -> {
            String name = newRoom.getValue().trim().toLowerCase();
            if (!name.isEmpty()) {
                topic.room(name);
                newRoom.clear();
            }
        });
        create.addThemeVariants(ButtonVariant.LUMO_SMALL);

        openSelected.run();

        Div content = new Div(roomBar, new HorizontalLayout(newRoom, create),
                roomPanel);
        content.addDetachListener(
                event -> topic.presence(selected.peek()).remove(peer.key()));
        return content;
    }

    private static Component roomView(RoomsTopic topic, Peer peer,
            String room) {
        var presence = topic.presence(room);
        var messages = topic.messages(room);

        presence.put(peer.key(), peer);

        AvatarGroup group = new AvatarGroup();
        group.setMaxItemsVisible(4);
        group.bindItems(com.example.collab.Presence.avatarItems(presence));

        MessageList list = new MessageList();
        list.setHeight("10rem");
        list.bindItems(Signal.computed(() -> messages.get().stream()
                .<Signal<MessageListItem>> map(entry -> entry
                        .map(post -> post == null ? new MessageListItem()
                                : post.toItem()))
                .toList()));

        MessageInput input = new MessageInput();
        input.addSubmitListener(event -> messages.insertLast(
                new Post(peer.name(), peer.colorIndex(), event.getValue())));

        Span header = new Span("Room: " + room);

        Div panel = new Div(header, group, list, input);
        panel.addDetachListener(event -> presence.remove(peer.key()));
        return panel;
    }
}
