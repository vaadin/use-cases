package com.example.uc4;

import jakarta.annotation.PostConstruct;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.messages.MessageListItem;
import com.vaadin.flow.signals.shared.SharedListSignal;

/**
 * The chat topic: an append-only shared list, plus the durable store that
 * Collaboration Kit would have filled in through a
 * {@code CollaborationMessagePersister}.
 * <p>
 * The two halves are separate on purpose. {@link #messages()} is the live state
 * every client renders; {@link #history} stands in for a database, and nothing
 * in the signal API connects the two — no write-through, no load callback, no
 * "fetch messages since". So {@link #send} writes twice, and
 * {@link #simulateRestart()} shows what that buys: the shared list can be
 * thrown away and rebuilt from the store, which is what a real deployment does
 * on every restart. See API-GAPS.md #4.
 */
@Component
public class ChatTopic {

    /**
     * A chat message. Stored as a shared signal value, so it travels through
     * Jackson and carries its author rather than referring to one — a peer that
     * has long since left still has to render with the right name and colour.
     */
    public record Message(int authorId, String author, int colorIndex,
            String text, Instant time) {

        public MessageListItem toItem() {
            MessageListItem item = new MessageListItem(text, time, author);
            item.setUserColorIndex(colorIndex);
            item.setUserAbbreviation(abbreviation(author));
            return item;
        }

        private static String abbreviation(String name) {
            StringBuilder initials = new StringBuilder();
            for (String part : name.split(" ")) {
                if (!part.isEmpty()) {
                    initials.append(Character.toUpperCase(part.charAt(0)));
                }
            }
            return initials.toString();
        }
    }

    private final SharedListSignal<Message> messages = new SharedListSignal<>(
            Message.class);

    /** Stands in for the persistence a real chat would have. */
    private final List<Message> history = Collections
            .synchronizedList(new ArrayList<>());

    public SharedListSignal<Message> messages() {
        return messages;
    }

    @PostConstruct
    void seedHistory() {
        history.add(new Message(0, "Acme Support", 7,
                "Welcome — this room is the whole application's, so anything said here outlives the session that said it.",
                Instant.now()));
        loadFromHistory();
    }

    /**
     * Appends a message. Both writes are the application's job: the signal so
     * that every open client sees it now, the store so that it is still there
     * after a restart.
     */
    public void send(Message message) {
        history.add(message);
        messages.insertLast(message);
    }

    /**
     * Drops the live state and rebuilds it from the store, the way a fresh
     * server process would.
     */
    public void simulateRestart() {
        messages.clear();
        loadFromHistory();
    }

    private void loadFromHistory() {
        List<Message> snapshot;
        synchronized (history) {
            snapshot = List.copyOf(history);
        }
        messages.insertAllLast(snapshot);
    }

    public int historySize() {
        return history.size();
    }
}
