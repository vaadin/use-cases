package com.example.uc12;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.collab.Peer;
import com.example.collab.PeerRig;
import com.example.collab.UseCaseView;
import com.example.common.UseCaseDescription;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC12 — Rooms created on demand. Beyond the sampler.
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
@PageTitle("UC12 — Rooms created on demand")
@UseCaseDescription("Creating a shared topic per room only when someone opens it")
@Menu(order = 12, title = "UC12 — Rooms created on demand")
public class RoomsView extends UseCaseView {

    public RoomsView(RoomsTopic topic) {
        super("UC12 — Rooms created on demand",
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

        Tabs tabs = new Tabs();
        // Kept across rebuilds, so a room keeps its tab while it exists.
        Map<String, Tab> tabOfRoom = new HashMap<>();
        Div roomPanel = new Div();

        // Switching rooms rebuilds the room panel rather than re-pointing the
        // bindings inside it: a binding belongs to a component, so a new room
        // gets new components, and the old room panel's detach leaves the old
        // room. That is also why this is Tabs over one panel and not a
        // TabSheet, which keeps a visited tab's content attached — and so
        // would keep the user present in every room they had looked at.
        SerializableConsumer<String> open = room -> {
            selected.set(room);
            roomPanel.removeAll();
            roomPanel.add(roomView(topic, peer, room));
        };

        // Rebuilding the tabs selects whichever tab comes first; that is not
        // the user choosing a room, so it is ignored.
        boolean[] rebuilding = { false };
        tabs.addSelectedChangeListener(event -> {
            String room = tabOfRoom.entrySet().stream()
                    .filter(entry -> entry.getValue()
                            .equals(event.getSelectedTab()))
                    .map(Map.Entry::getKey).findFirst().orElse(null);
            if (!rebuilding[0] && room != null
                    && !room.equals(selected.peek())) {
                open.accept(room);
            }
        });

        // Reads the selection as well as the room list: a room created here
        // may reach the list before it is selected, and its tab still has to
        // end up selected.
        Signal.effect(tabs, () -> {
            List<String> names = topic.roomNames().get();
            String current = selected.get();
            rebuilding[0] = true;
            tabs.removeAll();
            tabOfRoom.keySet().retainAll(names);
            names.forEach(name -> {
                Tab tab = tabOfRoom.computeIfAbsent(name,
                        room -> roomTab(topic, room));
                tabs.add(tab);
                if (name.equals(current)) {
                    tabs.setSelectedTab(tab);
                }
            });
            rebuilding[0] = false;
        });

        TextField newRoom = new TextField();
        newRoom.setPlaceholder("New room");
        Button create = new Button("Create", event -> {
            String name = newRoom.getValue().trim().toLowerCase();
            if (!name.isEmpty()) {
                topic.room(name);
                newRoom.clear();
                open.accept(name);
            }
        });
        create.addThemeVariants(ButtonVariant.SMALL);

        open.accept(RoomsTopic.DEFAULT_ROOM);

        return new Div(tabs, new HorizontalLayout(newRoom, create), roomPanel);
    }

    /** A room's tab, with its message count kept current. */
    private static Tab roomTab(RoomsTopic topic, String room) {
        var messages = topic.messages(room);
        Span label = new Span();
        label.bindText(Signal
                .computed(() -> room + " (" + messages.get().size() + ")"));
        return new Tab(label);
    }

    private static Component roomView(RoomsTopic topic, Peer peer,
            String room) {
        var presence = topic.presence(room);
        var messages = topic.messages(room);

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

        Div panel = new Div(group, list, input);
        panel.whenAttached(ui -> {
            presence.put(peer.key(), peer);
            return () -> presence.remove(peer.key());
        });
        return panel;
    }
}
