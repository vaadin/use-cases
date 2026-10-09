package com.example.uc12;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedMapSignal;
import com.vaadin.flow.signals.shared.SharedNodeSignal;

/**
 * UC12's room tree: one {@link SharedNodeSignal} whose map children are the
 * rooms.
 * <p>
 * This is what Collaboration Kit calls a topic id, built out of the signal API
 * instead of being a parameter to it. A room is a node created on first join
 * with {@code putChildIfAbsent}, and because a node holds both list children
 * and map children, one room node carries both its messages (the list) and its
 * presence (the map) without needing two trees.
 * <p>
 * What the tree does not do is tidy itself. CE's topics take an expiration
 * timeout and drop their data when nobody has been in them; a node tree grows
 * until something removes a child, which is why {@link #closeEmptyRooms()}
 * exists and is a button in the UI rather than a policy. See API-GAPS.md #3.
 */
@Component
public class RoomsTopic {

    public record Post(String author, int colorIndex, String text) {

        public MessageListItem toItem() {
            MessageListItem item = new MessageListItem(text, null, author);
            item.setUserColorIndex(colorIndex);
            return item;
        }
    }

    public static final String DEFAULT_ROOM = "general";

    private final SharedNodeSignal root = new SharedNodeSignal();

    public RoomsTopic() {
        room(DEFAULT_ROOM);
        room("returns");
    }

    /** The rooms that exist right now, in alphabetical order. */
    public Signal<List<String>> roomNames() {
        return Signal.computed(() -> root.get().mapChildren().keySet().stream()
                .sorted(Comparator.naturalOrder()).toList());
    }

    /**
     * The room node, created if this is the first time anybody asks for it.
     * <p>
     * The node is read back out of the parent rather than taken from the
     * operation's result, which would need a cast through
     * {@code PutIfAbsentResult}: on a tree that applies commands synchronously
     * the child is already there. A tree that had to agree with other nodes
     * first would need the operation's future instead — the same distinction
     * UC13 is about.
     */
    public SharedNodeSignal room(String name) {
        SharedNodeSignal existing = root.peek().mapChildren().get(name);
        if (existing != null) {
            return existing;
        }
        root.putChildIfAbsent(name);
        SharedNodeSignal created = root.peek().mapChildren().get(name);
        if (created == null) {
            throw new IllegalStateException(
                    "Room " + name + " was not created");
        }
        return created;
    }

    /** A room's messages: the node's list children. */
    public SharedListSignal<Post> messages(String room) {
        return room(room).asList(Post.class);
    }

    /** A room's presence: the node's map children, keyed by peer. */
    public SharedMapSignal<com.example.collab.Peer> presence(String room) {
        return room(room).asMap(com.example.collab.Peer.class);
    }

    /**
     * Removes every room with no messages and nobody in it, except the default
     * one. The manual equivalent of an expiration timeout.
     *
     * @return the rooms that were removed
     */
    public List<String> closeEmptyRooms() {
        List<String> removed = root.peek().mapChildren().entrySet().stream()
                .filter(entry -> !DEFAULT_ROOM.equals(entry.getKey()))
                .filter(entry -> {
                    var node = entry.getValue().peek();
                    return node.listChildren().isEmpty()
                            && node.mapChildren().isEmpty();
                }).map(java.util.Map.Entry::getKey).toList();
        removed.forEach(root::removeChild);
        return removed;
    }
}
