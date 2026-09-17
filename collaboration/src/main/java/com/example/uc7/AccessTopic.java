package com.example.uc7;

import com.example.collab.FormState;
import com.example.collab.Peer;
import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * UC7's shared form plus the write lock in front of it.
 * <p>
 * The lock is a {@link SharedValueSignal} holding whoever may write, with
 * {@link Peer#NOBODY} for "free". That makes claiming it a compare-and-set —
 * {@code replace(NOBODY, me)} — which is the only way two users pressing
 * <em>Claim</em> at the same moment can be resolved without one of them
 * silently winning twice.
 */
@Component
public class AccessTopic {

    public static final String NAME = "name";
    public static final String ADDRESS = "address";

    private final SharedValueSignal<Peer> holder = new SharedValueSignal<>(
            Peer.NOBODY);

    private final FormState form = new FormState();

    public SharedValueSignal<Peer> holder() {
        return holder;
    }

    /** Whether a given peer currently holds the lock. */
    public Signal<Boolean> isHolder(Peer peer) {
        return holder
                .map(current -> current != null && current.id() == peer.id());
    }

    /**
     * The form as this peer may use it: every write is checked against the lock
     * first.
     * <p>
     * The validator closes over the peer, which is what makes it an access
     * check at all — a {@code CommandValidator} is handed the command and
     * nothing else, so it cannot ask who is writing (API-GAPS.md #11). What it
     * can do is guard <em>this handle</em>, which makes the returned form a
     * capability: hand it to a peer and that peer can write while it holds the
     * lock, and not otherwise.
     */
    public FormState formFor(Peer peer) {
        return form.withValidator(command -> {
            Peer current = holder.peek();
            return current != null && current.id() == peer.id();
        });
    }

    /** The form as a spectator sees it: readable, not writable. */
    public FormState readonlyForm() {
        return form.asReadonly();
    }

    public FormState form() {
        return form;
    }
}
